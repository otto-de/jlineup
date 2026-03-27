package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.config.JobConfig;

import java.util.List;
import java.util.Map;

public record Report(Summary summary, JobConfig config, List<UrlReport> urlReports,
                     Map<BrowserStep, String> browsers) {

    @JsonCreator
    public Report(Summary summary, JobConfig config, List<UrlReport> urlReports, Map<BrowserStep, String> browsers) {
        this.summary = summary;
        this.config = config.sanitize();
        this.urlReports = urlReports;
        this.browsers = browsers;
    }

    @UsedInTemplate
    public String getBrowser(String step) {
        return browsers.get(BrowserStep.valueOf(step));
    }

}
