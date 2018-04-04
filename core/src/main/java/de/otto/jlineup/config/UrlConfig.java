package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.ImmutableList.of;
import static de.otto.jlineup.config.Config.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UrlConfig {

    public final List<String> paths;

    @SerializedName("max-diff")
    @JsonProperty("max-diff")
    public final float maxDiff;

    public final List<Cookie> cookies;

    @SerializedName("env-mapping")
    @JsonProperty("env-mapping")
    public final Map<String, String> envMapping;

    @SerializedName("local-storage")
    @JsonProperty("local-storage")
    public final Map<String, String> localStorage;

    @SerializedName("session-storage")
    @JsonProperty("session-storage")
    public final Map<String, String> sessionStorage;

    @SerializedName(value = "window-widths", alternate = {"resolutions","widths"})
    @JsonProperty(value = "window-widths")
    @JsonAlias({"resolutions","widths"})
    public final List<Integer> windowWidths;

    @SerializedName("max-scroll-height")
    @JsonProperty("max-scroll-height")
    public final int maxScrollHeight;

    @SerializedName("wait-after-page-load")
    @JsonProperty("wait-after-page-load")
    public final int waitAfterPageLoad;

    @SerializedName("wait-after-scroll")
    @JsonProperty("wait-after-scroll")
    public final int waitAfterScroll;

    @SerializedName("wait-for-no-animation-after-scroll")
    @JsonProperty("wait-for-no-animation-after-scroll")
    public final float waitForNoAnimationAfterScroll;

    @SerializedName("warmup-browser-cache-time")
    @JsonProperty("warmup-browser-cache-time")
    public final int warmupBrowserCacheTime;

    @SerializedName("wait-for-fonts-time")
    @JsonProperty("wait-for-fonts-time")
    public final int waitForFontsTime;

    @SerializedName("javascript")
    @JsonProperty("javascript")
    public final String javaScript;

    //Default constructor for GSON
    public UrlConfig() {
        this.paths = of(DEFAULT_PATH);
        this.windowWidths = of(DEFAULT_WINDOW_WIDTH);
        this.maxDiff = DEFAULT_MAX_DIFF;
        this.cookies = null;
        this.localStorage = null;
        this.sessionStorage = null;
        this.maxScrollHeight = DEFAULT_MAX_SCROLL_HEIGHT;
        this.waitAfterPageLoad = DEFAULT_WAIT_AFTER_PAGE_LOAD;
        this.waitAfterScroll = DEFAULT_WAIT_AFTER_SCROLL;
        this.waitForNoAnimationAfterScroll = DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL;
        this.envMapping = null;
        this.warmupBrowserCacheTime = DEFAULT_WARMUP_BROWSER_CACHE_TIME;
        this.javaScript = null;
        this.waitForFontsTime = DEFAULT_WAIT_FOR_FONTS_TIME;
    }

    public UrlConfig(List<String> paths, float maxDiff, List<Cookie> cookies, Map<String, String> envMapping, Map<String, String> localStorage, Map<String, String> sessionStorage, List<Integer> windowWidths, int maxScrollHeight, int waitAfterPageLoad, int waitAfterScroll, float waitForNoAnimationAfterScroll, int warmupBrowserCacheTime, String javaScript, int waitForFontsTime) {
        this.paths = paths != null ? paths : of(DEFAULT_PATH);
        this.windowWidths = windowWidths != null ? windowWidths : of(DEFAULT_WINDOW_WIDTH);
        this.maxDiff = maxDiff;
        this.cookies = cookies;
        this.envMapping = envMapping;
        this.localStorage = localStorage;
        this.sessionStorage = sessionStorage;
        this.maxScrollHeight = maxScrollHeight;
        this.waitAfterPageLoad = waitAfterPageLoad;
        this.waitAfterScroll = waitAfterScroll;
        this.waitForNoAnimationAfterScroll = waitForNoAnimationAfterScroll;
        this.warmupBrowserCacheTime = warmupBrowserCacheTime;
        this.javaScript = javaScript;
        this.waitForFontsTime = waitForFontsTime;
    }

    @Override
    public String toString() {
        return "UrlConfig{" +
                "paths=" + paths +
                ", maxDiff=" + maxDiff +
                ", cookies=" + cookies +
                ", envMapping=" + envMapping +
                ", localStorage=" + localStorage +
                ", sessionStorage=" + sessionStorage +
                ", windowWidths=" + windowWidths +
                ", maxScrollHeight=" + maxScrollHeight +
                ", waitAfterPageLoad=" + waitAfterPageLoad +
                ", waitAfterScroll=" + waitAfterScroll +
                ", waitForNoAnimationAfterScroll=" + waitForNoAnimationAfterScroll +
                ", warmupBrowserCacheTime=" + warmupBrowserCacheTime +
                ", waitForFontsTime=" + waitForFontsTime +
                ", javaScript='" + javaScript + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlConfig urlConfig = (UrlConfig) o;
        return Float.compare(urlConfig.maxDiff, maxDiff) == 0 &&
                maxScrollHeight == urlConfig.maxScrollHeight &&
                waitAfterPageLoad == urlConfig.waitAfterPageLoad &&
                waitAfterScroll == urlConfig.waitAfterScroll &&
                Float.compare(urlConfig.waitForNoAnimationAfterScroll, waitForNoAnimationAfterScroll) == 0 &&
                warmupBrowserCacheTime == urlConfig.warmupBrowserCacheTime &&
                waitForFontsTime == urlConfig.waitForFontsTime &&
                Objects.equals(paths, urlConfig.paths) &&
                Objects.equals(cookies, urlConfig.cookies) &&
                Objects.equals(envMapping, urlConfig.envMapping) &&
                Objects.equals(localStorage, urlConfig.localStorage) &&
                Objects.equals(sessionStorage, urlConfig.sessionStorage) &&
                Objects.equals(windowWidths, urlConfig.windowWidths) &&
                Objects.equals(javaScript, urlConfig.javaScript);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, maxDiff, cookies, envMapping, localStorage, sessionStorage, windowWidths, maxScrollHeight, waitAfterPageLoad, waitAfterScroll, waitForNoAnimationAfterScroll, warmupBrowserCacheTime, waitForFontsTime, javaScript);
    }
}
