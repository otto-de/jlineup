package de.otto.jlineup.config;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.otto.jlineup.config.JobConfig.*;
import static java.lang.invoke.MethodHandles.lookup;

public class ConfigMerger {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static JobConfig mergeJobConfigWithMergeConfig(JobConfig originalGlobalConfig, JobConfig mergeGlobalConfig) {

        JobConfig.Builder mergedJobConfigBuilder = JobConfig.jobConfigBuilder();

        //Start with global stuff
        mergeGlobalScopeConfigItems(originalGlobalConfig, mergeGlobalConfig, mergedJobConfigBuilder);

        //Now the urls

        Map<String, UrlConfig> urls = originalGlobalConfig.urls;
        for (Map.Entry<String, UrlConfig> originalUrlConfigEntry : urls.entrySet()) {
            UrlConfig originalUrlConfig = originalUrlConfigEntry.getValue();
            Map<String, UrlConfig> mergeUrls = mergeGlobalConfig.urls;
            for (Map.Entry<String, UrlConfig> mergeUrlConfigEntry : mergeUrls.entrySet()) {
                if (originalUrlConfigEntry.getKey().matches(mergeUrlConfigEntry.getKey())){
                    LOG.info("Merging merge config for '{}' into '{}'", mergeUrlConfigEntry.getKey(), originalUrlConfigEntry.getKey());
                    UrlConfig mergeUrlConfig = mergeUrlConfigEntry.getValue();
                    UrlConfig.Builder urlConfigBuilder = UrlConfig.urlConfigBuilder();
                    urlConfigBuilder.withAlternatingCookies(merge(originalUrlConfig.alternatingCookies, mergeUrlConfig.alternatingCookies));
                    urlConfigBuilder.withCleanupPaths(merge(originalUrlConfig.cleanupPaths, mergeUrlConfig.cleanupPaths));
                    urlConfigBuilder.withCookies(merge(originalUrlConfig.cookies, mergeUrlConfig.cookies));
                    urlConfigBuilder.withDevices(merge(originalUrlConfig.devices, mergeUrlConfig.devices));
                    urlConfigBuilder.withEnvMapping(merge(originalUrlConfig.envMapping, mergeUrlConfig.envMapping));
                    urlConfigBuilder.withFailIfSelectorsNotFound(originalUrlConfig.failIfSelectorsNotFound || mergeUrlConfig.failIfSelectorsNotFound);
                    urlConfigBuilder.withHideImages(originalUrlConfig.hideImages || mergeUrlConfig.hideImages);
                    urlConfigBuilder.withHttpCheck(originalUrlConfig.httpCheck != DEFAULT_HTTP_CHECK_CONFIG ? originalUrlConfig.httpCheck : mergeUrlConfig.httpCheck);
                    urlConfigBuilder.withIgnoreAntiAliasing(originalUrlConfig.ignoreAntiAliasing || mergeUrlConfig.ignoreAntiAliasing);
                    urlConfigBuilder.withJavaScript(originalUrlConfig.javaScript == null && mergeUrlConfig.javaScript == null ? null : "" + originalUrlConfig.javaScript + "\n" + mergeUrlConfig.javaScript);
                    urlConfigBuilder.withLocalStorage(merge(originalUrlConfig.localStorage, mergeUrlConfig.localStorage));
                    urlConfigBuilder.withMaxColorDistance(originalUrlConfig.maxColorDistance != DEFAULT_MAX_COLOR_DISTANCE ? originalUrlConfig.maxColorDistance : mergeUrlConfig.maxColorDistance);
                    urlConfigBuilder.withMaxDiff(originalUrlConfig.maxDiff != DEFAULT_MAX_DIFF ? originalUrlConfig.maxDiff : mergeUrlConfig.maxDiff);
                    urlConfigBuilder.withMaxScrollHeight(originalUrlConfig.maxScrollHeight != DEFAULT_MAX_SCROLL_HEIGHT ? originalUrlConfig.maxScrollHeight : mergeUrlConfig.maxScrollHeight);
                    urlConfigBuilder.withPaths(merge(originalUrlConfig.paths, mergeUrlConfig.paths));
                    urlConfigBuilder.withRemoveSelectors(merge(originalUrlConfig.removeSelectors, mergeUrlConfig.removeSelectors));
                    urlConfigBuilder.withSessionStorage(merge(originalUrlConfig.sessionStorage, mergeUrlConfig.sessionStorage));
                    urlConfigBuilder.withSetupPaths(merge(originalUrlConfig.setupPaths, mergeUrlConfig.setupPaths));
                    urlConfigBuilder.withStrictColorComparison(originalUrlConfig.strictColorComparison || mergeUrlConfig.strictColorComparison);
                    urlConfigBuilder.withWaitAfterPageLoad(originalUrlConfig.waitAfterPageLoad != DEFAULT_WAIT_AFTER_PAGE_LOAD ? originalUrlConfig.waitAfterPageLoad : mergeUrlConfig.waitAfterPageLoad);
                    urlConfigBuilder.withWaitAfterScroll(originalUrlConfig.waitAfterScroll != DEFAULT_WAIT_AFTER_SCROLL ? originalUrlConfig.waitAfterScroll : mergeUrlConfig.waitAfterScroll);
                    urlConfigBuilder.withWaitForFontsTime(originalUrlConfig.waitForFontsTime != DEFAULT_WAIT_FOR_FONTS_TIME ? originalUrlConfig.waitForFontsTime : mergeUrlConfig.waitForFontsTime);
                    urlConfigBuilder.withWaitForNoAnimationAfterScroll(originalUrlConfig.waitForNoAnimationAfterScroll != DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL ? originalUrlConfig.waitForNoAnimationAfterScroll : mergeUrlConfig.waitForNoAnimationAfterScroll);
                    urlConfigBuilder.withWaitForSelectors(merge(originalUrlConfig.waitForSelectors, mergeUrlConfig.waitForSelectors));
                    urlConfigBuilder.withWaitForSelectorsTimeout(originalUrlConfig.waitForSelectorsTimeout != DEFAULT_WAIT_FOR_SELECTORS_TIMEOUT ? originalUrlConfig.waitForSelectorsTimeout : mergeUrlConfig.waitForSelectorsTimeout);
                    urlConfigBuilder.withWarmupBrowserCacheTime(originalUrlConfig.warmupBrowserCacheTime != DEFAULT_WARMUP_BROWSER_CACHE_TIME ? originalUrlConfig.warmupBrowserCacheTime : mergeUrlConfig.warmupBrowserCacheTime);
                    urlConfigBuilder.withWindowWidths(merge(originalUrlConfig.windowWidths, mergeUrlConfig.windowWidths));

                    originalUrlConfig = urlConfigBuilder.build();
                }
            }
            mergedJobConfigBuilder.addUrlConfig(originalUrlConfigEntry.getKey(), originalUrlConfig);
        }
        return mergedJobConfigBuilder.build();
    }

    private static <T> Set<T> merge(Set<T> one, Set<T> two) {
        if (one == null && two == null) return null;
        return Stream.concat(one != null ? one.stream() : Stream.empty(), two != null ? two.stream() : Stream.empty()).collect(Collectors.toSet());
    }

    private static <T> List<T> merge(List<T> one, List<T> two) {
        if (one == null && two == null) return null;
        return Stream.concat(one != null ? one.stream() : Stream.empty(), two != null ? two.stream() : Stream.empty()).distinct().collect(Collectors.toList());
    }

    private static <T,U> Map<T,U> merge(Map<T,U> one, Map<T,U> two) {
        if (one == null && two == null) return null;
        ImmutableMap.Builder<T, U> mapBuilder = ImmutableMap.<T, U>builder();
        if (two != null) mapBuilder.putAll(two);
        if (one != null) mapBuilder.putAll(one);
        return mapBuilder.build();
    }

    private static void mergeGlobalScopeConfigItems(JobConfig originalConfig, JobConfig mergeConfig, Builder mergedJobConfigBuilder) {
        mergedJobConfigBuilder.withBrowser(originalConfig.browser != DEFAULT_BROWSER ? originalConfig.browser : mergeConfig.browser);
        mergedJobConfigBuilder.withCheckForErrorsInLog(originalConfig.checkForErrorsInLog || mergeConfig.checkForErrorsInLog);
        mergedJobConfigBuilder.withDebug(originalConfig.debug || mergeConfig.debug);
        mergedJobConfigBuilder.withGlobalTimeout(originalConfig.globalTimeout != DEFAULT_GLOBAL_TIMEOUT ? originalConfig.globalTimeout : mergeConfig.globalTimeout);
        mergedJobConfigBuilder.withGlobalWaitAfterPageLoad(originalConfig.globalWaitAfterPageLoad != DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD ? originalConfig.globalWaitAfterPageLoad : mergeConfig.globalWaitAfterPageLoad);
        mergedJobConfigBuilder.withHttpCheck(!originalConfig.httpCheck.equals(DEFAULT_HTTP_CHECK_CONFIG) ? originalConfig.httpCheck : mergeConfig.httpCheck);
        mergedJobConfigBuilder.withLogToFile(originalConfig.logToFile || mergeConfig.logToFile);
        mergedJobConfigBuilder.withName(originalConfig.name != null ? originalConfig.name : mergeConfig.name);
        mergedJobConfigBuilder.withPageLoadTimeout(originalConfig.pageLoadTimeout != DEFAULT_PAGELOAD_TIMEOUT ? originalConfig.pageLoadTimeout : mergeConfig.pageLoadTimeout);
        mergedJobConfigBuilder.withReportFormat(originalConfig.reportFormat != DEFAULT_REPORT_FORMAT ? originalConfig.reportFormat : mergeConfig.reportFormat);
        mergedJobConfigBuilder.withScreenshotRetries(originalConfig.screenshotRetries != DEFAULT_SCREENSHOT_RETRIES ? originalConfig.screenshotRetries : mergeConfig.screenshotRetries);
        mergedJobConfigBuilder.withThreads(originalConfig.threads != DEFAULT_THREADS ? originalConfig.threads : mergeConfig.threads);
        mergedJobConfigBuilder.withWindowHeight(originalConfig.windowHeight != null ? originalConfig.windowHeight : mergeConfig.windowHeight);
    }

}
