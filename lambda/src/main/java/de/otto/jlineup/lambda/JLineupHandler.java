package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static de.otto.jlineup.GlobalOption.*;
import static de.otto.jlineup.JLineupRunner.LOGFILE_NAME;
import static de.otto.jlineup.browser.BrowserUtils.getFullPathOfReportDir;

public class JLineupHandler implements RequestStreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupHandler.class);

    private final JsonMapper jsonMapper = JacksonWrapper.jsonMapperForLambdaHandler();

    /**
     * Built once per container and then kept for its whole lifetime.
     *
     * <p>Creating it per invocation used to leak: {@link S3TransferManager#close()} only closes the
     * {@link S3AsyncClient} if the transfer manager created it itself, and we pass one in explicitly.
     * Every orphaned CRT client keeps its "sdk-ScheduledExecutor" pool (5 threads, no core thread timeout)
     * plus native CRT resources alive, and AWS reuses a warm container for hours worth of invocations.
     *
     * <p>Building it once also keeps the credential chain lookup and the CRT native setup off the hot path.
     * It is intentionally never closed – there is exactly one per container, and the container dying
     * releases it.
     */
    private static volatile S3TransferManager transferManager;

    static {
        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();
    }

    private static S3TransferManager transferManager() {
        S3TransferManager result = transferManager;
        if (result == null) {
            synchronized (JLineupHandler.class) {
                result = transferManager;
                if (result == null) {
                    AwsCredentialsProviderChain cp = AwsCredentialsProviderChain
                            .builder()
                            .credentialsProviders(
                                    // instance profile is also needed for people not using ecs but directly using ec2 instances!!
                                    ContainerCredentialsProvider.builder().build(),
                                    //InstanceProfileCredentialsProvider.builder().build(),
                                    EnvironmentVariableCredentialsProvider.create(),
                                    ProfileCredentialsProvider
                                            .builder()
                                            .profileName(GlobalOptions.getOption(JLINEUP_LAMBDA_AWS_PROFILE))
                                            .build())
                            .build();

                    result = S3TransferManager.builder()
                            .s3Client(S3AsyncClient.crtBuilder().credentialsProvider(cp).build())
                            .build();
                    transferManager = result;
                }
            }
        }
        return result;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try {
            LambdaRequestPayload event = jsonMapper.readValue(input, LambdaRequestPayload.class);
            ScreenshotContext screenshotContext = ScreenshotContext.copyOfBuilder(event.screenshotContext()).withStep(event.step().toBrowserStep()).withUrlKey(event.urlKey()).withUrlConfig(event.jobConfig().urls.get(event.urlKey())).build();
            LambdaRunner runner = createRun(event.runId(), event.step() == RunStep.after ? RunStep.after_only : event.step(), event.jobConfig(), screenshotContext);
            int retries = runner.run();

            Path logfile = Paths.get(getFullPathOfReportDir(runner.getRunStepConfig()) + "/" + LOGFILE_NAME);
            Path workingDir = Paths.get("/tmp/jlineup/run-" + event.runId());
            if (Files.exists(logfile)) {
                Files.move(logfile, Paths.get(getFullPathOfReportDir(runner.getRunStepConfig()) + "/context_" + screenshotContext.contextHash() + "_" + LOGFILE_NAME));
            }
            String bucketName = GlobalOptions.getOption(JLINEUP_LAMBDA_S3_BUCKET);
            if (bucketName == null) {
                throw new RuntimeException("Environment variable JLINEUP_LAMBDA_S3_BUCKET not set! Please create a bucket and set the environment variable to contain it's name.");
            }
            String s3Prefix = GlobalOptions.getOption(JLINEUP_LAMBDA_S3_PREFIX);

            String uploadStatus = isBundlingEnabled()
                    ? uploadAsSingleBundle(bucketName, s3Prefix, event.runId(), runner.getRunStepConfig(), screenshotContext.contextHash(), event.step())
                    : uploadEverySingleFile(bucketName, s3Prefix, workingDir);

            output.write(("OK! S3 upload status: " + uploadStatus + " - Retries: " + retries).getBytes(StandardCharsets.UTF_8));

            // Introduced to avoid the following error: "java.lang.RuntimeException: java.nio.file.FileSystemException: /tmp/jlineup/run-c5f6232e-4e76-4b39-90f0-151ff69223f9/jlineup-c5f6232e-4e76-4b39-90f0-151ff69223f9/150886105: No space left on device","errorType":"java.lang.RuntimeException","stackTrace":["de.otto.jlineup.lambda.JLineupHandler.handleRequest(JLineupHandler.java:90)","java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)","java.base/java.lang.reflect.Method.invoke(Unknown Source)"]}
            //	at de.otto.jlineup.lambda.LambdaBrowser.takeScreenshots(LambdaBrowser.java:105)
            //	at de.otto.jlineup.browser.Browser.runSetupAndTakeScreenshots(Browser.java:195)
            //	at de.otto.jlineup.JLineupRunner.run(JLineupRunner.java:64)
            // ...
            FileUtils.deleteDirectory(workingDir);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isBundlingEnabled() {
        return !"false".equalsIgnoreCase(GlobalOptions.getOption(JLINEUP_LAMBDA_S3_BUNDLE));
    }

    /**
     * Packs all artifacts of this invocation into a single ZIP and uploads it as one S3 object.
     *
     * <p>The screenshots are dominated by PNGs and therefore barely shrink, but replacing one PUT per
     * screenshot file with a single multipart upload removes most of the round trips – and the upload
     * blocks the handler, so that time is billed as Lambda duration.
     *
     * <p>The bundle is written next to (not inside) the working directory and removed again right after
     * the upload, so the peak usage of the 1 GiB ephemeral storage grows only by the size of the
     * screenshots of this one context.
     */
    private String uploadAsSingleBundle(String bucketName, String s3Prefix, String runId, RunStepConfig runStepConfig,
                                        String contextHash, RunStep step) throws Exception {
        Path reportDir = Paths.get(getFullPathOfReportDir(runStepConfig));
        String bundleFileName = ScreenshotBundle.bundleFileName(contextHash, step);
        Path bundle = Paths.get("/tmp/jlineup", "upload-" + runId + "-" + bundleFileName);

        try {
            long zipStart = System.currentTimeMillis();
            int entries = ScreenshotBundle.zipDirectory(reportDir, bundle);
            long bundleSize = Files.size(bundle);
            LOG.info("Bundled {} file(s) into '{}' ({} bytes) in {} ms", entries, bundleFileName, bundleSize, System.currentTimeMillis() - zipStart);

            String key = ScreenshotBundle.s3KeyPrefixForRun(s3Prefix, runId) + "/" + bundleFileName;
            long uploadStart = System.currentTimeMillis();
            transferManager().uploadFile(u -> u
                            .source(bundle)
                            .putObjectRequest(p -> p.bucket(bucketName).key(key).contentType(ScreenshotBundle.BUNDLE_CONTENT_TYPE)))
                    .completionFuture()
                    //Block until upload is completed
                    .get();
            LOG.info("Uploaded bundle to 's3://{}/{}' in {} ms", bucketName, key, System.currentTimeMillis() - uploadStart);

            return String.format("bundle='%s', entries=%d, bytes=%d", key, entries, bundleSize);
        } finally {
            Files.deleteIfExists(bundle);
        }
    }

    /**
     * Legacy upload: one S3 object per file. Kept behind {@code JLINEUP_LAMBDA_S3_BUNDLE=false} as an
     * escape hatch, and understood by every core version.
     */
    private String uploadEverySingleFile(String bucketName, String s3Prefix, Path workingDir) throws Exception {
        long uploadStart = System.currentTimeMillis();
        CompletableFuture<CompletedDirectoryUpload> uploadStatus = transferManager()
                .uploadDirectory(r -> r.bucket(bucketName).source(workingDir).s3Prefix(s3Prefix))
                .completionFuture();

        //Block until upload is completed
        CompletedDirectoryUpload completedDirectoryUpload = uploadStatus.get();
        LOG.info("Uploaded working directory file by file in {} ms", System.currentTimeMillis() - uploadStart);
        return completedDirectoryUpload.toString();
    }

    private LambdaRunner createRun(String id, RunStep step, JobConfig jobConfig, ScreenshotContext screenshotContext) {
        Browser.Type browserType = screenshotContext.browserType != null
                ? screenshotContext.browserType
                : jobConfig.getBrowser();

        RunStepConfig.Builder builder = RunStepConfig.runStepConfigBuilder()
                .withWebDriverCachePath("/tmp/jlineup/webdrivers")
                .withWorkingDirectory("/tmp/jlineup/run-{id}".replace("{id}", id))
                .withScreenshotsDirectory("jlineup-{id}".replace("{id}", id))
                .withReportDirectory("jlineup-{id}".replace("{id}", id))
                .withStep(step);

        if (browserType.isChrome() || browserType.isChromium()) {
            builder.withChromeParameters(ImmutableList.of(
                    "--single-process",
                    "--headless=new",
                    "--enable-logging",
                    "--v=1",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--use-spdy=off",
                    "--disable-dev-shm-usage",
                    "--disable-web-security",
                    "--no-zygote",
                    "--force-color-profile=srgb",
                    "--hide-scrollbars",
                    "--user-data-dir=/tmp/jlineup/chrome-profile-" + id));
        } else if (browserType.isFirefox()) {
            builder.withFirefoxParameters(ImmutableList.of(
                    "--headless",
                    "--no-sandbox"));
        } else if (browserType.isWebkit()) {
            // WebKit has no extra CLI args in Lambda; it is handled via WebKitDriverManager
            LOG.info("Using WebKit browser in Lambda for run '{}'", id);
        } else {
            LOG.warn("Unrecognized browser type '{}' in Lambda, falling back to Chrome parameters", browserType);
            builder.withChromeParameters(ImmutableList.of(
                    "--single-process",
                    "--headless=new",
                    "--enable-logging",
                    "--v=1",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--use-spdy=off",
                    "--disable-dev-shm-usage",
                    "--disable-web-security",
                    "--no-zygote",
                    "--force-color-profile=srgb",
                    "--hide-scrollbars",
                    "--user-data-dir=/tmp/jlineup/chrome-profile-" + id));
        }

        return new LambdaRunner(id, jobConfig, builder.build(), screenshotContext);
    }

}