package de.otto.jlineup.report;

import java.util.List;

public record UrlReport(String urlKey, String url, Summary summary, List<ContextReport> contextReports) {
}
