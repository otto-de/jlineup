package de.otto.jlineup.report;

import de.otto.jlineup.config.JobConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Report {

    public final Summary summary;
    public final Map<String, UrlReport> screenshotComparisonsForUrl;
    public final JobConfig config;

    public Report(Summary summary, Map<String, UrlReport> screenshotComparisons, JobConfig config) {
        this.summary = summary;
        this.screenshotComparisonsForUrl = screenshotComparisons;
        this.config = config.sanitize();
    }

    public List<ScreenshotComparisonResult> getFlatResultList() {
        return screenshotComparisonsForUrl.values().stream().flatMap(urlReport -> urlReport.comparisonResults.stream()).collect(Collectors.toList());
    }
}
