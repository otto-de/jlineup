package de.otto.jlineup;

import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.report.Report;
import de.otto.jlineup.report.Summary;
import de.otto.jlineup.report.UrlReport;
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

}