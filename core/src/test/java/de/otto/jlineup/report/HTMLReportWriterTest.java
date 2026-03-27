package de.otto.jlineup.report;

import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.util.Map;

import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HTMLReportWriterTest {

    private HTMLReportWriter testee;

    @Mock
    private FileService fileServiceMock;

    private final Summary globalSummary = new Summary(false, 1d, 0.5d, 0);
    private final Summary localSummary = new Summary(false, 2d, 0.3d, 0);
    private final ScreenshotComparisonResult screenshotComparisonResult =
            new ScreenshotComparisonResult("1887", "someurl/somepath", DeviceConfig.deviceConfig(1337, 200), 1338, 0d, 0d, "before", "after", "differenceSum", 0);
    private final ContextReport contextReport = new ContextReport(
            "1887",
            ScreenshotContext.of("someurl", "somepath", DeviceConfig.deviceConfig(1337, 200), before, UrlConfig.urlConfigBuilder().build()),
            localSummary,
            singletonList(screenshotComparisonResult)
    );
    private final UrlReport urlReport = new UrlReport("someurl/somepath", "someurl/somepath", localSummary, singletonList(contextReport));
    private final Report report = new Report(globalSummary, JobConfig.exampleConfig(), singletonList(urlReport), Map.of(before, "SomeBrowser 1.2.3", after, "SomeBrowser 4.5.6"));

    @BeforeEach
    void setup() {
        testee = new HTMLReportWriter(fileServiceMock);
    }

    @Test
    void shouldWriteReport() throws FileNotFoundException {
        testee.writeReport(report);
        verify(fileServiceMock).writeHtmlReport(anyString(), anyString());
    }

    @Test
    void shouldRenderReportWithoutTemplateVariables() throws FileNotFoundException {
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        testee.writeReport(report);
        verify(fileServiceMock).writeHtmlReport(htmlCaptor.capture(), anyString());
        String renderedHtml = htmlCaptor.getValue();
        assertThat(renderedHtml, not(containsString("${")));
        assertThat(renderedHtml, not(containsString("th:if")));
        assertThat(renderedHtml, not(containsString("th:text")));
    }

    @Test
    void shouldWriteReportAfterBeforeStep() throws FileNotFoundException {
        testee.writeReportAfterBeforeStep(report);
        verify(fileServiceMock).writeHtmlReport(anyString(), anyString());
    }
}
