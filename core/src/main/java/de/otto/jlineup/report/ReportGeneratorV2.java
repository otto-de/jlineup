package de.otto.jlineup.report;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;

import java.util.*;
import java.util.stream.Collectors;

public class ReportGeneratorV2 {

    private final FileService fileService;

    public ReportGeneratorV2(FileService fileService) {
        this.fileService = fileService;
    }

    public ReportV2 generateReport(Map<String, List<ScreenshotComparisonResult>> screenshotComparisonResultLists, JobConfig config) {
        List<ScreenshotComparisonResult> resultList = screenshotComparisonResultLists.values().stream().flatMap(List::stream).collect(Collectors.toList());
        final Summary summary = getSummary(resultList);

        ArrayList<UrlReportV2> urlReports = new ArrayList<>();
        for (Map.Entry<String, List<ScreenshotComparisonResult>> resultForUrl : screenshotComparisonResultLists.entrySet()) {
            Summary urlSummary = getSummary(resultForUrl.getValue());

            Map<Integer, List<ScreenshotComparisonResult>> resultsPerContextHash = resultForUrl.getValue().stream().collect(Collectors.groupingBy(res -> res.contextHash, Collectors.mapping(res -> res, Collectors.toList())));
            ArrayList<ContextReport> contextReports = new ArrayList<>();
            for (Map.Entry<Integer, List<ScreenshotComparisonResult>> resultPerHash : resultsPerContextHash.entrySet()) {
                Summary contextSummary = getSummary(resultPerHash.getValue());
                ContextReport contextReport = new ContextReport(resultPerHash.getKey(), fileService.getRecordedContext(resultPerHash.getKey()), contextSummary, resultPerHash.getValue());
                contextReports.add(contextReport);
            }

            contextReports.sort(Comparator.comparing(ContextReport::getUrl).thenComparing(ContextReport::getWidth).thenComparing(ContextReport::getShownCookiesString));

            UrlReportV2 urlReport = new UrlReportV2(resultForUrl.getKey(), config.urls.get(resultForUrl.getKey()).url, urlSummary, contextReports);
            urlReports.add(urlReport);
        }

        return new ReportV2(summary, config, urlReports, fileService.getBrowsers());
    }

    private Summary getSummary(List<ScreenshotComparisonResult> resultList) {
        final double differenceSum = resultList.stream().mapToDouble(scr -> scr.difference).sum();
        final OptionalDouble differenceMax = resultList.stream().mapToDouble(scr -> scr.difference).max();
        final int acceptedDifferentPixelsSum = resultList.stream().mapToInt(scr -> scr.acceptedDifferentPixels).sum();
        return new Summary(differenceSum > 0, differenceSum, differenceMax.orElseGet(() -> 0), acceptedDifferentPixelsSum);
    }

}
