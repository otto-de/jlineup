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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
    private final Report report = new Report(globalSummary, JobConfig.exampleConfig(), singletonList(urlReport), Map.of(before, Set.of("SomeBrowser 1.2.3"), after, Set.of("SomeBrowser 4.5.6")));

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

    @Test
    void shouldRenderDefaultLabelsInReport() throws FileNotFoundException {
        String html = renderReport(JobConfig.exampleConfig(), false);

        assertThat(html, containsString(">Before | After<"));
        assertThat(html, containsString("&#8599;&nbsp;Before"));
        assertThat(html, containsString("&#8599;&nbsp;After"));
        assertThat(html, containsString("No&nbsp;before&nbsp;image"));
        assertThat(html, containsString("No&nbsp;after&nbsp;image"));
        assertThat(html, containsString("before: \"Before\""));
        assertThat(html, containsString("after: \"After\""));
        assertThat(html, containsString("The 'before' step was rendered with"));
    }

    @Test
    void shouldRenderConfiguredLabelsInReport() throws FileNotFoundException {
        JobConfig jobConfig = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withBeforeLabel("Reference (local)").withAfterLabel("Current (dev)").build();

        String html = renderReport(jobConfig, false);

        assertThat(html, containsString(">Reference (local) | Current (dev)<"));
        assertThat(html, containsString("&#8599;&nbsp;Reference (local)"));
        assertThat(html, containsString("&#8599;&nbsp;Current (dev)"));
        assertThat(html, containsString("No&nbsp;Reference (local)&nbsp;image"));
        assertThat(html, containsString("No&nbsp;Current (dev)&nbsp;image"));
        assertThat(html, containsString("Reference (local) (only)"));
        assertThat(html, containsString("Current (dev) (only)"));
        assertThat(html, containsString("<div class=\"zoom-head\">Reference (local)</div>"));
        assertThat(html, containsString("<div class=\"zoom-head\">Current (dev)</div>"));
        assertThat(html, containsString("before: \"Reference (local)\""));
        assertThat(html, containsString("after: \"Current (dev)\""));
        assertThat(html, containsString("The 'before' step (Reference (local)) was rendered with"));
        assertThat(html, containsString("the 'after' step (Current (dev)) was rendered with"));
        assertThat(html, not(containsString(">Before | After<")));
        assertThat(html, not(containsString("&#8599;&nbsp;Before")));
    }

    @Test
    void shouldEscapeConfiguredLabelsInReport() throws FileNotFoundException {
        JobConfig jobConfig = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withBeforeLabel("<b>\"x'</b>").withAfterLabel("</script>").build();

        String html = renderReport(jobConfig, false);

        assertThat(html, not(containsString("<b>\"x'</b>")));
        assertThat(html, not(containsString("after: \"</script>\"")));
        assertThat(html, containsString("&lt;b&gt;"));
    }

    @Test
    void shouldRenderConfiguredBeforeLabelInReportAfterBeforeStep() throws FileNotFoundException {
        JobConfig jobConfig = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withBeforeLabel("Reference (local)").build();

        String html = renderReport(jobConfig, true);

        assertThat(html, containsString("<div class=\"compare-cell\">Reference (local)</div>"));
        assertThat(html, containsString("&#8599;&nbsp;Reference (local)"));
        assertThat(html, containsString("The 'before' step (Reference (local)) was rendered with"));
    }

    @Test
    void shouldRenderDefaultBeforeLabelInReportAfterBeforeStep() throws FileNotFoundException {
        String html = renderReport(JobConfig.exampleConfig(), true);

        assertThat(html, containsString("<div class=\"compare-cell\">Before</div>"));
        assertThat(html, containsString("&#8599;&nbsp;Before"));
        assertThat(html, containsString("The 'before' step was rendered with"));
    }

    private String renderReport(JobConfig jobConfig, boolean afterBeforeStep) throws FileNotFoundException {
        DeviceConfig deviceConfig = DeviceConfig.deviceConfig(1337, 200);
        List<ScreenshotComparisonResult> results = List.of(
                screenshotComparisonResult,
                ScreenshotComparisonResult.noBeforeImageComparisonResult("1887", "someurl/somepath", deviceConfig, 1400, "after2"),
                ScreenshotComparisonResult.noAfterImageComparisonResult("1887", "someurl/somepath", deviceConfig, 1600, "before3"));
        ContextReport labelContextReport = new ContextReport("1887",
                ScreenshotContext.of("someurl", "somepath", deviceConfig, before, UrlConfig.urlConfigBuilder().build()),
                localSummary, results);
        UrlReport labelUrlReport = new UrlReport("someurl/somepath", "someurl/somepath", localSummary, singletonList(labelContextReport));
        Report labelReport = new Report(globalSummary, jobConfig, singletonList(labelUrlReport), Map.of(before, Set.of("SomeBrowser 1.2.3"), after, Set.of("SomeBrowser 4.5.6")));

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        if (afterBeforeStep) {
            testee.writeReportAfterBeforeStep(labelReport);
        } else {
            testee.writeReport(labelReport);
        }
        verify(fileServiceMock).writeHtmlReport(htmlCaptor.capture(), anyString());
        assertThat(htmlCaptor.getValue(), startsWith("<!DOCTYPE"));
        return htmlCaptor.getValue();
    }
}
