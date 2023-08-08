package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
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
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class JLineupHandler implements RequestStreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private S3TransferManager transferManager;

    static {
        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try {
            LambdaRequestPayload event = objectMapper.readValue(input, LambdaRequestPayload.class);
            ScreenshotContext screenshotContext = ScreenshotContext.copyOfBuilder(event.screenshotContext).withStep(event.step.toBrowserStep()).withUrlConfig(event.jobConfig.urls.get(event.screenshotContext.url)).build();
            LambdaRunner runner = createRun(event.runId, event.step, event.jobConfig, screenshotContext);
            runner.run();

            AwsCredentialsProviderChain cp = AwsCredentialsProviderChain
                    .builder()
                    .credentialsProviders(
                            // instance profile is also needed for people not using ecs but directly using ec2 instances!!
                            ContainerCredentialsProvider.builder().build(),
                            //InstanceProfileCredentialsProvider.builder().build(),
                            EnvironmentVariableCredentialsProvider.create(),
                            ProfileCredentialsProvider
                                    .builder()
                                    .profileName(LambdaProperties.getProfile())
                                    .build())
                    .build();

            transferManager = S3TransferManager.builder().s3Client(S3AsyncClient.crtBuilder().credentialsProvider(cp).build()).build();

            CompletableFuture<CompletedDirectoryUpload> uploadStatus = transferManager.uploadDirectory(r -> r.bucket("jlineuptest-marco").source(Paths.get("/tmp/jlineup/run-" + event.runId))).completionFuture();
            System.out.println(uploadStatus.get().toString());
            output.write("Ok!".getBytes(StandardCharsets.UTF_8));
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
                        "--headless=new",
                        "--disable-gpu",
                        "--no-sandbox",
                        "--use-spdy=off",
                        "--disable-dev-shm-usage",
                        "--disable-web-security",
                        "--no-zygote",
                        "--force-color-profile=srgb",
                        "--user-data-dir=/tmp/jlineup/chrome-profile-" + id))
                .withStep(step)
                .build();
        return new LambdaRunner(jobConfig, runStepConfig, screenshotContext);
    }

}