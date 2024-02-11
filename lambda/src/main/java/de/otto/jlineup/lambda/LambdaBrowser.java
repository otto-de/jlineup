package de.otto.jlineup.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.CloudBrowser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ServiceException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.utils.FileUtils;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;

import static de.otto.jlineup.lambda.LambdaProperties.getProfile;
import static java.lang.invoke.MethodHandles.lookup;

public class LambdaBrowser implements CloudBrowser {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final JobConfig jobConfig;

    private final RunStepConfig runStepConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private final FileService fileService;

    public LambdaBrowser(RunStepConfig runStepConfig, JobConfig jobConfig, FileService fileService) {
        this.fileService = fileService;
        this.runStepConfig = runStepConfig;
        this.jobConfig = jobConfig;
    }

    @Override
    public void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException, IOException {

        String runId = UUID.randomUUID().toString();
        Set<Future<InvokeResponse>> lambdaCalls = new HashSet<>();

        try (LambdaClient lambdaClient = LambdaClient.builder().credentialsProvider(DefaultCredentialsProvider.create()).region(Region.EU_CENTRAL_1).build()) {
            for (ScreenshotContext screenshotContext : screenshotContexts) {

                InvokeRequest invokeRequest;
                try {
                    invokeRequest = InvokeRequest.builder()
                            .functionName("jlineup-lambda")
                            .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(new LambdaRequestPayload(runId, jobConfig, screenshotContext, runStepConfig.getStep(), screenshotContext.urlKey)))).build();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                Future<InvokeResponse> invokeResponseFuture = executor.submit(() -> lambdaClient.invoke(invokeRequest));
                lambdaCalls.add(invokeResponseFuture);
            }

            for (Future<InvokeResponse> lambdaCall : lambdaCalls) {
                InvokeResponse invokeResponse = lambdaCall.get();
                String answer = invokeResponse.payload().asUtf8String();
                String logResult = invokeResponse.logResult();
                //write out the return value
                System.out.println("Answer: " + answer);
                System.out.println("Log: " + logResult);

                if (answer.contains("errorMessage")) {
                    throw new RuntimeException("Lambda call failed: " + answer);
                }
            }

        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        AwsCredentialsProvider cp = null;
        try {
            cp = AWSConfig.defaultAwsCredentialsProvider(getProfile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CompletableFuture<CompletedDirectoryDownload> download;
        Path localFolderOfS3Content = Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getReportDirectory(), "lambda-s3");
        try (S3TransferManager transferManager = S3TransferManager.builder().s3Client(S3AsyncClient.crtBuilder().credentialsProvider(cp).build()).build()) {
            download = transferManager.downloadDirectory(d -> d.bucket("jlineuptest-marco").listObjectsV2RequestTransformer(l -> l.prefix("jlineup-" + runId)).destination(localFolderOfS3Content)).completionFuture();
        }

        download.get();

        fileService.mergeContextFileTrackersIntoFileTracker(localFolderOfS3Content, (d, name) -> name.startsWith("files_") && name.endsWith(".json"));
        Arrays.stream(Objects.requireNonNull(localFolderOfS3Content.toFile().listFiles()))
                .forEach(f -> {
                    try {
                        if (f.isDirectory()) {
                            Arrays.stream(f.listFiles()).toList().forEach(ff -> {
                                try {
                                    Files.createDirectories(Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(), f.getName()));
                                    Files.move(ff.toPath(), Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(), f.getName(), ff.getName()), StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            f.delete();
                        } else {
                            Files.move(f.toPath(), Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(),
                                    f.getName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        Files.delete(localFolderOfS3Content);

        executor.shutdownNow();
    }
}
