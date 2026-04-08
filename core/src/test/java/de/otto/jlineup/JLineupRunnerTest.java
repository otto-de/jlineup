package de.otto.jlineup;

import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.report.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JLineupRunnerTest {

    @Test
    void shouldNotFailIfMaxDiffIsSameAsDetectedDiff() {

        UrlReport urlReport = new UrlReport("abc", "abc", new Summary(false, 0, 15.1234567890123456, 0), Collections.emptyList());
        Report report = new Report(new Summary(false, 0, 15.1234567890123456, 0), JobConfig.exampleConfigBuilder().withUrls(Collections.singletonMap("abc", UrlConfig.urlConfigBuilder().withMaxDiff(15.1234567890123456).build())).build(), List.of(urlReport), Map.of(BrowserStep.before, "SomeBrowser"));
        boolean detectedDifferenceGreaterThanMaxDifference = JLineupRunner.isDetectedDifferenceGreaterThanMaxDifference(report, JobConfig.exampleConfigBuilder().withUrls(Collections.singletonMap("abc", UrlConfig.urlConfigBuilder().withMaxDiff(15.1234567890123456).build())).build());

        assertThat(detectedDifferenceGreaterThanMaxDifference, is(false));
    }

    @Test
    void shouldNotFailWhenContextIsFlakyAccepted() {
        ScreenshotContext ctx = ScreenshotContext.of("https://example.com", "/page",
                DeviceConfig.deviceConfig(800, 600), BrowserStep.before,
                UrlConfig.urlConfigBuilder().build(), Collections.emptyList(), "abc");

        // Context has a difference but is flaky-accepted
        ContextReport flakyContext = new ContextReport(ctx.contextHash(), ctx,
                new Summary(true, 0.05, 0.05, 0), Collections.emptyList(), true);

        UrlReport urlReport = new UrlReport("abc", "abc",
                new Summary(true, 0.05, 0.05, 0), List.of(flakyContext));

        JobConfig config = JobConfig.jobConfigBuilder()
                .withUrls(Collections.singletonMap("abc", UrlConfig.urlConfigBuilder().withMaxDiff(0).build()))
                .build();

        Report report = new Report(new Summary(true, 0.05, 0.05, 0), config,
                List.of(urlReport), Map.of(BrowserStep.before, "SomeBrowser"));

        boolean result = JLineupRunner.isDetectedDifferenceGreaterThanMaxDifference(report, config);

        // Should NOT fail because the context is flaky-accepted
        assertThat(result, is(false));
    }

    @Test
    void shouldStillFailWhenNonFlakyContextExceedsMaxDiff() {
        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page",
                DeviceConfig.deviceConfig(800, 600), BrowserStep.before,
                UrlConfig.urlConfigBuilder().build(), Collections.emptyList(), "abc");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page",
                DeviceConfig.deviceConfig(1024, 768), BrowserStep.before,
                UrlConfig.urlConfigBuilder().build(), Collections.emptyList(), "abc");

        // One context is flaky-accepted, the other is not
        ContextReport flakyContext = new ContextReport(ctx1.contextHash(), ctx1,
                new Summary(true, 0.05, 0.05, 0), Collections.emptyList(), true);
        ContextReport failingContext = new ContextReport(ctx2.contextHash(), ctx2,
                new Summary(true, 0.1, 0.1, 0), Collections.emptyList(), false);

        UrlReport urlReport = new UrlReport("abc", "abc",
                new Summary(true, 0.15, 0.1, 0), List.of(flakyContext, failingContext));

        JobConfig config = JobConfig.jobConfigBuilder()
                .withUrls(Collections.singletonMap("abc", UrlConfig.urlConfigBuilder().withMaxDiff(0).build()))
                .build();

        Report report = new Report(new Summary(true, 0.15, 0.1, 0), config,
                List.of(urlReport), Map.of(BrowserStep.before, "SomeBrowser"));

        boolean result = JLineupRunner.isDetectedDifferenceGreaterThanMaxDifference(report, config);

        // Should still fail because ctx2 is not flaky-accepted and has difference > maxDiff
        assertThat(result, is(true));
    }

}