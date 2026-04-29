package de.otto.jlineup.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record UrlReport(String urlKey, String url, Summary summary, List<ContextReport> contextReports) {

    /**
     * Groups context reports by their URL sub-path (the part after the base URL).
     * Used in the report template to avoid repeating the sub-path for each device/browser variant.
     * Returns a LinkedHashMap to preserve insertion (sort) order.
     */
    @UsedInTemplate
    public Map<String, List<ContextReport>> getContextReportsBySubPath() {
        return contextReports.stream()
                .collect(Collectors.groupingBy(
                        cr -> {
                            String fullUrl = cr.getUrl();
                            if (fullUrl != null && url != null && fullUrl.startsWith(url)) {
                                return fullUrl.substring(url.length());
                            }
                            return fullUrl != null ? fullUrl : "";
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
