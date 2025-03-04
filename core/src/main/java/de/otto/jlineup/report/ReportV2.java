package de.otto.jlineup.report;

import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.config.JobConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportV2 {

    public final Summary summary;
    public final JobConfig config;
    public final List<UrlReportV2> urlReports;
    public final Map<BrowserStep, String> browsers;

    public ReportV2(Summary summary, JobConfig jobConfig, List<UrlReportV2> urlReports, Map<BrowserStep, String> browsers) {
        this.summary = summary;
        this.config = jobConfig.sanitize();
        this.urlReports = urlReports;
        this.browsers = browsers;
    }

    @UsedInTemplate
    public String getBrowser(String step) {
        return browsers.get(BrowserStep.valueOf(step));
    }

    @Override
    public String toString() {
        return "ReportV2{" +
                "summary=" + summary +
                ", config=" + config +
                ", urlReports=" + urlReports +
                ", browsers=" + browsers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportV2 reportV2 = (ReportV2) o;
        return Objects.equals(summary, reportV2.summary) && Objects.equals(config, reportV2.config) && Objects.equals(urlReports, reportV2.urlReports) && Objects.equals(browsers, reportV2.browsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, config, urlReports, browsers);
    }
}
