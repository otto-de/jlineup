package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.config.JobConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record Report(Summary summary, JobConfig config, List<UrlReport> urlReports,
                     Map<BrowserStep, Set<String>> browsers) {

    @JsonCreator
    public Report(Summary summary, JobConfig config, List<UrlReport> urlReports, Map<BrowserStep, Set<String>> browsers) {
        this.summary = summary;
        this.config = config.sanitize();
        this.urlReports = urlReports;
        this.browsers = browsers;
    }

    @UsedInTemplate
    public String getBrowser(String step) {
        Set<String> browserSet = browsers.get(BrowserStep.valueOf(step));
        if (browserSet == null || browserSet.isEmpty()) {
            return null;
        }
        return browserSet.stream().sorted().collect(Collectors.joining(", "));
    }

    /**
     * Returns true if differences were detected but every failing context was flaky-accepted,
     * meaning the run passes despite having non-zero differences.
     */
    @UsedInTemplate
    public boolean isOnlyFlakyDifferences() {
        if (!summary.error()) {
            return false;
        }
        return urlReports.stream()
                .flatMap(ur -> ur.contextReports().stream())
                .filter(cr -> cr.summary().differenceSum() > 0)
                .allMatch(ContextReport::isFlakyAccepted);
    }

}
