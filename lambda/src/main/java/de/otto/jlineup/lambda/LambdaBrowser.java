package de.otto.jlineup.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.invoke.MethodHandles.lookup;

public class LambdaBrowser implements CloudBrowser {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final JobConfig jobConfig;
    private final FileService fileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public LambdaBrowser(JobConfig jobConfig, FileService fileService) {
        this.fileService = fileService;
        this.jobConfig = jobConfig;
    }

    @Override
    public void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException {

        String runId = UUID.randomUUID().toString();
        Set<Future<InvokeResponse>> lambdaCalls = new HashSet<>();

        for (ScreenshotContext screenshotContext : screenshotContexts) {

            InvokeRequest invokeRequest;
            try {
                invokeRequest = InvokeRequest.builder()
                        .functionName("jlineup-run")
                        .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(new LambdaRequestPayload(runId, jobConfig, screenshotContext)))).build();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            try (LambdaClient lambdaClient = LambdaClient.builder().credentialsProvider(DefaultCredentialsProvider.create()).region(Region.EU_CENTRAL_1).build()) {

                Future<InvokeResponse> invokeResponseFuture = executor.submit(() -> lambdaClient.invoke(invokeRequest));
                lambdaCalls.add(invokeResponseFuture);

            } catch (ServiceException e) {
                throw new RuntimeException(e);
            }
        }

        for (Future<InvokeResponse> lambdaCall : lambdaCalls) {
            InvokeResponse invokeResponse = lambdaCall.get();
            String answer = invokeResponse.payload().asUtf8String();
            String logResult = invokeResponse.logResult();
            //write out the return value
            System.out.println(answer);
            System.out.println(logResult);
        }

        try (S3TransferManager transferManager = S3TransferManager.builder().s3ClientConfiguration(S3ClientConfiguration.builder().credentialsProvider(DefaultCredentialsProvider.create()).region(Region.EU_CENTRAL_1).build()).build()) {
            transferManager.downloadFile(r -> r.destination(Paths.get("ft3", "bla")));
        }


    }
}
