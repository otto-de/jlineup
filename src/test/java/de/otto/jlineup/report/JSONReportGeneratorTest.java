package de.otto.jlineup.report;

import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static java.util.Collections.singletonList;
import static org.mockito.MockitoAnnotations.initMocks;

public class JSONReportGeneratorTest {

    private JSONReportGenerator testee;

    @Mock
    private FileService fileServiceMock;

    @Before
    public void setup() {
        initMocks(this);
        testee = new JSONReportGenerator(fileServiceMock);
    }

    @Test
    public void shouldWriteComparisonReportAsJson() throws Exception {

        ScreenshotComparisonResult screenshotComparisonResult =
                new ScreenshotComparisonResult("url", 1337, 1338, 0d, "before", "after", "difference");

        String expectedString = "[\n" +
                "  {\n" +
                "    \"url\": \"url\",\n" +
                "    \"width\": 1337,\n" +
                "    \"verticalScrollPosition\": 1338,\n" +
                "    \"difference\": 0.0,\n" +
                "    \"screenshotBeforeFileName\": \"before\",\n" +
                "    \"screenshotAfterFileName\": \"after\",\n" +
                "    \"differenceImageFileName\": \"difference\"\n" +
                "  }\n" +
                "]";

        testee.writeComparisonReportAsJson(singletonList(screenshotComparisonResult));

        Mockito.verify(fileServiceMock).writeJsonReport(expectedString);
    }
}