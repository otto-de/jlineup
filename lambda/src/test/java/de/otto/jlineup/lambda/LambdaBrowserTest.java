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
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

            assertTrue(exception.getMessage().contains("Lambda call failed"));
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

    private ScreenshotContext createTestScreenshotContext() {
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
                "url1"
        );
    }
}