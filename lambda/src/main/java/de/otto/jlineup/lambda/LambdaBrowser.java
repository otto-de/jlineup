package de.otto.jlineup.lambda;

import com.google.common.base.Strings;
import de.otto.jlineup.*;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.CloudBrowser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ServiceException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

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

    private final JsonMapper jsonMapper = JacksonWrapper.jsonMapperForLambdaHandler();

    private final FileService fileService;

    public LambdaBrowser(RunStepConfig runStepConfig, JobConfig jobConfig, FileService fileService) {
        this.fileService = fileService;
        this.runStepConfig = runStepConfig;
        this.jobConfig = jobConfig;
    }

    /**
     * Resolves the Lambda function name for a given browser type.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Per-browser explicit option (e.g. {@code JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS})</li>
     *   <li>Base name + browser slug (e.g. {@code JLINEUP_LAMBDA_FUNCTION_NAME_BASE} → {@code mybase-chrome-headless})</li>
     *   <li>Legacy fallback: {@code JLINEUP_LAMBDA_FUNCTION_NAME}</li>
     * </ol>
     *
     * @param browserType the browser type to resolve the function name for
     * @return the Lambda function name, or {@code null} if none is configured
     */
    String resolveLambdaFunctionName(Browser.Type browserType) {
        // 1. Per-browser explicit override
        GlobalOption perBrowserOption = perBrowserGlobalOption(browserType);
        if (perBrowserOption != null) {
            String perBrowser = GlobalOptions.getOption(perBrowserOption);
            if (perBrowser != null) {
                return perBrowser;
            }
        }

        // 2. Base name + browser slug
        String base = GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE);
        if (base != null) {
            String slug = browserType != null ? browserType.name().toLowerCase().replace("_", "-") : "chrome-headless";
            return base + "-" + slug;
        }

        // 3. Legacy fallback
        return GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME);
    }

    private static GlobalOption perBrowserGlobalOption(Browser.Type browserType) {
        if (browserType == null) return null;
        return switch (browserType) {
            case CHROME_HEADLESS -> GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS;
            case FIREFOX_HEADLESS -> GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_FIREFOX_HEADLESS;
            case WEBKIT_HEADLESS -> GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_WEBKIT_HEADLESS;
            default -> null;
        };
    }

    private void validateLambdaFunctionNamesConfigured(List<ScreenshotContext> screenshotContexts) {
        screenshotContexts.stream()
                .map(ctx -> ctx.browserType)
                .distinct()
                .forEach(browserType -> {
                    if (resolveLambdaFunctionName(browserType) == null) {
                        throw new IllegalStateException(
                                "No Lambda function name configured for browser type '" + browserType + "'. " +
                                "Set JLINEUP_LAMBDA_FUNCTION_NAME_BASE, a per-browser option like " +
                                "JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS, or the legacy JLINEUP_LAMBDA_FUNCTION_NAME.");
                    }
                });
    }

    @Override
    public void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException, IOException {

        validateLambdaFunctionNamesConfigured(screenshotContexts);

        String runId = UUID.randomUUID().toString();
        HashMap<ScreenshotContext, Future<InvokeResponse>> lambdaCalls = new HashMap<>();

        LOG.info("Starting {} lambda calls for run '{}' using function(s): {}", screenshotContexts.size(), runId,
                screenshotContexts.stream()
                        .map(ctx -> resolveLambdaFunctionName(ctx.browserType))
                        .distinct()
                        .collect(java.util.stream.Collectors.joining(", ")));
        final String s3Bucket;
        final String s3Prefix;
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();

        // Use the function name of the first context to read the shared S3 config.
        // All browser-specific Lambda functions are expected to share the same S3 bucket/prefix.
        String s3ConfigFunctionName = resolveLambdaFunctionName(screenshotContexts.get(0).browserType);

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_AWS_REGION)))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .maxConnections(screenshotContexts.size() + 10)
                        .socketTimeout(Duration.ofSeconds(jobConfig.globalTimeout))
                        .connectionTimeout(Duration.ofSeconds(jobConfig.globalTimeout)))
                .build()) {
            GetFunctionResponse s3ConfigFunction = lambdaClient.getFunction(GetFunctionRequest.builder()
                    .functionName(s3ConfigFunctionName)
                    .build());
            s3Bucket = s3ConfigFunction
                    .configuration()
                    .environment()
                    .variables()
                    .get(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name());
            LOG.info("Using S3 bucket: {}", s3Bucket);
            s3Prefix = s3ConfigFunction
                    .configuration()
                    .environment()
                    .variables()
                    .get(GlobalOption.JLINEUP_LAMBDA_S3_PREFIX.name());
            LOG.info("Using S3 prefix: {}", s3Prefix);
            for (ScreenshotContext screenshotContext : screenshotContexts) {
                Future<InvokeResponse> invokeResponseFuture = invokeLambdaAndGetInvokeResponseFuture(screenshotContext, runId, lambdaClient);
                lambdaCalls.put(screenshotContext, invokeResponseFuture);
            }

            LOG.info("All {} lambda calls started, waiting for results...", screenshotContexts.size());

            int i = 0;
            int digits = String.valueOf(screenshotContexts.size()).length();
            for (Map.Entry<ScreenshotContext, Future<InvokeResponse>> lambdaCall : lambdaCalls.entrySet()) {
                i++;
                String indexString = Strings.padStart(String.valueOf(i), digits, '0');
                String functionName = resolveLambdaFunctionName(lambdaCall.getKey().browserType);
                InvokeResponse invokeResponse = lambdaCall.getValue().get();
                String answer = invokeResponse.payload().asUtf8String();
                String logResult = invokeResponse.logResult();
                //write out the return value
                if (logResult != null) {
                    LOG.error("[{}] [{}] Log: {}", indexString, functionName, logResult);
                }
                if (answer.contains("errorMessage")) {
                    if (answer.contains("SessionNotCreatedException")
                            || answer.contains("disconnected: Unable to receive message from renderer")
                            || answer.contains("disconnected: not connected to DevTools")
                            || answer.contains("unknown error: unhandled inspector error")
                            || answer.contains("Task timed out after")
                            || answer.contains("error writing PNG file")) {
                        LOG.warn("[{}] [{}] Retrying lambda call because of specific error message in answer: '{}'", indexString, functionName, answer);
                        //Do one retry if browser crashed in lambda
                        Future<InvokeResponse> invokeResponseFuture = invokeLambdaAndGetInvokeResponseFuture(lambdaCall.getKey(), runId, lambdaClient);
                        InvokeResponse invokeResponseRetry = invokeResponseFuture.get();
                        String retryAnswer = invokeResponseRetry.payload().asUtf8String();
                        if (retryAnswer.contains("errorMessage")) {
                            //LOG.error(retryAnswer); Is logged outside of this method by catching function
                            throw new RuntimeException("Lambda call [" + indexString + "] [" + functionName + "] failed even when retried: " + retryAnswer);
                        } else {
                            LOG.info("[{}] [{}] Answer from Lambda after retry: '{}'", indexString, functionName, retryAnswer);
                        }
                    } else {
                        //LOG.error(answer); Is logged outside of this method by catching function
                        throw new RuntimeException("Lambda call [" + indexString + "] [" + functionName + "] failed: " + answer);
                    }
                } else {
                    LOG.info("[{}] [{}] Answer from Lambda: '{}'", indexString, functionName, answer);
                }
            }

        } catch (ServiceException e) {
            LOG.error("Lambda call failed");
            throw new RuntimeException(e);
        }

        Path localFolderOfS3Content = downloadFilesFromS3(credentialsProvider, s3Bucket, s3Prefix, runId);
        mergeLambdaContextsIntoLocalFileStructure(localFolderOfS3Content);


        LOG.info("Cleaning up temporary downloaded files...");
        fileService.deleteRecursively(localFolderOfS3Content);
        LOG.info("All done. :D");

        executor.shutdownNow();
    }

    private void mergeLambdaContextsIntoLocalFileStructure(Path localFolderOfS3Content) throws IOException {
        LOG.info("Merging context file trackers into file tracker...");
        LOG.info("Download directory: '{}' (exists={}, isDir={})", localFolderOfS3Content, java.nio.file.Files.exists(localFolderOfS3Content), java.nio.file.Files.isDirectory(localFolderOfS3Content));
        if (java.nio.file.Files.exists(localFolderOfS3Content)) {
            try (var stream = java.nio.file.Files.list(localFolderOfS3Content)) {
                stream.forEach(p -> LOG.info("  Entry: {} (isDir={})", p.getFileName(), java.nio.file.Files.isDirectory(p)));
            }
        }
        fileService.mergeContextFileTrackersIntoFileTracker(localFolderOfS3Content, (d, name) -> name.startsWith("files_") && name.endsWith(".json"));
        Arrays.stream(Objects.requireNonNull(localFolderOfS3Content.toFile().listFiles()))
                .forEach(f -> {
                    try {
                        if (f.isDirectory()) {
                            Arrays.stream(Objects.requireNonNull(f.listFiles())).toList().forEach(ff -> {
                                try {
                                    Files.createDirectories(Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(), f.getName()));
                                    Files.move(ff.toPath(), Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(), f.getName(), ff.getName()), StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                    throw new RuntimeException(e);
                                }
                            });
                            if (!f.delete()) {
                                LOG.warn("Could not delete temporary folder {}", f.getAbsolutePath());
                            }
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
                            if (!f.delete()) {
                                LOG.warn("Could not delete temporary file {}", f.getAbsolutePath());
                            }
                        } else {
                            Files.move(f.toPath(), Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getScreenshotsDirectory(),
                                    f.getName()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                });

        LOG.info("Merging finished.");
    }

    private Path downloadFilesFromS3(AwsCredentialsProvider credentialsProvider, String s3Bucket, String s3Prefix, String runId) {
        LOG.info("All lambda calls finished, starting download from S3 with transfer manager...");
        CompletableFuture<CompletedDirectoryDownload> download;
        Path localFolderOfS3Content = Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getReportDirectory(), "lambda-s3");

        final String prefix = buildS3Prefix(s3Prefix, runId);
        LOG.info("S3 download prefix: '{}', destination: '{}'", prefix, localFolderOfS3Content);

        try (S3TransferManager transferManager = S3TransferManager.builder().s3Client(S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).build()).build()) {
            download = transferManager.downloadDirectory(d -> d.bucket(s3Bucket).listObjectsV2RequestTransformer(l -> l.prefix(prefix)).destination(localFolderOfS3Content)).completionFuture();
        }
        LOG.info("Waiting for download to finish...");
        try {
            CompletedDirectoryDownload result = download.get();
            LOG.info("Download finished. Failed transfers: {}", result.failedTransfers().size());
            result.failedTransfers().forEach(ft -> LOG.error("  Failed transfer: {} - {}", ft.request().getObjectRequest().key(), ft.exception().getMessage()));
        } catch (Exception e) {
            LOG.error("S3 Download failed", e);
            throw new RuntimeException(e);
        }
        return localFolderOfS3Content;
    }

    private static @NonNull String buildS3Prefix(String s3Prefix, String runId) {
        String prefix = s3Prefix;
        if (prefix != null) {
            prefix = prefix.endsWith("/") ? prefix : prefix + "/";
            prefix = prefix + "jlineup-" + runId;
        } else {
            prefix = "jlineup-" + runId;
        }
        return prefix;
    }

    private Future<InvokeResponse> invokeLambdaAndGetInvokeResponseFuture(ScreenshotContext screenshotContext, String runId, LambdaClient lambdaClient) {
        InvokeRequest invokeRequest;
        try {
            String functionName = resolveLambdaFunctionName(screenshotContext.browserType);
            LOG.debug("Routing screenshotContext (browser={}) to Lambda function '{}'", screenshotContext.browserType, functionName);
            invokeRequest = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(jsonMapper.writeValueAsString(
                            new LambdaRequestPayload(runId, jobConfig, screenshotContext, runStepConfig.getStep(), screenshotContext.urlKey))))
                    .build();
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
        return executor.submit(() -> lambdaClient.invoke(invokeRequest));
    }
}
