package de.otto.jlineup.lambda;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import de.otto.jlineup.*;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.CloudBrowser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.invoke.MethodHandles.lookup;

public class LambdaBrowser implements CloudBrowser {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final int MAX_PARALLEL_DOWNLOADS = 16;

    private final JobConfig jobConfig;
    private final RunStepConfig runStepConfig;
    private final ExecutorService executor = Executors.newCachedThreadPool(Utils.createThreadFactory("LambdaBrowserSupervisorThread"));

    private final JsonMapper jsonMapper = JacksonWrapper.jsonMapperForLambdaHandler();

    private final FileService fileService;

    /**
     * Seam that lets tests inject a stubbed S3 client. Production code always uses
     * {@link #defaultS3Client(AwsCredentialsProvider)}.
     */
    @VisibleForTesting
    S3ClientFactory s3ClientFactory = LambdaBrowser::defaultS3Client;

    @FunctionalInterface
    interface S3ClientFactory {
        S3Client create(AwsCredentialsProvider credentialsProvider);
    }

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
        //The credentials provider is an SdkAutoCloseable that owns the http clients of the underlying provider
        //chain, so it has to be closed explicitly. It is not closed by the clients it is handed to.
        try (DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build()) {
            takeScreenshots(screenshotContexts, credentialsProvider);
        } finally {
            //Has to happen in a finally block: if anything above throws, the supervisor threads are still
            //blocked in LambdaClient.invoke() (up to globalTimeout, 1800s by default) and would otherwise
            //never be interrupted. shutdownNow() interrupts the in-flight invocations as well.
            executor.shutdownNow();
        }
    }

    private void takeScreenshots(List<ScreenshotContext> screenshotContexts, AwsCredentialsProvider credentialsProvider) throws ExecutionException, InterruptedException, IOException {

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
                    if (isTransientLambdaError(answer)) {
                        LOG.warn("[{}] [{}] Retrying lambda call because of specific error message in answer: '{}'", indexString, functionName, answer);
                        //Do one retry if browser crashed in lambda
                        Future<InvokeResponse> invokeResponseFuture = invokeLambdaAndGetInvokeResponseFuture(lambdaCall.getKey(), runId, lambdaClient);
                        InvokeResponse invokeResponseRetry = invokeResponseFuture.get();
                        String retryAnswer = invokeResponseRetry.payload().asUtf8String();
                        if (retryAnswer.contains("errorMessage")) {
                            String userMessage = extractLambdaUserMessage(retryAnswer, indexString, functionName);
                            throw new RuntimeException("Lambda failed even when retried: " + userMessage);
                        } else {
                            LOG.info("[{}] [{}] Answer from Lambda after retry: '{}'", indexString, functionName, retryAnswer);
                        }
                    } else {
                        String userMessage = extractLambdaUserMessage(answer, indexString, functionName);
                        throw new RuntimeException(userMessage);
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
    }

    private void mergeLambdaContextsIntoLocalFileStructure(Path localFolderOfS3Content) throws IOException {
        LOG.info("Merging context file trackers into file tracker...");
        if (LOG.isDebugEnabled()) {
            LOG.info("Download directory: '{}' (exists={}, isDir={})", localFolderOfS3Content, java.nio.file.Files.exists(localFolderOfS3Content), java.nio.file.Files.isDirectory(localFolderOfS3Content));
            if (java.nio.file.Files.exists(localFolderOfS3Content)) {
                try (var stream = java.nio.file.Files.list(localFolderOfS3Content)) {
                    stream.forEach(p -> LOG.info("  Entry: {} (isDir={})", p.getFileName(), java.nio.file.Files.isDirectory(p)));
                }
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
        Path localFolderOfS3Content = Paths.get(this.runStepConfig.getWorkingDirectory(), this.runStepConfig.getReportDirectory(), "lambda-s3");
        final String prefix = ScreenshotBundle.s3KeyPrefixForRun(s3Prefix, runId);

        LOG.info("All lambda calls finished, listing S3 objects with prefix '{}'...", prefix);
        List<String> keys;
        try (S3Client s3Client = buildS3Client(credentialsProvider)) {
            keys = listAllKeys(s3Client, s3Bucket, prefix);
            if (keys.stream().anyMatch(ScreenshotBundle::isBundleKey)) {
                downloadAndUnpack(s3Client, s3Bucket, prefix, keys, localFolderOfS3Content);
                return localFolderOfS3Content;
            }
        } catch (Exception e) {
            LOG.error("S3 download failed", e);
            throw new RuntimeException(e);
        }

        LOG.info("No bundles found under prefix '{}' ({} object(s)), the lambdas seem to run an older JLineup version. " +
                "Falling back to downloading every single file.", prefix, keys.size());
        return downloadEverySingleFileFromS3(credentialsProvider, s3Bucket, prefix, localFolderOfS3Content);
    }

    private S3Client buildS3Client(AwsCredentialsProvider credentialsProvider) {
        return s3ClientFactory.create(credentialsProvider);
    }

    static S3Client defaultS3Client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_AWS_REGION)))
                .build();
    }

    private static List<String> listAllKeys(S3Client s3Client, String s3Bucket, String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;
        do {
            final String token = continuationToken;
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(s3Bucket)
                    .prefix(prefix)
                    .continuationToken(token)
                    .build());
            response.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> !key.endsWith("/"))
                    .forEach(keys::add);
            continuationToken = Boolean.TRUE.equals(response.isTruncated()) ? response.nextContinuationToken() : null;
        } while (continuationToken != null);
        return keys;
    }

    /**
     * Downloads every object of the run and, for bundles, extracts it straight from the HTTP response into
     * the target directory – no intermediate archive ever hits the local disk.
     *
     * <p>Plain objects are handled too, because during a rolling deployment the per-browser lambda functions
     * can temporarily run different JLineup versions, so a single run may produce a mix of bundles and
     * loose files.
     */
    private void downloadAndUnpack(S3Client s3Client, String s3Bucket, String prefix, List<String> keys, Path localFolderOfS3Content) throws IOException {
        long start = System.currentTimeMillis();
        long bundleCount = keys.stream().filter(ScreenshotBundle::isBundleKey).count();
        LOG.info("Downloading {} bundle(s) and {} loose object(s) into '{}'...", bundleCount, keys.size() - bundleCount, localFolderOfS3Content);

        Files.createDirectories(localFolderOfS3Content);
        ExecutorService downloadPool = Executors.newFixedThreadPool(
                Math.min(MAX_PARALLEL_DOWNLOADS, keys.size()), Utils.createThreadFactory("LambdaS3DownloadThread"));
        List<Future<Integer>> results = new ArrayList<>(keys.size());
        try {
            for (String key : keys) {
                results.add(downloadPool.submit(() -> downloadSingleObject(s3Client, s3Bucket, prefix, key, localFolderOfS3Content)));
            }
            int extractedFiles = 0;
            for (Future<Integer> result : results) {
                try {
                    extractedFiles += result.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while downloading from S3", e);
                } catch (ExecutionException e) {
                    throw new IOException("Download from S3 failed: " + e.getCause().getMessage(), e.getCause());
                }
            }
            LOG.info("Download finished. {} object(s) yielded {} file(s) in {} ms.", keys.size(), extractedFiles, System.currentTimeMillis() - start);
        } finally {
            downloadPool.shutdownNow();
        }
    }

    private static int downloadSingleObject(S3Client s3Client, String s3Bucket, String prefix, String key, Path localFolderOfS3Content) throws IOException {
        try (InputStream in = s3Client.getObject(g -> g.bucket(s3Bucket).key(key))) {
            if (ScreenshotBundle.isBundleKey(key)) {
                return ScreenshotBundle.unzipInto(in, localFolderOfS3Content);
            }
            //Loose object from an older lambda version: mirror the key below the run prefix, exactly like
            //the transfer manager's downloadDirectory would have done.
            String relativeKey = key.substring(prefix.length()).replaceFirst("^/+", "");
            Path target = ScreenshotBundle.resolveSafely(localFolderOfS3Content.toAbsolutePath().normalize(), relativeKey);
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return 1;
        }
    }

    /**
     * Legacy path, used when no lambda of this run produced a bundle. Unchanged behaviour.
     */
    private Path downloadEverySingleFileFromS3(AwsCredentialsProvider credentialsProvider, String s3Bucket, String prefix, Path localFolderOfS3Content) {
        LOG.info("S3 download prefix: '{}', destination: '{}'", prefix, localFolderOfS3Content);

        //The S3AsyncClient has to be closed by us: S3TransferManager.close() only closes the async client if the
        //transfer manager created it itself. Because we pass one in explicitly, an unclosed client would leak its
        //"sdk-ScheduledExecutor" pool (5 threads, no core thread timeout) on every single run.
        try (S3AsyncClient s3AsyncClient = S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).build();
             S3TransferManager transferManager = S3TransferManager.builder().s3Client(s3AsyncClient).build()) {
            CompletableFuture<CompletedDirectoryDownload> download = transferManager
                    .downloadDirectory(d -> d.bucket(s3Bucket).listObjectsV2RequestTransformer(l -> l.prefix(prefix)).destination(localFolderOfS3Content))
                    .completionFuture();
            LOG.info("Waiting for download to finish...");
            CompletedDirectoryDownload result = download.get();
            LOG.info("Download finished. Failed transfers: {}", result.failedTransfers().size());
            result.failedTransfers().forEach(ft -> LOG.error("  Failed transfer: {} - {}", ft.request().getObjectRequest().key(), ft.exception().getMessage()));
        } catch (Exception e) {
            LOG.error("S3 Download failed", e);
            throw new RuntimeException(e);
        }
        return localFolderOfS3Content;
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

    /**
     * Extracts a user-friendly error message from a Lambda response.
     * Uses Utils.extractUserFriendlyErrorMessage to parse JSON error responses
     * and extract the actual error message (e.g., JLineupException messages).
     *
     * @param answer the raw Lambda response containing the error
     * @param indexString the index string for context
     * @param functionName the Lambda function name for context
     * @return a formatted user-friendly error message
     */
    private String extractLambdaUserMessage(String answer, String indexString, String functionName) {
        String extractedMessage = Utils.extractLambdaErrorMessage(answer);
        if (extractedMessage != null) {
            // Check if the extracted message contains a JLineupException
            String jlineupMessage = Utils.extractJLineupExceptionMessage(extractedMessage);
            if (jlineupMessage != null) {
                return String.format("Lambda [%s] [%s]: %s", indexString, functionName, jlineupMessage);
            }
            return String.format("Lambda [%s] [%s]: %s", indexString, functionName, extractedMessage);
        }
        // Fallback to the raw answer if we couldn't extract a cleaner message
        return String.format("Lambda [%s] [%s] failed: %s", indexString, functionName, answer);
    }

    public static boolean isTransientLambdaError(String answer) {
        return answer.contains("SessionNotCreatedException")
                || answer.contains("disconnected: Unable to receive message from renderer")
                || answer.contains("Timed out receiving message from renderer")
                || answer.contains("disconnected: not connected to DevTools")
                || answer.contains("unknown error: unhandled inspector error")
                || answer.contains("Task timed out after")
                || answer.contains("error writing PNG file");
    }
}
