package de.otto.jlineup.report;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.MockitoAnnotations.initMocks;

public class JSONReportWriterV1Test {

    private JSONReportWriter_V1 testee;

    @Mock
    private FileService fileServiceMock;

    @Before
    public void setup() {
        initMocks(this);
        testee = new JSONReportWriter_V1(fileServiceMock);
    }

    @Test
    public void shouldWriteComparisonReportAsJson() throws Exception {

        ScreenshotComparisonResult screenshotComparisonResult =
                new ScreenshotComparisonResult("url", 1337, 1338, 0d, "before", "after", "differenceImageFileName", 0);
        final Summary localSummary = new Summary(false, 0d, 0d);
        final Summary globalSummary = new Summary(false, 0d, 0d);
        Report report = new Report(globalSummary, singletonMap("test", new UrlReport(singletonList(screenshotComparisonResult), localSummary)), JobConfig.exampleConfig());

        String expectedString = "[ {\n" +
                "  \"url\" : \"url\",\n" +
                "  \"width\" : 1337,\n" +
                "  \"verticalScrollPosition\" : 1338,\n" +
                "  \"difference\" : 0.0,\n" +
                "  \"screenshotBeforeFileName\" : \"before\",\n" +
                "  \"screenshotAfterFileName\" : \"after\",\n" +
                "  \"differenceImageFileName\" : \"differenceImageFileName\",\n" +
                "  \"maxSingleColorDifference\" : 0\n" +
                "} ]";

        testee.writeComparisonReportAsJson(report);

        Mockito.verify(fileServiceMock).writeJsonReport(expectedString);
    }
}