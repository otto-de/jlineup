package de.otto.jlineup.report;

import java.util.List;

public record UrlReportV2(String urlKey, String url, Summary summary, List<ContextReport> contextReports) {
}
