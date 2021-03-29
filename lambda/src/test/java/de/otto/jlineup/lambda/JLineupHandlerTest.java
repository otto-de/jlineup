package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.sampling.NoSamplingStrategy;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JLineupHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    public JLineupHandlerTest() {
            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();
            builder.withSamplingStrategy(new NoSamplingStrategy());
            AWSXRay.setGlobalRecorder(builder.build());
    }

    @Test
    void invokeTest() throws IOException {
        AWSXRay.beginSegment("jlineup-handler-test");

        HashMap<String, String> event = new HashMap<>();
        event.put("payload", "hello world!");

        Context context = new TestContext();
        JLineupHandler handler = new JLineupHandler();
        String result = handler.handleRequest(event, context);
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