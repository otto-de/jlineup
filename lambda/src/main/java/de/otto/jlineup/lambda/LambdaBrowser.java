package de.otto.jlineup.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.otto.jlineup.GlobalOption;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.CloudBrowser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ServiceException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.invoke.MethodHandles.lookup;

public class LambdaBrowser implements CloudBrowser {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private final JobConfig jobConfig;
    private final RunStepConfig runStepConfig;
    private final ExecutorService executor = Executors.newCachedThreadPool(Utils.createThreadFactory("LambdaBrowserSupervisorThread"));

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(MapperFeature.USE_ANNOTATIONS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private final FileService fileService;

    public LambdaBrowser(RunStepConfig runStepConfig, JobConfig jobConfig, FileService fileService) {
        this.fileService = fileService;
        this.runStepConfig = runStepConfig;
        this.jobConfig = jobConfig;
    }

    @Override
    public void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException, IOException {

        String runId = UUID.randomUUID().toString();
        HashMap<ScreenshotContext, Future<InvokeResponse>> lambdaCalls = new HashMap<>();

        LOG.info("Starting {} lambda calls for run '{}'...", screenshotContexts.size(), runId);
        final String s3Bucket;
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        try (LambdaClient lambdaClient = LambdaClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.EU_CENTRAL_1)
                .httpClientBuilder(ApacheHttpClient.builder()
                        .maxConnections(screenshotContexts.size() + 10)
                        .socketTimeout(Duration.ofSeconds(jobConfig.globalTimeout))
                        .connectionTimeout(Duration.ofSeconds(jobConfig.globalTimeout)))
                .build()) {
            s3Bucket = lambdaClient.getFunction(GetFunctionRequest.builder()
                            .functionName(GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME))
                            .build())
                    .configuration()
                    .environment()
                    .variables()
                    .get(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name());
            LOG.info("Using S3 bucket: {}", s3Bucket);
            for (ScreenshotContext screenshotContext : screenshotContexts) {
                Future<InvokeResponse> invokeResponseFuture = invokeLambdaAndGetInvokeResponseFuture(screenshotContext, runId, lambdaClient);
                lambdaCalls.put(screenshotContext, invokeResponseFuture);
            }

            LOG.info("All lambda calls started, waiting for results...");

            for (Map.Entry<ScreenshotContext, Future<InvokeResponse>> lambdaCall : lambdaCalls.entrySet()) {
                InvokeResponse invokeResponse = lambdaCall.getValue().get();
                String answer = invokeResponse.payload().asUtf8String();
                String logResult = invokeResponse.logResult();
                //write out the return value
                if (logResult != null) {
                    LOG.error("Log: {}", logResult);
                }
                if (answer.contains("errorMessage")) {
                    if (answer.contains("SessionNotCreatedException")
                            || answer.contains("disconnected: Unable to receive message from renderer")
                            || answer.contains("disconnected: not connected to DevTools")
                            || answer.contains("unknown error: unhandled inspector error")
                            || answer.contains("error writing PNG file")) {
                        LOG.warn("Retrying lambda call because of specific error message in answer: '{}'", answer);
                        //Do one retry if browser crashed in lambda
                        Future<InvokeResponse> invokeResponseFuture = invokeLambdaAndGetInvokeResponseFuture(lambdaCall.getKey(), runId, lambdaClient);
                        InvokeResponse invokeResponseRetry = invokeResponseFuture.get();
                        String retryAnswer = invokeResponseRetry.payload().asUtf8String();
                        if (retryAnswer.contains("errorMessage")) {
                            //LOG.error(retryAnswer); Is logged outside of this method by catching function
                            throw new RuntimeException("Lambda call failed even when retried: " + retryAnswer);
                        } else {
                            LOG.info("Answer from Lambda after retry: '{}'", retryAnswer);
                        }
                    } else {
                        //LOG.error(answer); Is logged outside of this method by catching function
                        throw new RuntimeException("Lambda call failed: " + answer);
                    }
                } else {
                    LOG.info("Answer from Lambda: '{}'", answer);
                }
            }

        } catch (ServiceException e) {
            LOG.error("Lambda call failed");
            throw new RuntimeException(e);
        }

        LOG.info("All lambda calls finished, starting download from S3 with transfer manager...");
        CompletableFuture<CompletedDirectoryDownload> download;
        Path localFolderOfS3Content = Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getReportDirectory(), "lambda-s3");
        try (S3TransferManager transferManager = S3TransferManager.builder().s3Client(S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).build()).build()) {
            download = transferManager.downloadDirectory(d -> d.bucket(s3Bucket).listObjectsV2RequestTransformer(l -> l.prefix("jlineup-" + runId)).destination(localFolderOfS3Content)).completionFuture();
        }
        LOG.info("Waiting for download to finish...");
        try {
            download.get();
        } catch (Exception e) {
            LOG.error("S3 Download failed", e);
            throw new RuntimeException(e);
        }
        LOG.info("Download finished, merging context file trackers into file tracker...");

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
                        } else if (f.getName().endsWith(".log")) {
                            Path lambdaLogPath = Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getReportDirectory(),
                                    "lambda.log");
                            boolean first = false;
                            if (!Files.exists(lambdaLogPath)) {
                                Files.createFile(lambdaLogPath);
                                first = true;
                            }
                            if (!first) {
                                Files.write(lambdaLogPath, "\n---\n\n".getBytes(), StandardOpenOption.APPEND);
                            }
                            Files.write(lambdaLogPath, Files.readAllBytes(f.toPath()), StandardOpenOption.APPEND);
                            f.delete();
                        } else {
                            Files.move(f.toPath(), Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(),
                                    f.getName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        LOG.info("Merging finished, cleaning up...");

        Files.delete(localFolderOfS3Content);

        LOG.info("All done. :D");

        executor.shutdownNow();
    }

    private Future<InvokeResponse> invokeLambdaAndGetInvokeResponseFuture(ScreenshotContext screenshotContext, String runId, LambdaClient lambdaClient) {
        InvokeRequest invokeRequest;
        try {
            invokeRequest = InvokeRequest.builder()
                    .functionName(GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME))
                    .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(
                            new LambdaRequestPayload(runId, jobConfig, screenshotContext, runStepConfig.getStep(), screenshotContext.urlKey))))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return executor.submit(() -> lambdaClient.invoke(invokeRequest));
    }
}
