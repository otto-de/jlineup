package de.otto.jlineup.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.json.Jackson;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JLineupHandler implements RequestHandler<LambdaRequestPayload, String> {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupHandler.class);

    static {
        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();
    }

    @Override
    public String handleRequest(LambdaRequestPayload event, Context context) {
        try {

            LOG.info("Event: " + Jackson.getObjectMapper().writer().writeValueAsString(event));

            // Download the config from S3
            /*
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(
                    srcBucket, srcKey));
            InputStream objectData = s3Object.getObjectContent();
            */

            // Upload all created files to bucket
            /*
            LOG.info("Writing to: " + dstBucket + "/" + dstKey);
            try {
                s3Client.putObject(dstBucket, dstKey, is, meta);
            } catch (AmazonServiceException e) {
                LOG.error(e.getErrorMessage());
                System.exit(1);
            }
            */

            LambdaRunner runner = createRun(event.runId, event.jobConfig, event.screenshotContext);
            runner.run();
            return "Ok!";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LambdaRunner createRun(String id, JobConfig jobConfig, ScreenshotContext screenshotContext) {
        RunStepConfig runStepConfig = RunStepConfig.runStepConfigBuilder()
                .withWebDriverCachePath("/tmp/jlineup/webdrivers")
                .withWorkingDirectory("/tmp/jlineup")
                .withScreenshotsDirectory("jlineup-{id}".replace("{id}", id))
                .withReportDirectory("jlineup-{id}".replace("{id}", id))
                .withChromeParameters(ImmutableList.of("--use-spdy=off", "--disable-dev-shm-usage", "--disable-web-security", "--user-data-dir=/tmp/jlineup/chrome-profile-" + id))
                .withStep(screenshotContext.step)
                .build();
        return new LambdaRunner(jobConfig, runStepConfig, screenshotContext);
    }
}