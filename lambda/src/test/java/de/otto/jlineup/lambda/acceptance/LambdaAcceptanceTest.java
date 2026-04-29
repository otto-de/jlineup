package de.otto.jlineup.lambda.acceptance;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.lambda.LambdaRequestPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real AWS acceptance test for the JLineup Docker Lambda.
 *
 * <p>The CDK stack is <strong>kept alive</strong> between test runs for speed.
 * On the first run (or when the stack is missing/broken) the stack is deployed
 * automatically. Subsequent runs reuse the existing Lambda without redeploying.
 * Call {@link #destroyAcceptanceStack()} explicitly (via env var) when you
 * want to tear everything down.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@code @BeforeAll} checks CloudFormation: if the stack exists and is
 *       healthy the deploy is skipped entirely → fast re-run.</li>
 *   <li>If the stack is missing or unhealthy, CDK synthesizes and deploys it.</li>
 *   <li>The stack is <em>never</em> destroyed automatically – call
 *       {@link #destroyAcceptanceStack()} when done.</li>
 * </ol>
 *
 * <h2>Prerequisites</h2>
 * <ul>
 *   <li>{@code JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED=true} – gates the test</li>
 *   <li>{@code AWS_ACCOUNT_ID} – 12-digit AWS account ID</li>
 *   <li>{@code JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true} – (optional) forces a
 *       CDK redeploy even if a healthy stack already exists; use this after changing
 *       the Lambda handler code</li>
 *   <li>{@code AWS_DEFAULT_REGION} or {@code AWS_REGION} – target region
 *       (default: {@code eu-central-1})</li>
 *   <li>Active AWS credentials (env vars, profile, or instance role)</li>
 *   <li>{@code cdk} CLI installed and bootstrapped (needed only for first deploy /
 *       destroy – {@code cdk bootstrap aws://$AWS_ACCOUNT_ID/$AWS_REGION})</li>
 *   <li>Docker daemon running (needed only when the stack does not exist yet)</li>
 *   <li>Lambda module pre-built ({@code ./gradlew :jlineup-lambda:build})</li>
 * </ul>
 *
 * <h2>Running</h2>
 * <pre>{@code
 * # First run or after stack was destroyed – builds Docker image:
 * export JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED=true
 * export AWS_ACCOUNT_ID=123456789012
 * export AWS_DEFAULT_REGION=eu-central-1
 * ./gradlew :jlineup-lambda:build :jlineup-lambda:test --tests '*LambdaAcceptanceTest.shouldInvoke*'
 *
 * # Subsequent runs – stack is reused, no Docker build:
 * ./gradlew :jlineup-lambda:test --tests '*LambdaAcceptanceTest.shouldInvoke*'
 *
 * # Force redeploy after handler changes:
 * JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true \
 *   ./gradlew :jlineup-lambda:build :jlineup-lambda:test --tests '*LambdaAcceptanceTest.shouldInvoke*'
 *
 * # Explicit teardown:
 * JLINEUP_LAMBDA_ACCEPTANCE_DESTROY=true \
 *   ./gradlew :jlineup-lambda:test --tests '*LambdaAcceptanceTest.destroyAcceptanceStack*'
 * }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED", matches = "true")
class LambdaAcceptanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaAcceptanceTest.class);

    /**
     * Fixed stack name – intentionally stable across runs so the stack can be
     * reused without redeploying on every test execution.
     */
    static final String STACK_NAME = "jlineup-lambda-acc-test";

    private String functionName;
    private String webkitFunctionName;
    private String bucketName;
    private String awsRegion;

    private final JsonMapper jsonMapper = JacksonWrapper.jsonMapperForLambdaHandler();

    // -------------------------------------------------------------------------
    // Lifecycle – deploy only when the stack is absent / broken
    // -------------------------------------------------------------------------

    @BeforeAll
    void ensureStackDeployed() throws Exception {
        awsRegion = resolveRegion();

        boolean stackExists = tryLoadExistingStack();
        boolean forceDeploy = "true".equalsIgnoreCase(System.getenv("JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY"));

        if (stackExists && !forceDeploy) {
            LOG.info("✔ Reusing existing stack '{}' (default: '{}', webkit: '{}'). Skipping deploy.",
                    STACK_NAME, functionName, webkitFunctionName);
            return;
        }

        if (stackExists) {
            LOG.info("JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true – redeploying stack '{}' despite existing stack.",
                    STACK_NAME);
        }

        String awsAccountId = System.getenv("AWS_ACCOUNT_ID");
        if (awsAccountId == null || awsAccountId.isBlank()) {
            throw new IllegalStateException(
                    "Stack '" + STACK_NAME + "' does not exist yet. " +
                    "Set AWS_ACCOUNT_ID so the stack can be created.");
        }

        deployStack(awsAccountId);
    }

    /**
     * Checks CloudFormation for an existing, healthy stack and reads its outputs.
     *
     * @return {@code true} if the stack exists and is ready to use
     */
    private boolean tryLoadExistingStack() {
        try (CloudFormationClient cf = CloudFormationClient.builder()
                .region(Region.of(awsRegion))
                .build()) {

            List<Stack> stacks = cf.describeStacks(r -> r.stackName(STACK_NAME)).stacks();
            if (stacks.isEmpty()) {
                return false;
            }
            Stack stack = stacks.get(0);
            StackStatus status = stack.stackStatus();
            if (status != StackStatus.CREATE_COMPLETE && status != StackStatus.UPDATE_COMPLETE) {
                LOG.warn("Stack '{}' found but status is {} – will redeploy.", STACK_NAME, status);
                return false;
            }
            functionName = stack.outputs().stream()
                    .filter(o -> JLineupLambdaCdkStack.OUTPUT_FUNCTION_NAME.equals(o.outputKey()))
                    .findFirst()
                    .map(Output::outputValue)
                    .orElseThrow(() -> new IllegalStateException(
                            "Stack exists but output '" + JLineupLambdaCdkStack.OUTPUT_FUNCTION_NAME
                            + "' is missing"));
            webkitFunctionName = stack.outputs().stream()
                    .filter(o -> JLineupLambdaCdkStack.OUTPUT_WEBKIT_FUNCTION_NAME.equals(o.outputKey()))
                    .findFirst()
                    .map(Output::outputValue)
                    .orElse(null);
            bucketName = stack.outputs().stream()
                    .filter(o -> JLineupLambdaCdkStack.OUTPUT_BUCKET_NAME.equals(o.outputKey()))
                    .findFirst()
                    .map(Output::outputValue)
                    .orElseThrow(() -> new IllegalStateException(
                            "Stack exists but output '" + JLineupLambdaCdkStack.OUTPUT_BUCKET_NAME
                            + "' is missing"));
            if (webkitFunctionName == null) {
                LOG.warn("Stack '{}' has no '{}' output – WebKit test will be skipped. " +
                        "Set JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true to redeploy with WebKit support.",
                        STACK_NAME, JLineupLambdaCdkStack.OUTPUT_WEBKIT_FUNCTION_NAME);
            }
            return true;

        } catch (CloudFormationException e) {
            // "Stack with id … does not exist"
            LOG.info("Stack '{}' not found in CloudFormation – will deploy.", STACK_NAME);
            return false;
        }
    }

    /** Full CDK synth + deploy. Called only when the stack is absent or broken. */
    private void deployStack(String awsAccountId) throws Exception {
        Path lambdaModuleDir  = Paths.get("").toAbsolutePath();
        Path cdkOutDir        = Files.createTempDirectory("jlineup-cdk-out-");
        Path cdkOutputsFile   = Files.createTempFile("jlineup-cdk-outputs-", ".json");

        LOG.info("Synthesizing CDK stack '{}' (account={}, region={})…",
                STACK_NAME, awsAccountId, awsRegion);
        App app = new App(AppProps.builder().outdir(cdkOutDir.toString()).build());
        new JLineupLambdaCdkStack(app, STACK_NAME, lambdaModuleDir.toString(), awsAccountId, awsRegion);
        app.synth();

        LOG.info("Deploying CDK stack '{}' – Docker build + ECR push may take several minutes…",
                STACK_NAME);
        runCommand("cdk", "deploy", STACK_NAME,
                "--require-approval", "never",
                "--outputs-file", cdkOutputsFile.toString(),
                "--app", cdkOutDir.toString());

        String outputsJson = Files.readString(cdkOutputsFile);
        LOG.info("CDK stack outputs: {}", outputsJson);
        functionName = parseJsonValue(outputsJson, JLineupLambdaCdkStack.OUTPUT_FUNCTION_NAME);
        webkitFunctionName = parseJsonValue(outputsJson, JLineupLambdaCdkStack.OUTPUT_WEBKIT_FUNCTION_NAME);
        bucketName = parseJsonValue(outputsJson, JLineupLambdaCdkStack.OUTPUT_BUCKET_NAME);
        LOG.info("Deployed Lambda functions: default='{}', webkit='{}', bucket='{}'", functionName, webkitFunctionName, bucketName);
    }

    // -------------------------------------------------------------------------
    // Explicit teardown – opt-in only
    // -------------------------------------------------------------------------

    /**
     * Destroys the shared acceptance stack.
     * <p>Run with:
     * <pre>{@code
     * JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED=true \
     * JLINEUP_LAMBDA_ACCEPTANCE_DESTROY=true \
     *   ./gradlew :jlineup-lambda:test --tests '*LambdaAcceptanceTest.destroyAcceptanceStack*'
     * }</pre>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "JLINEUP_LAMBDA_ACCEPTANCE_DESTROY", matches = "true")
    void destroyAcceptanceStack() throws Exception {
        String awsAccountId = System.getenv("AWS_ACCOUNT_ID");
        if (awsAccountId == null || awsAccountId.isBlank()) {
            throw new IllegalStateException("AWS_ACCOUNT_ID must be set to destroy the stack.");
        }

        Path lambdaModuleDir = Paths.get("").toAbsolutePath();
        Path cdkOutDir       = Files.createTempDirectory("jlineup-cdk-out-destroy-");

        LOG.info("Synthesizing CDK stack '{}' for destruction…", STACK_NAME);
        App app = new App(AppProps.builder().outdir(cdkOutDir.toString()).build());
        new JLineupLambdaCdkStack(app, STACK_NAME, lambdaModuleDir.toString(), awsAccountId, awsRegion);
        app.synth();

        LOG.info("Destroying CDK stack '{}'…", STACK_NAME);
        runCommand("cdk", "destroy", STACK_NAME,
                "--force",
                "--app", cdkOutDir.toString());
        LOG.info("Stack '{}' destroyed.", STACK_NAME);
    }

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    /**
     * Mirrors the pattern of {@link de.otto.jlineup.cli.acceptance.JLineupCLIAcceptanceTest}:
     * invokes the Lambda for a <em>before</em> step and an <em>after</em> step
     * against {@code https://www.example.com} and asserts that both return
     * {@code "OK"} (i.e. Chrome started, screenshot taken, S3 upload succeeded).
     */
    @Test
    void shouldInvokeLambdaForBeforeAndAfterStepAndReturnOk() throws Exception {
        JobConfig jobConfig = JobConfig.jobConfigBuilder()
                .withUrls(Map.of(
                        "https://www.example.com",
                        UrlConfig.urlConfigBuilder()
                                .withStringPaths(List.of("/"))
                                .build()))
                .withBrowser(Browser.Type.CHROME_HEADLESS)
                .withGlobalTimeout(120)
                .build().insertDefaults();

        RunStepConfig beforeCfg = RunStepConfig.runStepConfigBuilder().withStep(RunStep.before).build();
        RunStepConfig afterCfg  = RunStepConfig.runStepConfigBuilder().withStep(RunStep.after_only).build();

        List<ScreenshotContext> beforeContexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(beforeCfg, jobConfig);
        List<ScreenshotContext> afterContexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(afterCfg, jobConfig);

        String runId = UUID.randomUUID().toString();

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(330))
                        .connectionTimeout(Duration.ofSeconds(30)))
                .build()) {

            LOG.info("=== BEFORE STEP – {} context(s) ===", beforeContexts.size());
            for (ScreenshotContext ctx : beforeContexts) {
                String response = invokeLambda(lambdaClient, functionName, jobConfig, ctx, RunStep.before, runId);
                LOG.info("Before response [{}]: {}", ctx.url, response);
                assertFalse(response.contains("errorMessage"),
                        "Lambda before step failed for '" + ctx.url + "': " + response);
                assertTrue(response.startsWith("OK"),
                        "Expected 'OK' prefix in Lambda before response for '"
                                + ctx.url + "', got: " + response);
            }

            LOG.info("=== AFTER STEP – {} context(s) ===", afterContexts.size());
            for (ScreenshotContext ctx : afterContexts) {
                String response = invokeLambda(lambdaClient, functionName, jobConfig, ctx, RunStep.after_only, runId);
                LOG.info("After response [{}]: {}", ctx.url, response);
                assertFalse(response.contains("errorMessage"),
                        "Lambda after step failed for '" + ctx.url + "': " + response);
                assertTrue(response.startsWith("OK"),
                        "Expected 'OK' prefix in Lambda after response for '"
                                + ctx.url + "', got: " + response);
            }
        }
    }

    /**
     * Same as the default test but invokes the WebKit Lambda with
     * {@link Browser.Type#WEBKIT_HEADLESS}.
     */
    @Test
    void shouldInvokeWebKitLambdaForBeforeAndAfterStepAndReturnOk() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(webkitFunctionName != null,
                "WebKit Lambda not deployed – skipping. Redeploy with JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true.");

        JobConfig jobConfig = JobConfig.jobConfigBuilder()
                .withUrls(Map.of(
                        "https://www.example.com",
                        UrlConfig.urlConfigBuilder()
                                .withStringPaths(List.of("/"))
                                .build()))
                .withBrowser(Browser.Type.WEBKIT_HEADLESS)
                .withGlobalTimeout(120)
                .build().insertDefaults();

        RunStepConfig beforeCfg = RunStepConfig.runStepConfigBuilder().withStep(RunStep.before).build();
        RunStepConfig afterCfg  = RunStepConfig.runStepConfigBuilder().withStep(RunStep.after_only).build();

        List<ScreenshotContext> beforeContexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(beforeCfg, jobConfig);
        List<ScreenshotContext> afterContexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(afterCfg, jobConfig);

        String runId = UUID.randomUUID().toString();

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(330))
                        .connectionTimeout(Duration.ofSeconds(30)))
                .build()) {

            LOG.info("=== WEBKIT BEFORE STEP – {} context(s) ===", beforeContexts.size());
            for (ScreenshotContext ctx : beforeContexts) {
                String response = invokeLambda(lambdaClient, webkitFunctionName, jobConfig, ctx, RunStep.before, runId);
                LOG.info("WebKit before response [{}]: {}", ctx.url, response);
                assertFalse(response.contains("errorMessage"),
                        "WebKit Lambda before step failed for '" + ctx.url + "': " + response);
                assertTrue(response.startsWith("OK"),
                        "Expected 'OK' prefix in WebKit Lambda before response for '"
                                + ctx.url + "', got: " + response);
            }

            LOG.info("=== WEBKIT AFTER STEP – {} context(s) ===", afterContexts.size());
            for (ScreenshotContext ctx : afterContexts) {
                String response = invokeLambda(lambdaClient, webkitFunctionName, jobConfig, ctx, RunStep.after_only, runId);
                LOG.info("WebKit after response [{}]: {}", ctx.url, response);
                assertFalse(response.contains("errorMessage"),
                        "WebKit Lambda after step failed for '" + ctx.url + "': " + response);
                assertTrue(response.startsWith("OK"),
                        "Expected 'OK' prefix in WebKit Lambda after response for '"
                                + ctx.url + "', got: " + response);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Screenshot download & size verification tests
    // -------------------------------------------------------------------------

    /**
     * Invokes the Chrome Lambda for a before step, downloads the screenshots
     * from S3, and verifies the image dimensions match the configured viewport.
     */
    @Test
    void shouldProduceChromeScreenshotsWithCorrectDimensions() throws Exception {
        int viewportWidth = 800;
        int viewportHeight = 600;

        JobConfig jobConfig = JobConfig.jobConfigBuilder()
                .withUrls(Map.of(
                        "https://www.example.com",
                        UrlConfig.urlConfigBuilder()
                                .withStringPaths(List.of("/"))
                                .withDevices(List.of(
                                        DeviceConfig.deviceConfigBuilder()
                                                .withWidth(viewportWidth)
                                                .withHeight(viewportHeight)
                                                .build()))
                                .build()))
                .withBrowser(Browser.Type.CHROME_HEADLESS)
                .withGlobalTimeout(120)
                .build().insertDefaults();

        RunStepConfig beforeCfg = RunStepConfig.runStepConfigBuilder().withStep(RunStep.before).build();
        List<ScreenshotContext> contexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(beforeCfg, jobConfig);

        String runId = UUID.randomUUID().toString();

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(330))
                        .connectionTimeout(Duration.ofSeconds(30)))
                .build()) {

            for (ScreenshotContext ctx : contexts) {
                String response = invokeLambda(lambdaClient, functionName, jobConfig, ctx, RunStep.before, runId);
                assertTrue(response.startsWith("OK"), "Lambda failed: " + response);
            }
        }

        List<BufferedImage> screenshots = downloadScreenshotsFromS3(runId);
        assertFalse(screenshots.isEmpty(), "No screenshots found in S3 for run " + runId);

        for (BufferedImage img : screenshots) {
            LOG.info("Chrome screenshot dimensions: {}x{}", img.getWidth(), img.getHeight());
            assertEquals(viewportWidth, img.getWidth(),
                    "Screenshot width should match viewport width");
            // Height can vary depending on page content, but should be at least viewport height
            assertTrue(img.getHeight() >= viewportHeight,
                    "Screenshot height (" + img.getHeight() + ") should be >= viewport height (" + viewportHeight + ")");
        }
    }

    /**
     * Invokes the WebKit Lambda with different pixel ratios, downloads the screenshots
     * from S3, and verifies that the image dimensions scale accordingly.
     */
    @ParameterizedTest(name = "WebKit pixelRatio={0}")
    @ValueSource(floats = {1.0f, 2.0f})
    void shouldProduceWebKitScreenshotsWithCorrectDimensions(float pixelRatio) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(webkitFunctionName != null,
                "WebKit Lambda not deployed – skipping.");

        int viewportWidth = 800;
        int viewportHeight = 600;

        JobConfig jobConfig = JobConfig.jobConfigBuilder()
                .withUrls(Map.of(
                        "https://www.example.com",
                        UrlConfig.urlConfigBuilder()
                                .withStringPaths(List.of("/"))
                                .withDevices(List.of(
                                        DeviceConfig.deviceConfigBuilder()
                                                .withWidth(viewportWidth)
                                                .withHeight(viewportHeight)
                                                .withPixelRatio(pixelRatio)
                                                .build()))
                                .build()))
                .withBrowser(Browser.Type.WEBKIT_HEADLESS)
                .withGlobalTimeout(120)
                .build().insertDefaults();

        RunStepConfig beforeCfg = RunStepConfig.runStepConfigBuilder().withStep(RunStep.before).build();
        List<ScreenshotContext> contexts =
                BrowserUtils.buildScreenshotContextListFromConfigAndState(beforeCfg, jobConfig);

        String runId = UUID.randomUUID().toString();

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .socketTimeout(Duration.ofSeconds(330))
                        .connectionTimeout(Duration.ofSeconds(30)))
                .build()) {

            for (ScreenshotContext ctx : contexts) {
                String response = invokeLambda(lambdaClient, webkitFunctionName, jobConfig, ctx, RunStep.before, runId);
                assertTrue(response.startsWith("OK"), "WebKit Lambda failed: " + response);
            }
        }

        List<BufferedImage> screenshots = downloadScreenshotsFromS3(runId);
        assertFalse(screenshots.isEmpty(), "No screenshots found in S3 for run " + runId);

        int expectedWidth = Math.round(viewportWidth * pixelRatio);
        int expectedMinHeight = Math.round(viewportHeight * pixelRatio);

        for (BufferedImage img : screenshots) {
            LOG.info("WebKit (pixelRatio={}) screenshot dimensions: {}x{}", pixelRatio, img.getWidth(), img.getHeight());
            assertEquals(expectedWidth, img.getWidth(),
                    "Screenshot width should be " + expectedWidth + " (viewport " + viewportWidth + " * pixelRatio " + pixelRatio + ")");
            assertTrue(img.getHeight() >= expectedMinHeight,
                    "Screenshot height (" + img.getHeight() + ") should be >= " + expectedMinHeight
                    + " (viewport " + viewportHeight + " * pixelRatio " + pixelRatio + ")");
        }
    }

    // -------------------------------------------------------------------------
    // S3 screenshot download helper
    // -------------------------------------------------------------------------

    /**
     * Lists and downloads all PNG screenshots from S3 for the given run ID.
     */
    private List<BufferedImage> downloadScreenshotsFromS3(String runId) throws IOException {
        String s3Prefix = "lamba-screenshots-prefix/jlineup-" + runId;
        List<BufferedImage> images = new ArrayList<>();

        try (S3Client s3 = S3Client.builder().region(Region.of(awsRegion)).build()) {
            ListObjectsV2Response listing = s3.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Prefix)
                    .build());

            List<S3Object> pngObjects = listing.contents().stream()
                    .filter(o -> o.key().endsWith(".png"))
                    .toList();

            LOG.info("Found {} PNG files in S3 under prefix '{}'", pngObjects.size(), s3Prefix);

            for (S3Object obj : pngObjects) {
                LOG.info("Downloading s3://{}/{}", bucketName, obj.key());
                byte[] data = s3.getObject(
                        r -> r.bucket(bucketName).key(obj.key()),
                        ResponseTransformer.toBytes()
                ).asByteArray();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img != null) {
                    images.add(img);
                } else {
                    LOG.warn("Could not decode PNG: {}", obj.key());
                }
            }
        }
        return images;
    }

    // -------------------------------------------------------------------------

    private String invokeLambda(LambdaClient lambdaClient,
                                String targetFunctionName,
                                JobConfig jobConfig,
                                ScreenshotContext ctx,
                                RunStep step,
                                String runId) throws Exception {
        LambdaRequestPayload payload =
                new LambdaRequestPayload(runId, jobConfig, ctx, step, ctx.urlKey);
        String payloadJson = jsonMapper.writeValueAsString(payload);

        InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                .functionName(targetFunctionName)
                .payload(SdkBytes.fromUtf8String(payloadJson))
                .build());

        return response.payload().asUtf8String();
    }

    private static void runCommand(String... command) throws Exception {
        LOG.info("$ {}", String.join(" ", command));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOG.info("[cdk] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(
                    "Command exited with code " + exitCode + ": " + String.join(" ", command));
        }
    }

    private static String parseJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx == -1) {
            throw new IllegalStateException(
                    "Key '" + key + "' not found in CDK outputs: " + json);
        }
        int colon = json.indexOf(':', keyIdx);
        int open  = json.indexOf('"', colon + 1);
        int close = json.indexOf('"', open + 1);
        return json.substring(open + 1, close);
    }

    private static String resolveRegion() {
        String r = System.getenv("AWS_DEFAULT_REGION");
        if (r == null || r.isBlank()) r = System.getenv("AWS_REGION");
        return (r != null && !r.isBlank()) ? r : "eu-central-1";
    }
}
