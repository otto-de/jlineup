package de.otto.jlineup.report;

import java.util.List;

public class UrlReport {

    public final List<ScreenshotComparisonResult> comparisonResults;
    public final Summary summary;

    public UrlReport(List<ScreenshotComparisonResult> comparisonResults, Summary summary) {
        this.comparisonResults = comparisonResults;
        this.summary = summary;
    }
}
