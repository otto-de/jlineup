package de.otto.jlineup.report;

import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.mockito.MockitoAnnotations.initMocks;

public class JSONReportWriterV2Test {

    private JSONReportWriter_V2 testee;

    @Mock
    private FileService fileServiceMock;

    @Before
    public void setup() {
        initMocks(this);
        testee = new JSONReportWriter_V2(fileServiceMock);
    }

    @Test
    public void shouldWriteComparisonReportAsJson() throws Exception {

        ScreenshotComparisonResult screenshotComparisonResult =
                new ScreenshotComparisonResult("url", 1337, 1338, 0d, "before", "after", "differenceSum");
        Report report = new Report(new Summary(false, 0d, 0d), Collections.singletonMap("test", singletonList(screenshotComparisonResult)));

        String expectedString = "{\n" +
                "  \"summary\": {\n" +
                "    \"error\": false,\n" +
                "    \"differenceSum\": 0.0,\n" +
                "    \"differenceMax\": 0.0\n" +
                "  },\n" +
                "  \"screenshotComparisonsForUrl\": {\n" +
                "    \"test\": [\n" +
                "      {\n" +
                "        \"url\": \"url\",\n" +
                "        \"width\": 1337,\n" +
                "        \"verticalScrollPosition\": 1338,\n" +
                "        \"difference\": 0.0,\n" +
                "        \"screenshotBeforeFileName\": \"before\",\n" +
                "        \"screenshotAfterFileName\": \"after\",\n" +
                "        \"differenceImageFileName\": \"differenceSum\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        testee.writeComparisonReportAsJson(report);

        Mockito.verify(fileServiceMock).writeJsonReport(expectedString);
    }
}