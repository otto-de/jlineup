package de.otto.jlineup.clilambda.acceptance;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.cli.Main;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.report.Report;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full end-to-end acceptance test for the {@code jlineup-cli-lambda} module.
 *
 * <p>Reuses the CDK stack created by the {@code jlineup-lambda} module's acceptance
 * test ({@code jlineup-lambda-acc-test}).  If the stack already exists and is healthy
 * it is reused without redeploying (fast path).  Otherwise the stack is deployed via
 * {@code cdk deploy}.
 *
 * <p>The test performs a full <em>before → after</em> run via the {@code jlineup-cli-lambda}
 * CLI entry point ({@link Main#main}) using the {@code -F} flag to delegate screenshot
 * capture to the remote Lambda function.  The config used exercises alternating cookies
 * (two cookie sets: {@code theme=dark} / {@code theme=light}) combined with two device
 * configurations, producing 4 screenshot contexts in total.  After the run the generated
 * {@code report.json} is parsed and asserted to have zero pixel differences.
 *
 * <h2>Prerequisites</h2>
 * <ul>
 *   <li>{@code JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED=true} – gates the test</li>
 *   <li>{@code AWS_ACCOUNT_ID} – 12-digit AWS account ID (required only for first deploy)</li>
 *   <li>{@code JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true} – (optional) forces CDK redeploy</li>
 *   <li>{@code AWS_DEFAULT_REGION} or {@code AWS_REGION} – target region (default: {@code eu-central-1})</li>
 *   <li>Active AWS credentials (env vars, profile, or instance role)</li>
 *   <li>{@code cdk} CLI installed and bootstrapped (first deploy only)</li>
 *   <li>Docker daemon running (first deploy only)</li>
 *   <li>Lambda module pre-built: {@code ./gradlew :jlineup-lambda:build}</li>
 * </ul>
 *
 * <h2>Running</h2>
 * <pre>{@code
 * # First run (builds Docker image, deploys stack):
 * export JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED=true
 * export AWS_ACCOUNT_ID=123456789012
 * export AWS_DEFAULT_REGION=eu-central-1
 * ./gradlew :jlineup-lambda:build :jlineup-cli-lambda:test --tests '*CliLambdaAcceptanceTest*'
 *
 * # Subsequent runs (stack is reused, no Docker build needed):
 * ./gradlew :jlineup-cli-lambda:test --tests '*CliLambdaAcceptanceTest*'
 * }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED", matches = "true")
class CliLambdaAcceptanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(CliLambdaAcceptanceTest.class);

    /**
     * The same stack name used by the lambda module's acceptance test so both tests
     * share the same long-lived CDK stack.
     */
    static final String STACK_NAME = "jlineup-lambda-acc-test";

    /**
     * CloudFormation output key exported by {@code JLineupLambdaCdkStack}.
     */
    static final String OUTPUT_FUNCTION_NAME = "FunctionName";

    private String functionName;
    private String awsRegion;

    private Path tempDirectory;

    private ByteArrayOutputStream systemOutCaptor;
    private ByteArrayOutputStream systemErrCaptor;
    private final PrintStream stdout = System.out;
    private final PrintStream stderr = System.err;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    void ensureStackDeployed() throws Exception {
        awsRegion = resolveRegion();

        boolean stackExists = tryLoadExistingStack();
        boolean forceDeploy = "true".equalsIgnoreCase(System.getenv("JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY"));

        if (stackExists && !forceDeploy) {
            LOG.info("Reusing existing stack '{}' (Lambda: '{}'). Skipping deploy.", STACK_NAME, functionName);
            return;
        }

        if (stackExists) {
            LOG.info("JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true – redeploying stack '{}' despite existing stack.",
                    STACK_NAME);
        }

        String awsAccountId = System.getenv("AWS_ACCOUNT_ID");
        if (awsAccountId == null || awsAccountId.isBlank()) {
            throw new IllegalStateException(
                    "Stack '" + STACK_NAME + "' does not exist yet. Set AWS_ACCOUNT_ID so the stack can be created.");
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
                    .filter(o -> OUTPUT_FUNCTION_NAME.equals(o.outputKey()))
                    .findFirst()
                    .map(Output::outputValue)
                    .orElseThrow(() -> new IllegalStateException(
                            "Stack exists but output '" + OUTPUT_FUNCTION_NAME + "' is missing"));
            return true;

        } catch (CloudFormationException e) {
            LOG.info("Stack '{}' not found in CloudFormation – will deploy.", STACK_NAME);
            return false;
        }
    }

    /**
     * Full CDK synth + deploy using the same {@code lambda/} module directory as Docker
     * build context so that the same Dockerfile / handler image is used.
     */
    private void deployStack(String awsAccountId) throws Exception {
        // Docker context must be the lambda/ module directory (contains Dockerfile + build artefacts)
        Path lambdaModuleDir = Paths.get("").toAbsolutePath().getParent().resolve("lambda");
        Path cdkOutDir = Files.createTempDirectory("jlineup-cdk-out-cli-lambda-acc-");
        Path cdkOutputsFile = Files.createTempFile("jlineup-cdk-outputs-cli-lambda-", ".json");

        LOG.info("Synthesizing CDK stack '{}' (account={}, region={})…", STACK_NAME, awsAccountId, awsRegion);
        App app = new App(AppProps.builder().outdir(cdkOutDir.toString()).build());
        new JLineupLambdaCdkStack(app, STACK_NAME, lambdaModuleDir.toString(), awsAccountId, awsRegion);
        app.synth();

        LOG.info("Deploying CDK stack '{}' – Docker build + ECR push may take several minutes…", STACK_NAME);
        runCommand("cdk", "deploy", STACK_NAME,
                "--require-approval", "never",
                "--outputs-file", cdkOutputsFile.toString(),
                "--app", cdkOutDir.toString());

        String outputsJson = Files.readString(cdkOutputsFile);
        LOG.info("CDK stack outputs: {}", outputsJson);
        functionName = parseJsonValue(outputsJson, OUTPUT_FUNCTION_NAME);
        LOG.info("Deployed Lambda function: '{}'", functionName);
    }

    @BeforeEach
    void setUpStreams() {
        systemOutCaptor = new ByteArrayOutputStream();
        systemErrCaptor = new ByteArrayOutputStream();

        MirrorOutputStream teeOut = new MirrorOutputStream(stdout, systemOutCaptor);
        MirrorOutputStream teeErr = new MirrorOutputStream(stderr, systemErrCaptor);

        System.setOut(new PrintStream(teeOut));
        System.setErr(new PrintStream(teeErr));
    }

    @BeforeEach
    void createTempDir() throws IOException {
        tempDirectory = Files.createTempDirectory("jlineup-cli-lambda-acceptance-test-");
        LOG.info("Using temp directory: {}", tempDirectory);
    }

    @AfterEach
    void cleanUpStreams() throws IOException {
        System.setOut(stdout);
        System.setErr(stderr);
        systemOutCaptor.close();
        systemErrCaptor.close();
    }

    @AfterEach
    void deleteTempDir() throws Exception {
        deleteDir(tempDirectory);
    }

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    /**
     * Full end-to-end test via the CLI entry point with the Lambda browser.
     *
     * <p>Config uses:
     * <ul>
     *   <li>URL: {@code https://www.example.com/}</li>
     *   <li>A base cookie: {@code trackingDisabled=true}</li>
     *   <li>Two alternating cookie sets: {@code theme=dark} and {@code theme=light} –
     *       each set creates a separate screenshot context (4 contexts total with 2 devices)</li>
     *   <li>Two device configs: desktop 1280×800 and {@code iPhone SE} mobile emulation</li>
     * </ul>
     *
     * <p>Assertions:
     * <ul>
     *   <li>The {@code before} step completes without throwing (no {@code System.exit} on success)</li>
     *   <li>The {@code after} step completes without throwing</li>
     *   <li>{@code report.json} exists and the overall pixel difference sum is exactly 0.0</li>
     *   <li>{@code report.html} exists and contains screenshot links</li>
     *   <li>The file tracker records exactly 4 screenshot contexts (2 cookies × 2 devices)</li>
     * </ul>
     */
    @Test
    void shouldRunFullBeforeAndAfterCycleViaCliLambdaWithAlternatingCookies() throws Exception {
        String configPath = resolveConfigPath("acceptance/acceptance_cli_lambda.lineup.json");

        // --- BEFORE step ---
        LOG.info("=== BEFORE step (Lambda function: {}) ===", functionName);
        Main.main(new String[]{
                "--working-dir", tempDirectory.toString(),
                "--config", configPath,
                "--step", "before",
                "-F", functionName
        });

        Path reportBeforeHtml = tempDirectory.resolve("report/report_before.html");
        assertTrue(Files.exists(reportBeforeHtml),
                "report_before.html should exist after before step, but was not found at: " + reportBeforeHtml);
        LOG.info("report_before.html found at {}", reportBeforeHtml);

        // --- AFTER step ---
        LOG.info("=== AFTER step (Lambda function: {}) ===", functionName);
        Main.main(new String[]{
                "--working-dir", tempDirectory.toString(),
                "--config", configPath,
                "--step", "after",
                "-F", functionName
        });

        // --- Assert report files ---
        Path reportJson = tempDirectory.resolve("report/report.json");
        assertTrue(Files.exists(reportJson), "report.json should exist after after step");

        Path reportHtml = tempDirectory.resolve("report/report.html");
        assertTrue(Files.exists(reportHtml), "report.html should exist after after step");

        // --- Assert zero pixel difference ---
        Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat("Expected zero pixel difference across all screenshot contexts",
                report.summary().differenceSum(), is(0.0d));

        // --- Assert HTML report contains screenshot links ---
        String htmlReportText = Files.readString(reportHtml);
        assertTrue(htmlReportText.contains("<a href=\"screenshots/"),
                "HTML report should contain links to screenshots");

        // --- Assert file tracker has 4 contexts (2 alternating cookie sets × 2 devices) ---
        Path filesJson = tempDirectory.resolve("report/files.json");
        assertTrue(Files.exists(filesJson), "files.json (file tracker) should exist");

        FileTracker fileTracker = JacksonWrapper.readFileTrackerFile(filesJson.toFile());
        assertThat("Expected 4 screenshot contexts: 2 alternating-cookie sets × 2 device configs",
                fileTracker.contexts.size(), is(4));

        LOG.info("All assertions passed. differenceSum={}, contexts={}",
                report.summary().differenceSum(), fileTracker.contexts.size());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String resolveConfigPath(String resourcePath) {
        var url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Test resource not found on classpath: " + resourcePath);
        }
        return new File(url.getPath()).getAbsolutePath();
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
            throw new IllegalStateException("Key '" + key + "' not found in CDK outputs: " + json);
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

    private static void deleteDir(Path path) throws Exception {
        try (var pathStream = Files.walk(path)) {
            pathStream.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            try {
                                Files.delete(file.toPath());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    /**
     * An {@link java.io.OutputStream} that writes to two streams simultaneously –
     * one for the real console, one for capture.
     */
    static class MirrorOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream primary;
        private final java.io.OutputStream mirror;

        MirrorOutputStream(java.io.OutputStream primary, java.io.OutputStream mirror) {
            this.primary = primary;
            this.mirror  = mirror;
        }

        @Override
        public void write(int b) throws IOException {
            primary.write(b);
            mirror.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            primary.write(b, off, len);
            mirror.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            primary.flush();
            mirror.flush();
        }
    }
}
