package de.otto.jlineup.report;

import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static java.util.Collections.singletonList;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReportGeneratorTest {

    private ReportGenerator testee;

    @Mock
    private FileService fileServiceMock;

    @Mock
    private Parameters parameters;

    @Before
    public void setup() {
        initMocks(this);
        testee = new ReportGenerator(fileServiceMock, parameters);
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

    @Test
    public void shouldWriteHTMLReport() throws Exception {
        ScreenshotComparisonResult screenshotComparisonResult =
                new ScreenshotComparisonResult("url", 1337, 1338, 0d, "before", "after", "difference");

        String expectedHtml = "<html>\n" +
                "<head><title>JLineup Report</title><style>body {    background-color : white;\n" +
                "    font-family : Arial, Helvetica, sans-serif;\n" +
                "    margin-left : 10px;\n" +
                "    margin-top : 10px;\n" +
                "}\n" +
                "table tr:nth-child(even) {\n" +
                "    background-color: #eee;\n" +
                "}\n" +
                "table tr:nth-child(odd) {\n" +
                "    background-color: #fff;\n" +
                "}\n" +
                "table th {\n" +
                "    color: white;\n" +
                "    background-color: black;\n" +
                "}\n" +
                "td {\n" +
                "    padding: 0 0 0 0;\n" +
                "    border: 1px solid;\n" +
                "    border-collapse: collapse;\n" +
                "    vertical-align: top;\n" +
                "}\n" +
                "table {\n" +
                "    padding: 0 0 15px 0;\n" +
                "}\n" +
                "p {\n" +
                "    padding: 5px;\n" +
                "}\n" +
                "</style></head>\n" +
                "<body>\n" +
                "<h3>url (Browser window width: 1337)</h3><table><tr><th>Info</th><th>Before</th><th>After</th><th>Difference</th></tr>\n" +
                "<tr><td><p><a href=\"url\" target=\"_blank\" title=\"url\">url</a><br />Width: 1337<br />Scroll pos: 1338<br />Difference: 0.00%</p></td><td><a href=\"before\" target=\"_blank\"><img width=\"350\" src=\"before\" /></a></td><td><a href=\"after\" target=\"_blank\"><img width=\"350\" src=\"after\" /></a></td><td><a href=\"difference\" target=\"_blank\"><img width=\"350\" src=\"difference\" /></a></td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n" +
                "";

        testee.writeComparisonReportAsHtml(singletonList(screenshotComparisonResult));

        Mockito.verify(fileServiceMock).writeHtmlReport(expectedHtml);
    }
}