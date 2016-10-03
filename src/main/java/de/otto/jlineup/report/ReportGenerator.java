package de.otto.jlineup.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;

import java.io.FileNotFoundException;
import java.util.List;

public class ReportGenerator {

    private final FileService fileService;
    private final Parameters parameters;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ReportGenerator(FileService fileService, Parameters parameters) {
        this.fileService = fileService;
        this.parameters = parameters;
    }

    public void writeComparisonReportAsJson(List<ScreenshotComparisonResult> screenshotComparisonResults) throws FileNotFoundException {
        final String reportJson = gson.toJson(screenshotComparisonResults);
        fileService.writeJsonReport(reportJson);
    }

    public void writeComparisonReportAsHtml(List<ScreenshotComparisonResult> screenshotComparisonResults) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("<head><title>JLineup Report</title>");

        sb.append("<style>");
        sb.append("body {" +
                "    background-color : white;\n" +
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
                "");
        sb.append("</style>");

        sb.append("</head>\n");
        sb.append("<body>\n");

        String lastContext = null;
        for (ScreenshotComparisonResult screenshotComparisonResult : screenshotComparisonResults) {
            String context = screenshotComparisonResult.url + "|||" + screenshotComparisonResult.width;
            boolean firstOfContext = false;
            if (!context.equals(lastContext)) {
                if (lastContext != null) {
                    sb.append("</table>");
                }
                firstOfContext = true;
                lastContext = context;
                sb.append("<h3>");
                sb.append(screenshotComparisonResult.url);
                sb.append(" (Browser window width: ");
                sb.append(screenshotComparisonResult.width);
                sb.append(")</h3><table>");
            }
            if (firstOfContext) {
                sb.append("<tr><th>Info</th><th>Before</th><th>After</th><th>Difference</th></tr>\n");
            }
            sb.append("<tr><td>");
            writeLinkInfo(sb, screenshotComparisonResult);
            sb.append("</td><td>");
            if (screenshotComparisonResult.screenshotBeforeFileName != null) {
                writeImageLink(sb, screenshotComparisonResult.screenshotBeforeFileName, parameters);
            } else {
                sb.append("<p>No before image</p>");
            }
            sb.append("</td>");
            sb.append("<td>");
            if (screenshotComparisonResult.screenshotAfterFileName != null) {
                writeImageLink(sb, screenshotComparisonResult.screenshotAfterFileName, parameters);
            } else {
                sb.append("<p>No after image</p>");
            }
            sb.append("</td>");
            sb.append("<td>");
            if (screenshotComparisonResult.differenceImageFileName != null) {
                writeImageLink(sb, screenshotComparisonResult.differenceImageFileName, parameters);
            } else {
                sb.append("<p>No difference image</p>");
            }
            sb.append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("</body>\n");

        sb.append("</html>\n");
        fileService.writeHtmlReport(sb.toString());
    }

    private void writeLinkInfo(StringBuilder sb, ScreenshotComparisonResult screenshotComparisonResult) {
        sb.append("<p><a href=\"");
        sb.append(screenshotComparisonResult.url);
        sb.append("\" target=\"_blank\" title=\"");
        sb.append(screenshotComparisonResult.url);
        sb.append("\">");
        sb.append(shortenUrl(screenshotComparisonResult.url));
        sb.append("</a>");
        sb.append("<br />Width: ");
        sb.append(screenshotComparisonResult.width);
        sb.append("<br />Scroll pos: ");
        sb.append(screenshotComparisonResult.verticalScrollPosition);
        sb.append("<br />Difference: ");
        sb.append(formatDifference(screenshotComparisonResult.difference));
        sb.append("</p>");
    }

    private String formatDifference(double difference) {
        return String.format("%1$,.2f", difference * 100) + "%";
    }

    private String shortenUrl(final String url) {
        String retval = url;
        if (url.length() > 25) {
            retval = "..." + retval.substring(retval.lastIndexOf("/"), retval.length());
        }
        return retval;
    }

    private void writeImageLink(StringBuilder sb, String differenceImageFileName, Parameters parameters) {
        sb.append("<a href=\"");
        sb.append(differenceImageFileName);
        sb.append("\" target=\"_blank\">");
        sb.append("<img width=\"350\" src=\"");
        sb.append(differenceImageFileName);
        sb.append("\" />");
        sb.append("</a>");
    }

}
