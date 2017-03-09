package de.otto.jlineup.report;

import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    public Report generateReport(Map<String, List<ScreenshotComparisonResult>> screenshotComparisonResultLists) {
        List<ScreenshotComparisonResult> resultList = screenshotComparisonResultLists.values().stream().flatMap(List::stream).collect(Collectors.toList());
        final double differenceSum = resultList.stream().mapToDouble(scr -> scr.difference).sum();
        final OptionalDouble differenceMax = resultList.stream().mapToDouble(scr -> scr.difference).max();
        final Summary summary = new Summary(differenceSum > 0, differenceSum, differenceMax.orElseGet(() -> 0));

        return new Report(summary, screenshotComparisonResultLists);
    }
}
