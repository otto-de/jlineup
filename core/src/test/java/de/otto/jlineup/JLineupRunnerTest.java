package de.otto.jlineup;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.report.Summary;
import de.otto.jlineup.report.UrlReport;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JLineupRunnerTest {

    @Test
    public void shouldNotFailIfMaxDiffIsSameAsDetectedDiff() {

        UrlReport urlReport = new UrlReport(null, new Summary(false, 0, 15.1234567890123456, 0));
        boolean detectedDifferenceGreaterThanMaxDifference = JLineupRunner.isDetectedDifferenceGreaterThanMaxDifference(Collections.singleton(Map.entry("abc", urlReport)), JobConfig.exampleConfigBuilder().withUrls(Collections.singletonMap("abc", UrlConfig.urlConfigBuilder().withMaxDiff(15.1234567890123456).build())).build());

        assertThat(detectedDifferenceGreaterThanMaxDifference, is(false));
    }

}