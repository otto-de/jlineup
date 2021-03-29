package de.otto.jlineup.lambda;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static de.otto.jlineup.browser.BrowserUtils.getFullPathOfReportDir;
import static de.otto.jlineup.browser.BrowserUtils.prepareDomain;
import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;

public class JLineupHandler implements RequestHandler<Map<String,String>, String> {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupHandler.class);

    static {
        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        try {

            Jackson.getObjectMapper().registerModule(new JodaModule());
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
            return exampleRun("https://www.otto.de", "FIREFOX_HEADLESS");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String exampleRun(String url,
                             String browser) throws Exception {

        if (url == null) {
            url = "https://www.example.com";
        }

        JobConfig.Builder jobConfigBuilder = JobConfig.jobConfigBuilder().withBrowser(Browser.Type.CHROME_HEADLESS).withName("Example run").withUrls(ImmutableMap.of(url, urlConfigBuilder().withDevices(Collections.singletonList(DeviceConfig.deviceConfig(1920, 1080))).build()));
        if (browser != null) {
            try {
                Browser.Type type = Browser.Type.forValue(browser);
                jobConfigBuilder.withBrowser(type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        LambdaRunner lambdaRunner = createRun("lambdaRunnerId", jobConfigBuilder.build(), Step.before);
        boolean run = lambdaRunner.run();

        return "Ok! Example run successful with 'before' step with Browser '" + jobConfigBuilder.build().browser + "'.";
    }

    private LambdaRunner createRun(String id, JobConfig jobConfig, Step step) throws Exception {
        //JobConfig webJobConfig = sanitizeJobConfig(jobConfig);
        WebDriverManager.globalConfig().setCachePath("/tmp/jlineup/webdrivers");
        RunStepConfig runStepConfig = RunStepConfig.jLineupRunConfigurationBuilder()
                .withWorkingDirectory("/tmp")
                .withScreenshotsDirectory("jlineup-{id}".replace("{id}", id))
                .withReportDirectory("jlineup-{id}".replace("{id}", id))
                .withChromeParameters(ImmutableList.of("--use-spdy=off", "--disable-dev-shm-usage", "--disable-web-security", "--user-data-dir=/tmp/jlineup/chrome-profile-" + id))
                .withStep(step)
                .build();

        Map.Entry<String, UrlConfig> urlConfigEntry = jobConfig.urls.entrySet().stream().findFirst().get();
        String firstUrl = urlConfigEntry.getKey();
        UrlConfig urlConfig = urlConfigEntry.getValue();
        return new LambdaRunner(jobConfig, runStepConfig, new ScreenshotContext(prepareDomain(runStepConfig, firstUrl), urlConfig.paths.get(0), urlConfig.devices.get(0),
                runStepConfig.getStep(), urlConfig, getFullPathOfReportDir(runStepConfig), true, firstUrl));
    }
}