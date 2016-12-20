package de.otto.jlineup.report;

import java.util.List;

public class Report {

    public final Summary summary;
    public final List<ScreenshotComparisonResult> screenshotComparisons;

    public Report(Summary summary, List<ScreenshotComparisonResult> screenshotComparisons) {
        this.summary = summary;
        this.screenshotComparisons = screenshotComparisons;
    }
}
