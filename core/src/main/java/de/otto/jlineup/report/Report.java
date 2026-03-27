package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.otto.jlineup.config.JobConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record Report(Summary summary, Map<String, UrlReport> screenshotComparisonsForUrl, JobConfig config) {

    @JsonCreator
    public Report(Summary summary, Map<String, UrlReport> screenshotComparisonsForUrl, JobConfig config) {
        this.summary = summary;
        this.screenshotComparisonsForUrl = screenshotComparisonsForUrl;
        this.config = config.sanitize();
    }

    public List<ScreenshotComparisonResult> getFlatResultList() {
        return screenshotComparisonsForUrl.values().stream().flatMap(urlReport -> urlReport.comparisonResults().stream()).collect(Collectors.toList());
    }
}
