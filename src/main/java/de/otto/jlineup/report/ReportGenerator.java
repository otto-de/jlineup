package de.otto.jlineup.report;

import java.util.List;
import java.util.OptionalDouble;

public class ReportGenerator {

    public Report generateReport(List<ScreenshotComparisonResult> screenshotComparisonResultList) {
        final double differenceSum = screenshotComparisonResultList.stream().mapToDouble(scr -> scr.difference).sum();
        final OptionalDouble differenceMax = screenshotComparisonResultList.stream().mapToDouble(scr -> scr.difference).max();
        final Summary summary = new Summary(differenceSum > 0, differenceSum, differenceMax.orElseGet(() -> 0));
        return new Report(summary, screenshotComparisonResultList);
    }
}
