package de.otto.jlineup.report;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Report {

    public final Summary summary;
    public final Map<String, UrlReport> screenshotComparisonsForUrl;

    public Report(Summary summary, Map<String, UrlReport> screenshotComparisons) {
        this.summary = summary;
        this.screenshotComparisonsForUrl = screenshotComparisons;
    }

    public List<ScreenshotComparisonResult> getFlatResultList() {
        return screenshotComparisonsForUrl.values().stream().flatMap(urlReport -> urlReport.comparisonResults.stream()).collect(Collectors.toList());
    }
}
