package de.otto.jlineup.report;

import java.util.List;
import java.util.Objects;

public class UrlReport {

    public final List<ScreenshotComparisonResult> comparisonResults;
    public final Summary summary;

    public UrlReport(List<ScreenshotComparisonResult> comparisonResults, Summary summary) {
        this.comparisonResults = comparisonResults;
        this.summary = summary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlReport urlReport = (UrlReport) o;
        return Objects.equals(comparisonResults, urlReport.comparisonResults) && Objects.equals(summary, urlReport.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comparisonResults, summary);
    }

    @Override
    public String toString() {
        return "UrlReport{" +
                "comparisonResults=" + comparisonResults +
                ", summary=" + summary +
                '}';
    }
}
