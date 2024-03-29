package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.sampling.NoSamplingStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JLineupHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JLineupHandlerTest() {
            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();
            builder.withSamplingStrategy(new NoSamplingStrategy());
            AWSXRay.setGlobalRecorder(builder.build());
    }

    @Test
    @Disabled("Until fixed in pipeline, maybe with localstack etc.")
    void invokeTest() throws IOException {
        AWSXRay.beginSegment("jlineup-handler-test");

        JobConfig jobConfig = JobConfig.exampleConfigBuilder().withBrowser(Browser.Type.CHROME_HEADLESS).build();
        List<ScreenshotContext> screenshotContextsBefore = BrowserUtils.buildScreenshotContextListFromConfigAndState(RunStepConfig.runStepConfigBuilder().withStep(RunStep.before).build(), jobConfig);
        List<ScreenshotContext> screenshotContextsAfter = BrowserUtils.buildScreenshotContextListFromConfigAndState(RunStepConfig.runStepConfigBuilder().withStep(RunStep.after_only).build(), jobConfig);

        Context context = new TestContext();
        JLineupHandler handler = new JLineupHandler();

        for (ScreenshotContext screenshotContext : screenshotContextsBefore) {
            LambdaRequestPayload lambdaRequestPayload = new LambdaRequestPayload("someId", jobConfig, screenshotContext, RunStep.before, screenshotContext.urlKey);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            handler.handleRequest(new ByteArrayInputStream(objectMapper.writeValueAsBytes(lambdaRequestPayload)), output, context);
            assertTrue(output.toString().contains("Ok"));
        }

        for (ScreenshotContext screenshotContext : screenshotContextsAfter) {
            LambdaRequestPayload lambdaRequestPayload = new LambdaRequestPayload("someId", jobConfig, screenshotContext, RunStep.after_only, screenshotContext.urlKey);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            handler.handleRequest(new ByteArrayInputStream(objectMapper.writeValueAsBytes(lambdaRequestPayload)), output, context);
            assertTrue(output.toString().contains("Ok"));
        }

        AWSXRay.endSegment();
    }

    static class TestContext implements Context {

        public TestContext() {
        }

        public String getAwsRequestId() {
            return "awsRequestId";
        }

        public String getLogGroupName() {
            return "/aws/lambda/my-function";
        }

        public String getLogStreamName() {
            return "2021/03/28/[$LATEST]abcde";
        }

        public String getFunctionName() {
            return "my-function";
        }

        public String getFunctionVersion() {
            return "$LATEST";
        }

        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:eu-central-1:123456789012:function:my-function";
        }

        public CognitoIdentity getIdentity() {
            return null;
        }

        public ClientContext getClientContext() {
            return null;
        }

        public int getRemainingTimeInMillis() {
            return 300000;
        }

        public int getMemoryLimitInMB() {
            return 2048;
        }

        public LambdaLogger getLogger() {
            return new TestLogger();
        }
    }

    static class TestLogger implements LambdaLogger {

        public TestLogger(){}
        public void log(String message){
            logger.info(message);
        }
        public void log(byte[] message){
            logger.info(new String(message));
        }
    }

}