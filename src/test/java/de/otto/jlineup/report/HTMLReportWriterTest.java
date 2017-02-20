package de.otto.jlineup.report;

import de.otto.jlineup.file.FileService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;

public class HTMLReportWriterTest {

    private HTMLReportWriter testee;

    @Mock
    private FileService fileServiceMock;

    private final List<ScreenshotComparisonResult> screenshotComparisonResults = singletonList(new ScreenshotComparisonResult("url", 1337, 1338, 0d, "before", "after", "differenceSum"));
    private final Map<String, List<ScreenshotComparisonResult>> screenshotComparisonResultList =
            singletonMap("test", screenshotComparisonResults);
    private Summary summary = new Summary(true, 1d, 0.5d);
    private Report report = new Report(summary, screenshotComparisonResultList);

    @Before
    public void setup() {
        initMocks(this);
        testee = new HTMLReportWriter(fileServiceMock);
    }

    @Test
    public void shouldRenderHTMLReport() throws Exception {

        String n = System.getProperty("line.separator");

        String expectedHtmlStart = "<!DOCTYPE html>" + n +
                "<html>" + n +
                "<head>" + n +
                "    <title>JLineup Comparison Report</title>" + n +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" + n +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/>" + n +
                "    <META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\"/>" + n +
                "    <META HTTP-EQUIV=\"Expires\" CONTENT=\"-1\"/>" + n +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>" + n +
                "" + n +
                "    <style>" + n +
                "" + n +
                "        body {" + n +
                "            background-color: white;" + n +
                "            font-family: Arial, Helvetica, sans-serif;" + n +
                "            margin-left: 10px;" + n +
                "            margin-top: 10px;" + n +
                "        }" + n +
                "" + n +
                "        .footer {" + n +
                "            margin-top: 25px;" + n +
                "            font-size: 10px;" + n +
                "            color: grey;" + n +
                "        }" + n +
                "" + n +
                "        table tr:nth-child(even) {" + n +
                "            background-color: #eee;" + n +
                "        }" + n +
                "" + n +
                "        table tr:nth-child(odd) {" + n +
                "            background-color: #fff;" + n +
                "        }" + n +
                "" + n +
                "        table th {" + n +
                "            color: white;" + n +
                "            background-color: black;" + n +
                "        }" + n +
                "" + n +
                "        td {" + n +
                "            padding: 0 0 0 0;" + n +
                "            border: 1px solid;" + n +
                "            border-collapse: collapse;" + n +
                "            vertical-align: top;" + n +
                "        }" + n +
                "" + n +
                "        table {" + n +
                "            padding: 0 0 15px 20px;" + n +
                "            display: none;" + n +
                "        }" + n +
                "" + n +
                "        p {" + n +
                "            padding: 5px;" + n +
                "        }" + n +
                "" + n +
                "        label {" + n +
                "            cursor: pointer;" + n +
                "            font-weight: bold;" + n +
                "            font-size: 18px;" + n +
                "        }" + n +
                "" + n +
                "        .context input[type=checkbox] {" + n +
                "            display: none;" + n +
                "        }" + n +
                "" + n +
                "        .context input[type=checkbox]:checked ~ table {" + n +
                "            display: block;" + n +
                "        }" + n +
                "" + n +
                "        .context input[type=checkbox]:checked ~ label .arrow-down {" + n +
                "            display: block;" + n +
                "        }" + n +
                "" + n +
                "        .context input[type=checkbox]:checked ~ label .arrow-right {" + n +
                "            display: none;" + n +
                "        }" + n +
                "" + n +
                "        .arrow-right {" + n +
                "            display: block;" + n +
                "            float: left;" + n +
                "            width: 0;" + n +
                "            height: 0;" + n +
                "            margin: 5px 10px 0px 6px;" + n +
                "            border-top: 6px solid transparent;" + n +
                "            border-bottom: 6px solid transparent;" + n +
                "            border-left: 6px solid gray;" + n +
                "        }" + n +
                "" + n +
                "        .arrow-down {" + n +
                "            display: none;" + n +
                "            float: left;" + n +
                "            width: 0;" + n +
                "            height: 0;" + n +
                "            margin: 9px 7px 0px 3px;" + n +
                "            border-top: 6px solid gray;" + n +
                "            border-right: 6px solid transparent;" + n +
                "            border-left: 6px solid transparent;" + n +
                "        }" + n +
                "" + n +
                "        .success {" + n +
                "            color: green;" + n +
                "        }" + n +
                "" + n +
                "        .failure {" + n +
                "            color: red;" + n +
                "        }" + n +
                "" + n +
                "    </style>" + n +
                "</head>" + n +
                "" + n +
                "<body>" + n +
                "" + n +
                "<div class=\"report\">" + n +
                "    <h2>JLineup Comparison Report</h2>" + n +
                "    <div class=\"context\">" + n +
                "        <input type=\"checkbox\" id=\"url|||1337\" />" + n +
                "        <label for=\"url|||1337\" class=\"success\">" + n +
                "            <div class=\"arrow-right\"></div>" + n +
                "            <div class=\"arrow-down\"></div>" + n +
                "            url (Browser window width: 1337)" + n +
                "        </label>" + n +
                "        <table>" + n +
                "            <tr>" + n +
                "                <th>Info</th>" + n +
                "                <th>Before</th>" + n +
                "                <th>After</th>" + n +
                "                <th>Difference</th>" + n +
                "            </tr>" + n +
                "            <tr>" + n +
                "                <td>" + n +
                "                    <p><a href=\"url\" target=\"_blank\" title=\"url\">url</a><br/>" + n +
                "                        Width: 1337<br/>" + n +
                "                        Scroll pos: 1338<br/>" + n +
                "                        Difference: 0.00%" + n +
                "                    </p>" + n +
                "                </td>" + n +
                "                <td>" + n +
                "                    <a href=\"before\" target=\"_blank\">" + n +
                "                        <img src=\"before\" style=\"max-width: 350px;\" />" + n +
                "                    </a>" + n +
                "                    " + n +
                "                </td>" + n +
                "                <td>" + n +
                "                    <a href=\"after\" target=\"_blank\">" + n +
                "                        <img src=\"after\" style=\"max-width: 350px;\" />" + n +
                "                    </a>" + n +
                "                    " + n +
                "                </td>" + n +
                "                <td>" + n +
                "                    <a href=\"differenceSum\" target=\"_blank\">" + n +
                "                        <img src=\"differenceSum\" style=\"max-width: 350px;\" />" + n +
                "                    </a>" + n +
                "                    " + n +
                "                </td>" + n +
                "            </tr>" + n +
                "        </table>" + n +
                "    </div>" + n +
                "</div>" + n +
                "" + n +
                "<p class=\"footer\">Generated with Jlineup";

        String expectedHtmlEnd = "</p>" + n +
                "" + n +
                "</body>" + n +
                "</html>";

        final String report = testee.renderReport("report", screenshotComparisonResults);

        assertThat(report, startsWith(expectedHtmlStart));
        assertThat(report, endsWith(expectedHtmlEnd));
    }

    @Test
    public void shouldWriteReport() throws FileNotFoundException {

        testee.writeReport(report);

        Mockito.verify(fileServiceMock).writeHtmlReport(anyString());
    }
}