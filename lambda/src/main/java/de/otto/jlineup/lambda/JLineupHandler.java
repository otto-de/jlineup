package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static de.otto.jlineup.GlobalOption.JLINEUP_LAMBDA_AWS_PROFILE;
import static de.otto.jlineup.GlobalOption.JLINEUP_LAMBDA_S3_BUCKET;
import static de.otto.jlineup.JLineupRunner.LOGFILE_NAME;
import static de.otto.jlineup.browser.BrowserUtils.getFullPathOfReportDir;

public class JLineupHandler implements RequestStreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupHandler.class);

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(MapperFeature.USE_ANNOTATIONS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private S3TransferManager transferManager;

    static {
        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try {
            LambdaRequestPayload event = objectMapper.readValue(input, LambdaRequestPayload.class);
            ScreenshotContext screenshotContext = ScreenshotContext.copyOfBuilder(event.screenshotContext).withStep(event.step.toBrowserStep()).withUrlKey(event.urlKey).withUrlConfig(event.jobConfig.urls.get(event.screenshotContext.urlKey)).build();
            LambdaRunner runner = createRun(event.runId, event.step == RunStep.after ? RunStep.after_only : event.step, event.jobConfig, screenshotContext);
            int retries = runner.run();

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

            transferManager = S3TransferManager.builder().s3Client(S3AsyncClient.crtBuilder().credentialsProvider(cp).build()).build();

            Path logfile = Paths.get(getFullPathOfReportDir(runner.getRunStepConfig()) + "/" + LOGFILE_NAME);
            Path workingDir = Paths.get("/tmp/jlineup/run-" + event.runId);
            if (Files.exists(logfile)) {
                Files.move(logfile, Paths.get(getFullPathOfReportDir(runner.getRunStepConfig()) + "/context_" + screenshotContext.contextHash() + "_" + LOGFILE_NAME));
            }
            String bucketName = GlobalOptions.getOption(JLINEUP_LAMBDA_S3_BUCKET);
            if (bucketName == null) {
                throw new RuntimeException("Environment variable JLINEUP_LAMBDA_S3_BUCKET not set! Please create a bucket and set the environment variable to contain it's name.");
            }
            CompletableFuture<CompletedDirectoryUpload> uploadStatus = transferManager.uploadDirectory(r -> r.bucket(bucketName).source(workingDir)).completionFuture();

            //Block until upload is completed
            CompletedDirectoryUpload completedDirectoryUpload = uploadStatus.get();

            output.write(("Upload status: " + completedDirectoryUpload.toString() + "\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Ok! (Retries: " + retries + ")").getBytes(StandardCharsets.UTF_8));

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

    private LambdaRunner createRun(String id, RunStep step, JobConfig jobConfig, ScreenshotContext screenshotContext) {
        RunStepConfig runStepConfig = RunStepConfig.runStepConfigBuilder()
                .withWebDriverCachePath("/tmp/jlineup/webdrivers")
                .withWorkingDirectory("/tmp/jlineup/run-{id}".replace("{id}", id))
                .withScreenshotsDirectory("jlineup-{id}".replace("{id}", id))
                .withReportDirectory("jlineup-{id}".replace("{id}", id))
                .withChromeParameters(ImmutableList.of(
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
                        "--user-data-dir=/tmp/jlineup/chrome-profile-" + id))
                .withStep(step)
                .build();
        return new LambdaRunner(id, jobConfig, runStepConfig, screenshotContext);
    }

}