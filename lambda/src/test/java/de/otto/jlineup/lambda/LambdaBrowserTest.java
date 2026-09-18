package de.otto.jlineup.lambda;

import de.otto.jlineup.GlobalOption;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LambdaBrowserTest {

    @TempDir
    Path tempDir;

    private LambdaBrowser lambdaBrowser;
    private JobConfig jobConfig;
    private RunStepConfig runStepConfig;
    private FileService fileService;
    private LambdaClient mockLambdaClient;
    private S3TransferManager mockTransferManager;
    private MockedStatic<GlobalOptions> mockedGlobalOptions;

    @BeforeEach
    void setUp() {
        // Setup test config
        jobConfig = JobConfig.jobConfigBuilder()
                .withUrls(Map.of("https://example.com", UrlConfig.urlConfigBuilder().build()))
                .withBrowser(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS)
                .withGlobalTimeout(300)
                .build();

        runStepConfig = RunStepConfig.runStepConfigBuilder()
                .withWorkingDirectory(tempDir.toString())
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .withStep(RunStep.before)
                .build();

        fileService = mock(FileService.class);

        // Mock GlobalOptions
        mockedGlobalOptions = mockStatic(GlobalOptions.class);
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME))
                .thenReturn("test-lambda-function");
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET))
                .thenReturn("test-s3-bucket");
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_AWS_REGION))
                .thenReturn("eu-central-1");

        lambdaBrowser = new LambdaBrowser(runStepConfig, jobConfig, fileService);
    }

    @AfterEach
    void tearDown() {
        if (mockedGlobalOptions != null) {
            mockedGlobalOptions.close();
        }
    }

    @Test
    void testTakeScreenshots_Success() throws Exception {
        // Prepare test data
        ScreenshotContext screenshotContext = createTestScreenshotContext();
        List<ScreenshotContext> screenshotContexts = Collections.singletonList(screenshotContext);

        // Create temporary directories and files
        Path lambdaS3Dir = tempDir.resolve("report/lambda-s3");
        Files.createDirectories(lambdaS3Dir);
        Path screenshotsDir = tempDir.resolve("screenshots");
        Files.createDirectories(screenshotsDir);
        Path screenshotDir = lambdaS3Dir.resolve("url1");
        Files.createDirectories(screenshotDir);
        Path screenshotFile = screenshotDir.resolve("screenshot.png");
        Files.write(screenshotFile, "test screenshot".getBytes());
        Path filesJson = lambdaS3Dir.resolve("files_test.json");
        Files.write(filesJson, "{}".getBytes());
        Path logFile = lambdaS3Dir.resolve("test.log");
        Files.write(logFile, "test log".getBytes());

        // Mock AWS SDK components
        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<S3TransferManager> mockedTransferManager = mockStatic(S3TransferManager.class);
             MockedStatic<S3AsyncClient> mockedS3AsyncClient = mockStatic(S3AsyncClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            setupMocks(mockedLambdaClient, mockedTransferManager, mockedS3AsyncClient, mockedCredentials,
                    "{\"status\":\"success\"}", 1);

            // Execute
            lambdaBrowser.takeScreenshots(screenshotContexts);

            // Verify
            verify(mockLambdaClient, times(1)).invoke(any(InvokeRequest.class));
            verify(fileService, times(1)).mergeContextFileTrackersIntoFileTracker(any(Path.class), any());
        }
    }

    @Test
    void testTakeScreenshots_WithRetryOnSessionNotCreatedException() throws Exception {
        // Prepare test data
        ScreenshotContext screenshotContext = createTestScreenshotContext();
        List<ScreenshotContext> screenshotContexts = Collections.singletonList(screenshotContext);

        // Create temporary directories
        Path lambdaS3Dir = tempDir.resolve("report/lambda-s3");
        Files.createDirectories(lambdaS3Dir);

        // Mock AWS SDK components
        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<S3TransferManager> mockedTransferManager = mockStatic(S3TransferManager.class);
             MockedStatic<S3AsyncClient> mockedS3AsyncClient = mockStatic(S3AsyncClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            // Setup mocks but with failing then succeeding response
            DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class);
            mockedCredentials.when(DefaultCredentialsProvider::builder).thenReturn(mock(DefaultCredentialsProvider.Builder.class));

            DefaultCredentialsProvider.Builder mockBuilder = mock(DefaultCredentialsProvider.Builder.class);
            mockedCredentials.when(DefaultCredentialsProvider::builder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockCredentialsProvider);

            mockLambdaClient = mock(LambdaClient.class);
            LambdaClientBuilder mockLambdaBuilder = mock(LambdaClientBuilder.class);
            mockedLambdaClient.when(LambdaClient::builder).thenReturn(mockLambdaBuilder);

            when(mockLambdaBuilder.credentialsProvider(any())).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.region(any(Region.class))).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.httpClientBuilder(any())).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.build()).thenReturn(mockLambdaClient);

            GetFunctionResponse mockGetFunctionResponse = GetFunctionResponse.builder()
                    .configuration(FunctionConfiguration.builder()
                            .environment(EnvironmentResponse.builder()
                                    .variables(Map.of(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name(), "test-s3-bucket"))
                                    .build())
                            .build())
                    .build();
            when(mockLambdaClient.getFunction(any(GetFunctionRequest.class)))
                    .thenReturn(mockGetFunctionResponse);

            // First call fails with SessionNotCreatedException, second call succeeds
            InvokeResponse failedInvokeResponse = InvokeResponse.builder()
                    .statusCode(200)
                    .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"SessionNotCreatedException: test error\"}"))
                    .build();

            InvokeResponse successInvokeResponse = InvokeResponse.builder()
                    .statusCode(200)
                    .payload(SdkBytes.fromUtf8String("{\"status\":\"success\"}"))
                    .build();

            when(mockLambdaClient.invoke(any(InvokeRequest.class)))
                    .thenReturn(failedInvokeResponse)
                    .thenReturn(successInvokeResponse);

            setupS3Mocks(mockedTransferManager, mockedS3AsyncClient, mockCredentialsProvider);

            // Execute
            lambdaBrowser.takeScreenshots(screenshotContexts);

            // Verify - should have called invoke twice (once failed, once retry)
            verify(mockLambdaClient, times(2)).invoke(any(InvokeRequest.class));
        }
    }

    @Test
    void testTakeScreenshots_FailsAfterRetry() throws Exception {
        // Prepare test data
        ScreenshotContext screenshotContext = createTestScreenshotContext();
        List<ScreenshotContext> screenshotContexts = Collections.singletonList(screenshotContext);

        // Mock AWS SDK components
        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            // Setup credentials
            DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class);
            DefaultCredentialsProvider.Builder mockBuilder = mock(DefaultCredentialsProvider.Builder.class);
            mockedCredentials.when(DefaultCredentialsProvider::builder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockCredentialsProvider);

            // Setup Lambda client
            mockLambdaClient = mock(LambdaClient.class);
            LambdaClientBuilder mockLambdaBuilder = mock(LambdaClientBuilder.class);
            mockedLambdaClient.when(LambdaClient::builder).thenReturn(mockLambdaBuilder);

            when(mockLambdaBuilder.credentialsProvider(any())).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.region(any(Region.class))).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.httpClientBuilder(any())).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.build()).thenReturn(mockLambdaClient);

            // Mock GetFunction response
            GetFunctionResponse mockGetFunctionResponse = GetFunctionResponse.builder()
                    .configuration(FunctionConfiguration.builder()
                            .environment(EnvironmentResponse.builder()
                                    .variables(Map.of(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name(), "test-s3-bucket"))
                                    .build())
                            .build())
                    .build();
            when(mockLambdaClient.getFunction(any(GetFunctionRequest.class)))
                    .thenReturn(mockGetFunctionResponse);

            // Both calls fail
            InvokeResponse failedInvokeResponse = InvokeResponse.builder()
                    .statusCode(200)
                    .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"SessionNotCreatedException: test error\"}"))
                    .build();

            when(mockLambdaClient.invoke(any(InvokeRequest.class)))
                    .thenReturn(failedInvokeResponse);

            // Execute and expect exception
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                lambdaBrowser.takeScreenshots(screenshotContexts);
            });

            assertTrue(exception.getMessage().contains("failed even when retried"));
            verify(mockLambdaClient, times(2)).invoke(any(InvokeRequest.class));
        }
    }

    @Test
    void testTakeScreenshots_FailsWithNonRetryableError() throws Exception {
        // Prepare test data
        ScreenshotContext screenshotContext = createTestScreenshotContext();
        List<ScreenshotContext> screenshotContexts = Collections.singletonList(screenshotContext);

        // Mock AWS SDK components
        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            // Setup credentials
            DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class);
            DefaultCredentialsProvider.Builder mockBuilder = mock(DefaultCredentialsProvider.Builder.class);
            mockedCredentials.when(DefaultCredentialsProvider::builder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockCredentialsProvider);

            // Setup Lambda client
            mockLambdaClient = mock(LambdaClient.class);
            LambdaClientBuilder mockLambdaBuilder = mock(LambdaClientBuilder.class);
            mockedLambdaClient.when(LambdaClient::builder).thenReturn(mockLambdaBuilder);

            when(mockLambdaBuilder.credentialsProvider(any())).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.region(any(Region.class))).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.httpClientBuilder(any())).thenReturn(mockLambdaBuilder);
            when(mockLambdaBuilder.build()).thenReturn(mockLambdaClient);

            // Mock GetFunction response
            GetFunctionResponse mockGetFunctionResponse = GetFunctionResponse.builder()
                    .configuration(FunctionConfiguration.builder()
                            .environment(EnvironmentResponse.builder()
                                    .variables(Map.of(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name(), "test-s3-bucket"))
                                    .build())
                            .build())
                    .build();
            when(mockLambdaClient.getFunction(any(GetFunctionRequest.class)))
                    .thenReturn(mockGetFunctionResponse);

            // Call fails with non-retryable error
            InvokeResponse failedInvokeResponse = InvokeResponse.builder()
                    .statusCode(200)
                    .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"Some other error\"}"))
                    .build();

            when(mockLambdaClient.invoke(any(InvokeRequest.class)))
                    .thenReturn(failedInvokeResponse);

            // Execute and expect exception
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                lambdaBrowser.takeScreenshots(screenshotContexts);
            });

            assertTrue(exception.getMessage().contains("Some other error"));
            // Should only call once, no retry for non-retryable errors
            verify(mockLambdaClient, times(1)).invoke(any(InvokeRequest.class));
        }
    }

    @Test
    void testTakeScreenshots_MultipleContexts() throws Exception {
        // Prepare test data
        ScreenshotContext context1 = createTestScreenshotContext();
        ScreenshotContext context2 = createTestScreenshotContext();
        List<ScreenshotContext> screenshotContexts = Arrays.asList(context1, context2);

        // Create temporary directories
        Path lambdaS3Dir = tempDir.resolve("report/lambda-s3");
        Files.createDirectories(lambdaS3Dir);

        // Mock AWS SDK components
        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<S3TransferManager> mockedTransferManager = mockStatic(S3TransferManager.class);
             MockedStatic<S3AsyncClient> mockedS3AsyncClient = mockStatic(S3AsyncClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            setupMocks(mockedLambdaClient, mockedTransferManager, mockedS3AsyncClient, mockedCredentials,
                    "{\"status\":\"success\"}", 2);

            // Execute
            lambdaBrowser.takeScreenshots(screenshotContexts);

            // Verify - should have called invoke for each context
            verify(mockLambdaClient, times(2)).invoke(any(InvokeRequest.class));
        }
    }

    @Test
    void testTakeScreenshots_UnpacksBundlesInsteadOfDownloadingEveryFile() throws Exception {
        ScreenshotContext screenshotContext = createTestScreenshotContext();
        List<ScreenshotContext> screenshotContexts = Collections.singletonList(screenshotContext);

        Files.createDirectories(tempDir.resolve("screenshots"));
        byte[] bundle = buildBundle(Map.of(
                "1234567/shot_before.png", "screenshot-bytes",
                "files_before_1234567.json", "{}"));

        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<S3TransferManager> mockedTransferManager = mockStatic(S3TransferManager.class);
             MockedStatic<S3AsyncClient> mockedS3AsyncClient = mockStatic(S3AsyncClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            setupMocks(mockedLambdaClient, mockedTransferManager, mockedS3AsyncClient, mockedCredentials,
                    "{\"status\":\"success\"}", 1);

            String bundleKey = "jlineup-runid/bundle_1234567_before.zip";
            S3Client mockS3Client = stubS3Listing(bundleKey);
            when(mockS3Client.getObject(ArgumentMatchers.<Consumer<GetObjectRequest.Builder>>any()))
                    .thenAnswer(i -> new ResponseInputStream<>(GetObjectResponse.builder().build(), new ByteArrayInputStream(bundle)));

            lambdaBrowser.takeScreenshots(screenshotContexts);

            // The bundle was fetched and extracted, the legacy per-file download was not used at all
            verify(mockS3Client, times(1)).getObject(ArgumentMatchers.<Consumer<GetObjectRequest.Builder>>any());
            verify(mockTransferManager, never()).downloadDirectory(any(java.util.function.Consumer.class));
            verify(fileService, times(1)).mergeContextFileTrackersIntoFileTracker(any(Path.class), any());

            // ...and the extracted content ended up where mergeLambdaContextsIntoLocalFileStructure puts it
            assertEquals("screenshot-bytes", Files.readString(tempDir.resolve("screenshots/1234567/shot_before.png")));
            assertEquals("{}", Files.readString(tempDir.resolve("screenshots/files_before_1234567.json")));
        }
    }

    private static byte[] buildBundle(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private void setupMocks(MockedStatic<LambdaClient> mockedLambdaClient,
                           MockedStatic<S3TransferManager> mockedTransferManager,
                           MockedStatic<S3AsyncClient> mockedS3AsyncClient,
                           MockedStatic<DefaultCredentialsProvider> mockedCredentials,
                           String responsePayload,
                           int invocationCount) {

        // Setup credentials
        DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class);
        DefaultCredentialsProvider.Builder mockBuilder = mock(DefaultCredentialsProvider.Builder.class);
        mockedCredentials.when(DefaultCredentialsProvider::builder).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockCredentialsProvider);

        // Setup Lambda client
        mockLambdaClient = mock(LambdaClient.class);
        LambdaClientBuilder mockLambdaBuilder = mock(LambdaClientBuilder.class);
        mockedLambdaClient.when(LambdaClient::builder).thenReturn(mockLambdaBuilder);

        when(mockLambdaBuilder.credentialsProvider(any())).thenReturn(mockLambdaBuilder);
        when(mockLambdaBuilder.region(any(Region.class))).thenReturn(mockLambdaBuilder);
        when(mockLambdaBuilder.httpClientBuilder(any())).thenReturn(mockLambdaBuilder);
        when(mockLambdaBuilder.build()).thenReturn(mockLambdaClient);

        // Mock GetFunction response
        GetFunctionResponse mockGetFunctionResponse = GetFunctionResponse.builder()
                .configuration(FunctionConfiguration.builder()
                        .environment(EnvironmentResponse.builder()
                                .variables(Map.of(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name(), "test-s3-bucket"))
                                .build())
                        .build())
                .build();
        when(mockLambdaClient.getFunction(any(GetFunctionRequest.class)))
                .thenReturn(mockGetFunctionResponse);

        // Mock Lambda invoke response
        InvokeResponse mockInvokeResponse = InvokeResponse.builder()
                .statusCode(200)
                .payload(SdkBytes.fromUtf8String(responsePayload))
                .build();
        when(mockLambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(mockInvokeResponse);

        setupS3Mocks(mockedTransferManager, mockedS3AsyncClient, mockCredentialsProvider);
    }

    private void setupS3Mocks(MockedStatic<S3TransferManager> mockedTransferManager,
                             MockedStatic<S3AsyncClient> mockedS3AsyncClient,
                             DefaultCredentialsProvider mockCredentialsProvider) {
        // By default the bucket looks empty, so LambdaBrowser falls back to the legacy per-file download.
        stubS3Listing();

        // Setup S3 Transfer Manager
        mockTransferManager = mock(S3TransferManager.class);
        S3TransferManager.Builder mockTransferManagerBuilder = mock(S3TransferManager.Builder.class);
        mockedTransferManager.when(S3TransferManager::builder).thenReturn(mockTransferManagerBuilder);
        when(mockTransferManagerBuilder.s3Client(any())).thenReturn(mockTransferManagerBuilder);
        when(mockTransferManagerBuilder.build()).thenReturn(mockTransferManager);

        // Mock S3 CRT AsyncClient
        S3AsyncClient mockS3AsyncClient = mock(S3AsyncClient.class);
        S3CrtAsyncClientBuilder mockS3Builder = mock(S3CrtAsyncClientBuilder.class);
        mockedS3AsyncClient.when(() -> S3AsyncClient.crtBuilder()).thenReturn(mockS3Builder);
        when(mockS3Builder.credentialsProvider(any())).thenReturn(mockS3Builder);
        when(mockS3Builder.build()).thenReturn(mockS3AsyncClient);

        // Mock download
        DirectoryDownload mockDirectoryDownload = mock(DirectoryDownload.class);
        CompletedDirectoryDownload mockCompletedDownload = mock(CompletedDirectoryDownload.class);
        CompletableFuture<CompletedDirectoryDownload> completableFuture = CompletableFuture.completedFuture(mockCompletedDownload);

        when(mockTransferManager.downloadDirectory(any(java.util.function.Consumer.class)))
                .thenReturn(mockDirectoryDownload);
        when(mockDirectoryDownload.completionFuture()).thenReturn(completableFuture);
    }

    /**
     * Stubs {@code listObjectsV2} to report the given keys. An empty list means "no bundles", which makes
     * LambdaBrowser take the legacy per-file download path.
     */
    private S3Client stubS3Listing(String... keys) {
        S3Client mockS3Client = mock(S3Client.class);
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                        .contents(Arrays.stream(keys).map(k -> S3Object.builder().key(k).build()).toList())
                        .isTruncated(false)
                        .build());
        lambdaBrowser.s3ClientFactory = credentialsProvider -> mockS3Client;
        return mockS3Client;
    }

    private ScreenshotContext createTestScreenshotContext() {        return createTestScreenshotContext(null);
    }

    private ScreenshotContext createTestScreenshotContext(de.otto.jlineup.browser.Browser.Type browserType) {
        DeviceConfig deviceConfig = DeviceConfig.deviceConfigBuilder()
                .withWidth(800)
                .withHeight(600)
                .build();

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        return ScreenshotContext.of(
                "https://example.com",
                "/",
                deviceConfig,
                BrowserStep.before,
                urlConfig,
                Collections.emptyList(),
                "url1",
                browserType
        );
    }

    // --- resolveLambdaFunctionName tests ---

    @Test
    void resolveLambdaFunctionName_legacyFallback() {
        // Only JLINEUP_LAMBDA_FUNCTION_NAME is set (already mocked in setUp)
        String result = lambdaBrowser.resolveLambdaFunctionName(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS);
        assertEquals("test-lambda-function", result);
    }

    @Test
    void resolveLambdaFunctionName_perBrowserOptionWins() {
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS))
                .thenReturn("my-chrome-lambda");

        String result = lambdaBrowser.resolveLambdaFunctionName(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS);
        assertEquals("my-chrome-lambda", result);
    }

    @Test
    void resolveLambdaFunctionName_baseNameSlug() {
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE))
                .thenReturn("jlineup");
        // Per-browser option not set → base name wins over legacy
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS))
                .thenReturn(null);

        String result = lambdaBrowser.resolveLambdaFunctionName(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS);
        assertEquals("jlineup-chrome-headless", result);
    }

    @Test
    void resolveLambdaFunctionName_baseNameSlugFirefox() {
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE))
                .thenReturn("jlineup");
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_FIREFOX_HEADLESS))
                .thenReturn(null);

        String result = lambdaBrowser.resolveLambdaFunctionName(de.otto.jlineup.browser.Browser.Type.FIREFOX_HEADLESS);
        assertEquals("jlineup-firefox-headless", result);
    }

    @Test
    void resolveLambdaFunctionName_baseNameSlugWebkit() {
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE))
                .thenReturn("jlineup");
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_WEBKIT_HEADLESS))
                .thenReturn(null);

        String result = lambdaBrowser.resolveLambdaFunctionName(de.otto.jlineup.browser.Browser.Type.WEBKIT_HEADLESS);
        assertEquals("jlineup-webkit-headless", result);
    }

    @Test
    void resolveLambdaFunctionName_perBrowserTakesPrecedenceOverBase() {
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE))
                .thenReturn("jlineup");
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS))
                .thenReturn("explicit-chrome-lambda");

        String result = lambdaBrowser.resolveLambdaFunctionName(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS);
        assertEquals("explicit-chrome-lambda", result);
    }

    @Test
    void validateLambdaFunctionNames_throwsWhenNoneConfigured() {
        // Clear all function name options
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME))
                .thenReturn(null);
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE))
                .thenReturn(null);
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS))
                .thenReturn(null);

        ScreenshotContext ctx = createTestScreenshotContext(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS);

        assertThrows(IllegalStateException.class, () ->
                lambdaBrowser.takeScreenshots(Collections.singletonList(ctx)));
    }

    @Test
    void takeScreenshots_routesToCorrectFunctionPerBrowser() throws Exception {
        // Setup per-browser function names
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS))
                .thenReturn("jlineup-chrome-headless");
        mockedGlobalOptions.when(() -> GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_FIREFOX_HEADLESS))
                .thenReturn("jlineup-firefox-headless");

        ScreenshotContext chromeCtx = createTestScreenshotContext(de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS);
        ScreenshotContext firefoxCtx = createTestScreenshotContext(de.otto.jlineup.browser.Browser.Type.FIREFOX_HEADLESS);
        List<ScreenshotContext> contexts = Arrays.asList(chromeCtx, firefoxCtx);

        Path lambdaS3Dir = tempDir.resolve("report/lambda-s3");
        Files.createDirectories(lambdaS3Dir);

        try (MockedStatic<LambdaClient> mockedLambdaClient = mockStatic(LambdaClient.class);
             MockedStatic<S3TransferManager> mockedTransferManager = mockStatic(S3TransferManager.class);
             MockedStatic<S3AsyncClient> mockedS3AsyncClient = mockStatic(S3AsyncClient.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentials = mockStatic(DefaultCredentialsProvider.class)) {

            setupMocks(mockedLambdaClient, mockedTransferManager, mockedS3AsyncClient, mockedCredentials,
                    "{\"status\":\"success\"}", 2);

            lambdaBrowser.takeScreenshots(contexts);

            // Capture all InvokeRequests and verify they used the correct function names
            org.mockito.ArgumentCaptor<InvokeRequest> captor =
                    org.mockito.ArgumentCaptor.forClass(InvokeRequest.class);
            verify(mockLambdaClient, times(2)).invoke(captor.capture());

            List<String> invokedFunctions = captor.getAllValues().stream()
                    .map(InvokeRequest::functionName)
                    .sorted()
                    .toList();
            assertEquals(List.of("jlineup-chrome-headless", "jlineup-firefox-headless"), invokedFunctions);
        }
    }
}