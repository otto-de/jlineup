package de.otto.jlineup.report;

import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;

public class ReportGenerator {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final FileService fileService;

    public ReportGenerator(FileService fileService) {
        this.fileService = fileService;
    }

    public Report generateReport(Map<String, List<ScreenshotComparisonResult>> screenshotComparisonResultLists, JobConfig config) {
        List<ScreenshotComparisonResult> resultList = screenshotComparisonResultLists.values().stream().flatMap(List::stream).collect(Collectors.toList());
        final Summary summary = getSummary(resultList);

        ArrayList<UrlReport> urlReports = new ArrayList<>();
        for (Map.Entry<String, List<ScreenshotComparisonResult>> resultForUrl : screenshotComparisonResultLists.entrySet()) {
            Summary urlSummary = getSummary(resultForUrl.getValue());

            Map<String, List<ScreenshotComparisonResult>> resultsPerContextHash = resultForUrl.getValue().stream().collect(Collectors.groupingBy(res -> res.contextHash(), Collectors.mapping(res -> res, Collectors.toList())));
            ArrayList<ContextReport> contextReports = new ArrayList<>();
            for (Map.Entry<String, List<ScreenshotComparisonResult>> resultPerHash : resultsPerContextHash.entrySet()) {
                Summary contextSummary = getSummary(resultPerHash.getValue());
                ContextReport contextReport = new ContextReport(resultPerHash.getKey(), fileService.getRecordedContext(resultPerHash.getKey()), contextSummary, resultPerHash.getValue());
                contextReports.add(contextReport);
            }

            // Apply flaky-tolerance logic
            int flakyTolerance = getEffectiveFlakyTolerance(config, resultForUrl.getKey());
            if (flakyTolerance > 0) {
                contextReports = applyFlakyTolerance(contextReports, flakyTolerance);
            }

            contextReports.sort(Comparator.comparing(ContextReport::getUrl)
                    .thenComparing(ContextReport::getWidth)
                    .thenComparing(ContextReport::getShownCookiesString, Comparator.nullsLast(Comparator.naturalOrder())));

            UrlReport urlReport = new UrlReport(resultForUrl.getKey(), config.urls.get(resultForUrl.getKey()).url, urlSummary, contextReports);
            urlReports.add(urlReport);
        }

        return new Report(summary, config, urlReports, fileService.getBrowsers());
    }

    /**
     * Returns the effective flaky-tolerance value for the given URL key.
     * Per-URL value takes precedence if non-default, otherwise the global value is used.
     */
    private int getEffectiveFlakyTolerance(JobConfig config, String urlKey) {
        UrlConfig urlConfig = config.urls.get(urlKey);
        if (urlConfig != null && urlConfig.flakyTolerance != JobConfig.DEFAULT_FLAKY_TOLERANCE) {
            return urlConfig.flakyTolerance;
        }
        return config.flakyTolerance;
    }

    /**
     * Applies flaky-tolerance logic to context reports.
     *
     * For each failing context, finds sibling contexts (same URL sub-path and screenshot-context-giving
     * cookies, but different device config). If at least {@code flakyTolerance} siblings pass with zero
     * difference, the failing context is marked as flaky-accepted.
     */
    static ArrayList<ContextReport> applyFlakyTolerance(ArrayList<ContextReport> contextReports, int flakyTolerance) {
        // Group context reports by their flaky sibling key (path + context-giving cookies, excluding device)
        Map<String, List<ContextReport>> siblingGroups = contextReports.stream()
                .filter(cr -> cr.screenshotContext() != null)
                .collect(Collectors.groupingBy(cr -> cr.screenshotContext().flakySiblingKey()));

        ArrayList<ContextReport> result = new ArrayList<>();
        for (ContextReport contextReport : contextReports) {
            if (contextReport.screenshotContext() == null || contextReport.summary().differenceSum() == 0) {
                // No context or already passing — keep as-is
                result.add(contextReport);
                continue;
            }

            String siblingKey = contextReport.screenshotContext().flakySiblingKey();
            List<ContextReport> siblings = siblingGroups.getOrDefault(siblingKey, Collections.emptyList());

            // Count passing siblings (different context hash = different device, zero difference)
            long passingSiblingCount = siblings.stream()
                    .filter(sibling -> !sibling.contextHash().equals(contextReport.contextHash()))
                    .filter(sibling -> sibling.summary().differenceSum() == 0)
                    .count();

            if (passingSiblingCount >= flakyTolerance) {
                LOG.info("Flaky-tolerance triggered for context {} ({}): {} passing siblings >= threshold {}",
                        contextReport.contextHash(),
                        contextReport.getUrl() + " " + contextReport.screenshotContext().deviceConfig,
                        passingSiblingCount, flakyTolerance);
                result.add(new ContextReport(contextReport.contextHash(), contextReport.screenshotContext(),
                        contextReport.summary(), contextReport.results(), true));
            } else {
                result.add(contextReport);
            }
        }
        return result;
    }

    private Summary getSummary(List<ScreenshotComparisonResult> resultList) {
        final double differenceSum = resultList.stream().mapToDouble(scr -> scr.difference()).sum();
        final OptionalDouble differenceMax = resultList.stream().mapToDouble(scr -> scr.difference()).max();
        final int acceptedDifferentPixelsSum = resultList.stream().mapToInt(scr -> scr.acceptedDifferentPixels()).sum();
        return new Summary(differenceSum > 0, differenceSum, differenceMax.orElseGet(() -> 0), acceptedDifferentPixelsSum);
    }

}
