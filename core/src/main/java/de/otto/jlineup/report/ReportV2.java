package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.config.JobConfig;

import java.util.List;
import java.util.Map;

public record ReportV2(Summary summary, JobConfig config, List<UrlReportV2> urlReports,
                       Map<BrowserStep, String> browsers) {

    @JsonCreator
    public ReportV2(Summary summary, JobConfig config, List<UrlReportV2> urlReports, Map<BrowserStep, String> browsers) {
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
