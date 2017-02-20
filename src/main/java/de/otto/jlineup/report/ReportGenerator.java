package de.otto.jlineup.report;

import java.util.*;

public class ReportGenerator {

    public Report generateReport(List<ScreenshotComparisonResult> screenshotComparisonResultList) {
        final double differenceSum = screenshotComparisonResultList.stream().mapToDouble(scr -> scr.difference).sum();
        final OptionalDouble differenceMax = screenshotComparisonResultList.stream().mapToDouble(scr -> scr.difference).max();
        final Summary summary = new Summary(differenceSum > 0, differenceSum, differenceMax.orElseGet(() -> 0));

        Map<String, List<ScreenshotComparisonResult>> resultsPerUrl = new HashMap<>();

        for (ScreenshotComparisonResult screenshotComparisonResult : screenshotComparisonResultList) {
            List<ScreenshotComparisonResult> resultListForUrl = resultsPerUrl.getOrDefault(screenshotComparisonResult.url, new ArrayList<>());
            resultListForUrl.add(screenshotComparisonResult);
            resultsPerUrl.putIfAbsent(screenshotComparisonResult.url, resultListForUrl);
        }

        return new Report(summary, resultsPerUrl);
    }
}
