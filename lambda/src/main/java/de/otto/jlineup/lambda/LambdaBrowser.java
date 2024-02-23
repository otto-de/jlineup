package de.otto.jlineup.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ServiceException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;

import java.io.IOException;
import java.nio.file.*;
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
        Set<Future<InvokeResponse>> lambdaCalls = new HashSet<>();

        LOG.info("Starting {} lambda calls for run '{}'...", screenshotContexts.size(), runId);

        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        try (LambdaClient lambdaClient = LambdaClient.builder().credentialsProvider(credentialsProvider).region(Region.EU_CENTRAL_1).build()) {
            for (ScreenshotContext screenshotContext : screenshotContexts) {

                InvokeRequest invokeRequest;
                try {
                    invokeRequest = InvokeRequest.builder()
                            .functionName("jlineup-lambda")
                            .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(new LambdaRequestPayload(runId, jobConfig, screenshotContext, runStepConfig.getStep(), screenshotContext.urlKey)))).build();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

//                System.out.println(objectMapper.writeValueAsString(new LambdaRequestPayload(runId, jobConfig, screenshotContext, runStepConfig.getStep(), screenshotContext.urlKey)));
//                LambdaRequestPayload event = objectMapper.readValue(objectMapper.writeValueAsString(new LambdaRequestPayload(runId, jobConfig, screenshotContext, runStepConfig.getStep(), screenshotContext.urlKey)), LambdaRequestPayload.class);
//                System.err.println(event.screenshotContext.urlKey);
//
//                System.exit(0);

                Future<InvokeResponse> invokeResponseFuture = executor.submit(() -> lambdaClient.invoke(invokeRequest));
                lambdaCalls.add(invokeResponseFuture);
            }

            LOG.info("All lambda calls started, waiting for results...");

            for (Future<InvokeResponse> lambdaCall : lambdaCalls) {
                InvokeResponse invokeResponse = lambdaCall.get();
                String answer = invokeResponse.payload().asUtf8String();
                String logResult = invokeResponse.logResult();
                //write out the return value
                LOG.info("Answer:  {}", answer);
                if (logResult != null) {
                    LOG.error("Log: {}", logResult);
                }
                if (answer.contains("errorMessage")) {
                    throw new RuntimeException("Lambda call failed: " + answer);
                }
            }

        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        LOG.info("All lambda calls finished, starting download from S3...");

        LOG.info("Starting transfer manager...");
        CompletableFuture<CompletedDirectoryDownload> download;
        Path localFolderOfS3Content = Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getReportDirectory(), "lambda-s3");
        try (S3TransferManager transferManager = S3TransferManager.builder().s3Client(S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).build()).build()) {
            download = transferManager.downloadDirectory(d -> d.bucket("jlineuptest-marco").listObjectsV2RequestTransformer(l -> l.prefix("jlineup-" + runId)).destination(localFolderOfS3Content)).completionFuture();
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

        executor.shutdownNow();
    }
}
