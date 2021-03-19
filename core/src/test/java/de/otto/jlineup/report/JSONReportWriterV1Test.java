package de.otto.jlineup.report;

import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static java.lang.System.lineSeparator;
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
                new ScreenshotComparisonResult(1887, "url", DeviceConfig.deviceConfig(1337, 1887), 1979, 0d, "before", "after", "differenceImageFileName", 0);
        final Summary localSummary = new Summary(false, 0d, 0d, 0);
        final Summary globalSummary = new Summary(false, 0d, 0d, 0);
        Report report = new Report(globalSummary, singletonMap("test", new UrlReport(singletonList(screenshotComparisonResult), localSummary)), JobConfig.exampleConfig());

        String expectedString = "[ {" + lineSeparator() +
                "  \"contextHash\" : 1887," + lineSeparator() +
                "  \"url\" : \"url\"," + lineSeparator() +
                "  \"deviceConfig\" : {" + lineSeparator() +
                "    \"width\" : 1337," + lineSeparator() +
                "    \"height\" : 1887," + lineSeparator() +
                "    \"pixelRatio\" : 1.0," + lineSeparator() +
                "    \"deviceName\" : \"DESKTOP\"," + lineSeparator() +
                "    \"touch\" : false" + lineSeparator() +
                "  }," + lineSeparator() +
                "  \"verticalScrollPosition\" : 1979," + lineSeparator() +
                "  \"difference\" : 0.0," + lineSeparator() +
                "  \"screenshotBeforeFileName\" : \"before\"," + lineSeparator() +
                "  \"screenshotAfterFileName\" : \"after\"," + lineSeparator() +
                "  \"differenceImageFileName\" : \"differenceImageFileName\"," + lineSeparator() +
                "  \"acceptedDifferentPixels\" : 0" + lineSeparator() +
                "} ]";

        testee.writeComparisonReportAsJson(report);

        Mockito.verify(fileServiceMock).writeJsonReport(expectedString);
    }
}