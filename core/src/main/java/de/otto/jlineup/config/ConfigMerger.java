package de.otto.jlineup.config;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.otto.jlineup.config.JobConfig.*;
import static java.lang.invoke.MethodHandles.lookup;

public class ConfigMerger {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static JobConfig mergeJobConfigWithMergeConfig(JobConfig mainGlobalConfig, JobConfig mergeGlobalConfig) {

        JobConfig.Builder mergedJobConfigBuilder = JobConfig.jobConfigBuilder();

        //Start with global stuff
        mergeGlobalScopeConfigItems(mainGlobalConfig, mergeGlobalConfig, mergedJobConfigBuilder);

        //Now the urls
        Map<String, UrlConfig> urls = mainGlobalConfig.urls;
        for (Map.Entry<String, UrlConfig> mainUrlConfigEntry : urls.entrySet()) {
            UrlConfig mainUrlConfig = mainUrlConfigEntry.getValue();
            Map<String, UrlConfig> mergeUrls = mergeGlobalConfig.urls;
            for (Map.Entry<String, UrlConfig> mergeUrlConfigEntry : mergeUrls.entrySet()) {
                if (mainUrlConfigEntry.getKey().matches(Pattern.quote(mergeUrlConfigEntry.getKey()))){
                    LOG.info("Merging merge config for '{}' into '{}'", mergeUrlConfigEntry.getKey(), mainUrlConfigEntry.getKey());
                    UrlConfig mergeUrlConfig = mergeUrlConfigEntry.getValue();
                    UrlConfig.Builder urlConfigBuilder = UrlConfig.urlConfigBuilder();
                    urlConfigBuilder.withUrl(mainUrlConfig.url);
                    urlConfigBuilder.withAlternatingCookies(merge(mainUrlConfig.alternatingCookies, mergeUrlConfig.alternatingCookies));
                    urlConfigBuilder.withCleanupPaths(merge(mainUrlConfig.cleanupPaths, mergeUrlConfig.cleanupPaths));
                    urlConfigBuilder.withCookies(merge(mainUrlConfig.cookies, mergeUrlConfig.cookies));
                    urlConfigBuilder.withDevices(merge(mainUrlConfig.devices, mergeUrlConfig.devices));
                    urlConfigBuilder.withEnvMapping(merge(mainUrlConfig.envMapping, mergeUrlConfig.envMapping));
                    urlConfigBuilder.withFailIfSelectorsNotFound(mainUrlConfig.failIfSelectorsNotFound || mergeUrlConfig.failIfSelectorsNotFound);
                    urlConfigBuilder.withHideImages(mainUrlConfig.hideImages || mergeUrlConfig.hideImages);
                    urlConfigBuilder.withHttpCheck(mainUrlConfig.httpCheck != DEFAULT_HTTP_CHECK_CONFIG ? mainUrlConfig.httpCheck : mergeUrlConfig.httpCheck);
                    urlConfigBuilder.withIgnoreAntiAliasing(mainUrlConfig.ignoreAntiAliasing || mergeUrlConfig.ignoreAntiAliasing);
                    urlConfigBuilder.withJavaScript(mainUrlConfig.javaScript == null && mergeUrlConfig.javaScript == null ? null : "" + mainUrlConfig.javaScript + ";" + mergeUrlConfig.javaScript);
                    urlConfigBuilder.withLocalStorage(merge(mainUrlConfig.localStorage, mergeUrlConfig.localStorage));
                    urlConfigBuilder.withMaxColorDistance(mainUrlConfig.maxColorDistance != DEFAULT_MAX_COLOR_DISTANCE ? mainUrlConfig.maxColorDistance : mergeUrlConfig.maxColorDistance);
                    urlConfigBuilder.withMaxDiff(mainUrlConfig.maxDiff != DEFAULT_MAX_DIFF ? mainUrlConfig.maxDiff : mergeUrlConfig.maxDiff);
                    urlConfigBuilder.withMaxScrollHeight(mainUrlConfig.maxScrollHeight != DEFAULT_MAX_SCROLL_HEIGHT ? mainUrlConfig.maxScrollHeight : mergeUrlConfig.maxScrollHeight);
                    urlConfigBuilder.withPaths(merge(mainUrlConfig.paths, mergeUrlConfig.paths));
                    urlConfigBuilder.withRemoveSelectors(merge(mainUrlConfig.removeSelectors, mergeUrlConfig.removeSelectors));
                    urlConfigBuilder.withSessionStorage(merge(mainUrlConfig.sessionStorage, mergeUrlConfig.sessionStorage));
                    urlConfigBuilder.withSetupPaths(merge(mainUrlConfig.setupPaths, mergeUrlConfig.setupPaths));
                    urlConfigBuilder.withStrictColorComparison(mainUrlConfig.strictColorComparison || mergeUrlConfig.strictColorComparison);
                    urlConfigBuilder.withWaitAfterPageLoad(mainUrlConfig.waitAfterPageLoad != DEFAULT_WAIT_AFTER_PAGE_LOAD ? mainUrlConfig.waitAfterPageLoad : mergeUrlConfig.waitAfterPageLoad);
                    urlConfigBuilder.withWaitAfterScroll(mainUrlConfig.waitAfterScroll != DEFAULT_WAIT_AFTER_SCROLL ? mainUrlConfig.waitAfterScroll : mergeUrlConfig.waitAfterScroll);
                    urlConfigBuilder.withWaitForFontsTime(mainUrlConfig.waitForFontsTime != DEFAULT_WAIT_FOR_FONTS_TIME ? mainUrlConfig.waitForFontsTime : mergeUrlConfig.waitForFontsTime);
                    urlConfigBuilder.withWaitForNoAnimationAfterScroll(mainUrlConfig.waitForNoAnimationAfterScroll != DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL ? mainUrlConfig.waitForNoAnimationAfterScroll : mergeUrlConfig.waitForNoAnimationAfterScroll);
                    urlConfigBuilder.withWaitForSelectors(merge(mainUrlConfig.waitForSelectors, mergeUrlConfig.waitForSelectors));
                    urlConfigBuilder.withWaitForSelectorsTimeout(mainUrlConfig.waitForSelectorsTimeout != DEFAULT_WAIT_FOR_SELECTORS_TIMEOUT ? mainUrlConfig.waitForSelectorsTimeout : mergeUrlConfig.waitForSelectorsTimeout);
                    urlConfigBuilder.withWarmupBrowserCacheTime(mainUrlConfig.warmupBrowserCacheTime != DEFAULT_WARMUP_BROWSER_CACHE_TIME ? mainUrlConfig.warmupBrowserCacheTime : mergeUrlConfig.warmupBrowserCacheTime);
                    urlConfigBuilder.withWindowWidths(merge(mainUrlConfig.windowWidths, mergeUrlConfig.windowWidths));

                    mainUrlConfig = urlConfigBuilder.build();
                }
            }
            mergedJobConfigBuilder.addUrlConfig(mainUrlConfigEntry.getKey(), mainUrlConfig);
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
        return mapBuilder.buildKeepingLast();
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
