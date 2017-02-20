package de.otto.jlineup.report;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Report {

    public final Summary summary;
    public final Map<String, List<ScreenshotComparisonResult>> screenshotComparisonsForUrl;

    public Report(Summary summary, Map<String, List<ScreenshotComparisonResult>> screenshotComparisons) {
        this.summary = summary;
        this.screenshotComparisonsForUrl = screenshotComparisons;
    }

    public List<ScreenshotComparisonResult> getFlatResultList() {
        return screenshotComparisonsForUrl.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
}
