package de.otto.jlineup.report;

import java.util.List;

public class ReportGenerator {

    public Report generateReport(List<ScreenshotComparisonResult> screenshotComparisonResultList) {
        final double differenceSum = screenshotComparisonResultList.stream().mapToDouble(scr -> scr.difference).sum();
        final Summary summary = new Summary(differenceSum > 0, differenceSum);
        return new Report(summary, screenshotComparisonResultList);
    }
}
