package de.otto.jlineup.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.otto.jlineup.config.Config.*;

public class UrlConfig {

    public final List<String> paths;

    @SerializedName("max-diff")
    public final float maxDiff;

    public final List<Cookie> cookies;

    @SerializedName("env-mapping")
    public final Map<String, String> envMapping;

    @SerializedName("local-storage")
    public final Map<String, String> localStorage;

    @SerializedName(value = "window-widths", alternate = {"resolutions","widths"})
    public final List<Integer> windowWidths;

    @SerializedName("max-scroll-height")
    public final int maxScrollHeight;

    @SerializedName("wait-after-page-load")
    public final int waitAfterPageLoad;

    @SerializedName("wait-for-no-animation-after-scroll")
    public final float waitForNoAnimationAfterScroll;

    @SerializedName("warmup-browser-cache-time")
    private final Integer warmupBrowserCacheTime;

    //Default constructor for GSON
    public UrlConfig() {
        this.paths = DEFAULT_PATHS;
        this.windowWidths = DEFAULT_WINDOW_WIDTHS;
        this.maxDiff = DEFAULT_MAX_DIFF;
        this.cookies = null;
        this.localStorage = null;
        this.maxScrollHeight = DEFAULT_MAX_SCROLL_HEIGHT;
        this.waitAfterPageLoad = DEFAULT_WAIT_AFTER_PAGE_LOAD;
        this.waitForNoAnimationAfterScroll = DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL;
        this.envMapping = null;
        this.warmupBrowserCacheTime = null;
    }

    public UrlConfig(List<String> paths, float maxDiff, List<Cookie> cookies, Map<String, String> envMapping, Map<String, String> localStorage, List<Integer> windowWidths, int maxScrollHeight, int waitAfterPageLoad, float waitForNoAnimationAfterScroll, Integer warmupBrowserCacheTime) {
        this.paths = paths != null ? paths : DEFAULT_PATHS;
        this.windowWidths = windowWidths != null ? windowWidths : DEFAULT_WINDOW_WIDTHS;
        this.maxDiff = maxDiff;
        this.cookies = cookies;
        this.envMapping = envMapping;
        this.localStorage = localStorage;
        this.maxScrollHeight = maxScrollHeight;
        this.waitAfterPageLoad = waitAfterPageLoad;
        this.waitForNoAnimationAfterScroll = waitForNoAnimationAfterScroll;
        this.warmupBrowserCacheTime = warmupBrowserCacheTime;
    }

    @Override
    public String toString() {
        return "UrlConfig{" +
                "paths=" + paths +
                ", maxDiff=" + maxDiff +
                ", cookies=" + cookies +
                ", envMapping=" + envMapping +
                ", localStorage=" + localStorage +
                ", windowWidths=" + windowWidths +
                ", maxScrollHeight=" + maxScrollHeight +
                ", waitAfterPageLoad=" + waitAfterPageLoad +
                ", waitForNoAnimationAfterScroll=" + waitForNoAnimationAfterScroll +
                ", warmupBrowserCacheTime=" + warmupBrowserCacheTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlConfig urlConfig = (UrlConfig) o;
        return Float.compare(urlConfig.maxDiff, maxDiff) == 0 &&
                Objects.equals(paths, urlConfig.paths) &&
                Objects.equals(cookies, urlConfig.cookies) &&
                Objects.equals(envMapping, urlConfig.envMapping) &&
                Objects.equals(localStorage, urlConfig.localStorage) &&
                Objects.equals(windowWidths, urlConfig.windowWidths) &&
                Objects.equals(maxScrollHeight, urlConfig.maxScrollHeight) &&
                Objects.equals(waitAfterPageLoad, urlConfig.waitAfterPageLoad) &&
                Objects.equals(waitForNoAnimationAfterScroll, urlConfig.waitForNoAnimationAfterScroll) &&
                Objects.equals(warmupBrowserCacheTime, urlConfig.warmupBrowserCacheTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, maxDiff, cookies, envMapping, localStorage, windowWidths, maxScrollHeight, waitAfterPageLoad, waitForNoAnimationAfterScroll, warmupBrowserCacheTime);
    }

    public Optional<Integer> getWarmupBrowserCacheTime() {
        return Optional.ofNullable(warmupBrowserCacheTime);
    }
}
