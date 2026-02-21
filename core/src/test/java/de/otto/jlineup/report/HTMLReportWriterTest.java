package de.otto.jlineup.report;

import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTMLReportWriterTest {

    private HTMLReportWriter testee;

    @Mock
    private FileService fileServiceMock;

    private final List<ScreenshotComparisonResult> screenshotComparisonResults = singletonList(new ScreenshotComparisonResult(1887, "someurl/somepath", DeviceConfig.deviceConfig(1337, 200), 1338, 0d, 0d, "before", "after", "differenceSum", 0));
    private final Summary summary = new Summary(true, 1d, 0.5d, 0);
    private final Summary localSummary = new Summary(true, 2d, 0.3d, 0);
    private final Map<String, UrlReport> screenshotComparisonResultList =
            singletonMap("test", new UrlReport(screenshotComparisonResults, localSummary));
    private final Report report = new Report(summary, screenshotComparisonResultList, JobConfig.exampleConfig());

    @BeforeEach
    void setup() {
        testee = new HTMLReportWriter(fileServiceMock);
        when(fileServiceMock.getRecordedContext(anyInt())).thenReturn(ScreenshotContext.of("someUrl", "somePath", DeviceConfig.deviceConfig(1337, 200), before, UrlConfig.urlConfigBuilder().build()));
        when(fileServiceMock.getBrowsers()).thenReturn(ImmutableMap.of(before, "SomeBrowser 1.2.3", after, "SomeBrowser 4.5.6"));
    }

    @Test
    void shouldRenderHTMLReport() {

        //String n = System.getProperty("line.separator");
        String n = "\n";

        String expectedHtmlStart = "<!DOCTYPE html>" + n +
                "<html>" + n +
                "<head>" + n +
                "    <title>JLineup Comparison Report</title>" + n +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" + n +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/>" + n +
                "    <meta http-equiv=\"Pragma\" content=\"no-cache\"/>" + n +
                "    <meta http-equiv=\"Expires\" content=\"-1\"/>" + n +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>";

        String expectedHtmlEnd = "</p>" + n +
                "" + n +
                "</body>" + n +
                "</html>";

        final String report = testee.renderReport("report_legacy", JobConfig.exampleConfig(), screenshotComparisonResults);

        assertThat(report, startsWith(expectedHtmlStart));
        assertThat(report, endsWith(expectedHtmlEnd));
        //Check that all variables are replaced
        assertThat(report, not(containsString("${")));
        assertThat(report, not(containsString(" th:")));
    }

    @Test
    void shouldWriteReport() throws FileNotFoundException {

        testee.writeReport(report);
        Mockito.verify(fileServiceMock).writeHtmlReportLegacy(anyString());
    }
}