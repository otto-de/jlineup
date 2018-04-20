package de.otto.jlineup.report;

import de.otto.jlineup.config.JobConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class ReportGenerator {

    public Report generateReport(Map<String, List<ScreenshotComparisonResult>> screenshotComparisonResultLists, JobConfig config) {
        List<ScreenshotComparisonResult> resultList = screenshotComparisonResultLists.values().stream().flatMap(List::stream).collect(Collectors.toList());
        final Summary summary = getSummary(resultList);

        HashMap<String, UrlReport> urlReports = new HashMap<>();
        for (Map.Entry<String, List<ScreenshotComparisonResult>> result : screenshotComparisonResultLists.entrySet()) {
            Summary localSummary = getSummary(result.getValue());
            UrlReport urlReport = new UrlReport(result.getValue(), localSummary);
            urlReports.put(result.getKey(), urlReport);
        }
        return new Report(summary, urlReports, config);
    }

    private Summary getSummary(List<ScreenshotComparisonResult> resultList) {
        final double differenceSum = resultList.stream().mapToDouble(scr -> scr.difference).sum();
        final OptionalDouble differenceMax = resultList.stream().mapToDouble(scr -> scr.difference).max();
        return new Summary(differenceSum > 0, differenceSum, differenceMax.orElseGet(() -> 0));
    }
}
