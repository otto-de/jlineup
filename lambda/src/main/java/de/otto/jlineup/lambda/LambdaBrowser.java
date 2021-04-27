package de.otto.jlineup.lambda;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.json.Jackson;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.browser.CloudBrowser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
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

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public LambdaBrowser(JobConfig jobConfig, FileService fileService) {
        this.fileService = fileService;
        this.jobConfig = jobConfig;
    }

    @Override
    public void takeScreenshots(List<ScreenshotContext> screenshotContexts) throws ExecutionException, InterruptedException {

        String runId = UUID.randomUUID().toString();
        Set<Future<InvokeResult>> lambdaCalls = new HashSet<>();

        for (ScreenshotContext screenshotContext : screenshotContexts) {

            InvokeRequest invokeRequest = new InvokeRequest()
                    .withFunctionName("jlineup-run")
                    .withPayload(Jackson.toJsonString(new LambdaRequestPayload(runId, jobConfig, screenshotContext)));

            try {
                AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
                        .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                        .withRegion(Regions.EU_CENTRAL_1).build();

                Future<InvokeResult> invokeResultFuture = executor.submit(() -> awsLambda.invoke(invokeRequest));
                lambdaCalls.add(invokeResultFuture);

            } catch (ServiceException e) {
                System.out.println(e.toString());
            }
        }

        for (Future<InvokeResult> lambdaCall : lambdaCalls) {
            InvokeResult invokeResult = lambdaCall.get();
            String answer = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
            String logResult = invokeResult.getLogResult();
            //write out the return value
            System.out.println(answer);
            System.out.println(logResult);
        }

        AmazonS3 s3Client = AmazonS3Client.builder().withCredentials(InstanceProfileCredentialsProvider.getInstance()).build();

    }
}
