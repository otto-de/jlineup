package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.sampling.NoSamplingStrategy;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JLineupHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    public JLineupHandlerTest() {
            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();
            builder.withSamplingStrategy(new NoSamplingStrategy());
            AWSXRay.setGlobalRecorder(builder.build());
    }

    @Test
    void invokeTest() {
        AWSXRay.beginSegment("jlineup-handler-test");


        JobConfig jobConfig = JobConfig.exampleConfigBuilder().withBrowser(Browser.Type.CHROME_HEADLESS).build();
        List<ScreenshotContext> screenshotContexts = BrowserUtils.buildScreenshotContextListFromConfigAndState(RunStepConfig.jLineupRunConfigurationBuilder().withStep(Step.before).build(), jobConfig);
        LambdaRequestPayload lambdaRequestPayload = new LambdaRequestPayload("someId", jobConfig, screenshotContexts.get(0));

        Context context = new TestContext();
        JLineupHandler handler = new JLineupHandler();
        String result = handler.handleRequest(lambdaRequestPayload, context);
        assertTrue(result.contains("Ok"));
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