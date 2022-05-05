package de.otto.jlineup.report;

import java.util.List;
import java.util.Objects;

public class UrlReportV2 {

    public final String url;
    public final Summary summary;
    public final List<ContextReport> contextReports;

    public UrlReportV2(String url, Summary summary, List<ContextReport> contextReports) {
        this.url = url;
        this.summary = summary;
        this.contextReports = contextReports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlReportV2 that = (UrlReportV2) o;
        return Objects.equals(url, that.url) && Objects.equals(summary, that.summary) && Objects.equals(contextReports, that.contextReports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, summary, contextReports);
    }

    @Override
    public String toString() {
        return "UrlReportV2{" +
                "url='" + url + '\'' +
                ", summary=" + summary +
                ", contextReports=" + contextReports +
                '}';
    }
}
