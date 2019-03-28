package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableList;

import java.util.*;

import static de.otto.jlineup.config.JobConfig.*;

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
@JsonDeserialize(builder = UrlConfig.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrlConfig {

    public final List<String> paths;
    public final float maxDiff;
    public final List<Cookie> cookies;
    public final Map<String, String> envMapping;
    public final Map<String, String> localStorage;
    public final Map<String, String> sessionStorage;
    public final List<Integer> windowWidths;
    public final List<DeviceConfig> devices;
    public final int maxScrollHeight;
    public final float waitAfterPageLoad;
    public final float waitAfterScroll;
    public final float waitForNoAnimationAfterScroll;
    public final float warmupBrowserCacheTime;
    public final float waitForFontsTime;
    @JsonProperty("javascript")
    public final String javaScript;
    public final boolean hideImages;
    public final HttpCheckConfig httpCheck;
    public final int maxColorDiffPerPixel;

    public UrlConfig(List<String> paths, float maxDiff, List<Cookie> cookies, Map<String, String> envMapping, Map<String, String> localStorage, Map<String, String> sessionStorage, List<Integer> windowWidths, int maxScrollHeight, float waitAfterPageLoad, float waitAfterScroll, float waitForNoAnimationAfterScroll, float warmupBrowserCacheTime, String javaScript, float waitForFontsTime, HttpCheckConfig httpCheck, int maxColorDiffPerPixel, boolean hideImages) {
        this.paths = paths != null ? paths : ImmutableList.of(DEFAULT_PATH);
        this.windowWidths = windowWidths;
        this.devices = null;
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
        this.httpCheck = httpCheck;
        this.maxColorDiffPerPixel = maxColorDiffPerPixel;
        this.hideImages = hideImages;
    }

    private UrlConfig(Builder builder) {
        paths = builder.paths;
        maxDiff = builder.maxDiff;
        cookies = builder.cookies;
        envMapping = builder.envMapping;
        localStorage = builder.localStorage;
        sessionStorage = builder.sessionStorage;
        windowWidths = builder.windowWidths;
        devices = builder.devices;
        maxScrollHeight = builder.maxScrollHeight;
        waitAfterPageLoad = builder.waitAfterPageLoad;
        waitAfterScroll = builder.waitAfterScroll;
        waitForNoAnimationAfterScroll = builder.waitForNoAnimationAfterScroll;
        warmupBrowserCacheTime = builder.warmupBrowserCacheTime;
        waitForFontsTime = builder.waitForFontsTime;
        javaScript = builder.javaScript;
        httpCheck = builder.httpCheck;
        maxColorDiffPerPixel = builder.maxColorDiffPerPixel;
        hideImages = builder.hideImages;
    }

    public static Builder urlConfigBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(UrlConfig copy) {
        Builder builder = new Builder();
        builder.paths = copy.paths;
        builder.maxDiff = copy.maxDiff;
        builder.cookies = copy.cookies;
        builder.envMapping = copy.envMapping;
        builder.localStorage = copy.localStorage;
        builder.sessionStorage = copy.sessionStorage;
        builder.windowWidths = copy.windowWidths;
        builder.devices = copy.devices;
        builder.maxScrollHeight = copy.maxScrollHeight;
        builder.waitAfterPageLoad = copy.waitAfterPageLoad;
        builder.waitAfterScroll = copy.waitAfterScroll;
        builder.waitForNoAnimationAfterScroll = copy.waitForNoAnimationAfterScroll;
        builder.warmupBrowserCacheTime = copy.warmupBrowserCacheTime;
        builder.waitForFontsTime = copy.waitForFontsTime;
        builder.javaScript = copy.javaScript;
        builder.httpCheck = copy.httpCheck;
        builder.maxColorDiffPerPixel = copy.maxColorDiffPerPixel;
        builder.hideImages = copy.hideImages;
        return builder;
    }

    @JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
    public static final class Builder {
        private List<String> paths = ImmutableList.of(DEFAULT_PATH);
        private float maxDiff = DEFAULT_MAX_DIFF;
        private List<Cookie> cookies;
        private Map<String, String> envMapping;
        private Map<String, String> localStorage;
        private Map<String, String> sessionStorage;
        private List<Integer> windowWidths;
        private List<DeviceConfig> devices;
        private int maxScrollHeight = DEFAULT_MAX_SCROLL_HEIGHT;
        private float waitAfterPageLoad = DEFAULT_WAIT_AFTER_PAGE_LOAD;
        private float waitAfterScroll = DEFAULT_WAIT_AFTER_SCROLL;
        private float waitForNoAnimationAfterScroll = DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL;
        private float warmupBrowserCacheTime = DEFAULT_WARMUP_BROWSER_CACHE_TIME;
        private float waitForFontsTime = DEFAULT_WAIT_FOR_FONTS_TIME;
        private String javaScript;
        private HttpCheckConfig httpCheck = new HttpCheckConfig();
        private boolean hideImages;
        private int maxColorDiffPerPixel = DEFAULT_MAX_COLOR_DIFF_PER_PIXEL;

        private Builder() {
        }

        public Builder withPaths(List<String> val) {
            paths = val;
            return this;
        }

        public Builder withMaxDiff(float val) {
            maxDiff = val;
            return this;
        }

        public Builder withCookies(List<Cookie> val) {
            cookies = val;
            return this;
        }

        public Builder withCookie(Cookie val) {
            cookies = Collections.singletonList(val);
            return this;
        }

        public Builder withEnvMapping(Map<String, String> val) {
            envMapping = val;
            return this;
        }

        public Builder withHttpCheck(HttpCheckConfig val) {
            httpCheck = val;
            return this;
        }

        public Builder withLocalStorage(Map<String, String> val) {
            localStorage = val;
            return this;
        }

        public Builder withSessionStorage(Map<String, String> val) {
            sessionStorage = val;
            return this;
        }

        @JsonAlias({"resolutions","widths"})
        public Builder withWindowWidths(List<Integer> val) {
            windowWidths = val;
            return this;
        }

        public Builder withDevices(List<DeviceConfig> val) {
            devices = val;
            return this;
        }

        public Builder addDeviceConfig(DeviceConfig deviceConfig) {
            if (devices == null) {
                devices = new ArrayList<>();
            }
            devices.add(deviceConfig);
            return this;
        }

        public Builder withMaxScrollHeight(int val) {
            maxScrollHeight = val;
            return this;
        }

        public Builder withWaitAfterPageLoad(float val) {
            waitAfterPageLoad = val;
            return this;
        }

        public Builder withWaitAfterScroll(float val) {
            waitAfterScroll = val;
            return this;
        }

        public Builder withWaitForNoAnimationAfterScroll(float val) {
            waitForNoAnimationAfterScroll = val;
            return this;
        }

        public Builder withWarmupBrowserCacheTime(float val) {
            warmupBrowserCacheTime = val;
            return this;
        }

        public Builder withWaitForFontsTime(float val) {
            waitForFontsTime = val;
            return this;
        }

        @JsonProperty("javascript")
        public Builder withJavaScript(String val) {
            javaScript = val;
            return this;
        }

        public Builder withMaxColorDiffPerPixel(int val) {
            maxColorDiffPerPixel = val;
            return this;
        }

        public Builder withHideImages(boolean val) {
            hideImages = val;
            return this;
        }

        public UrlConfig build() {

            //If both are not set, use default window width
            if (windowWidths == null && devices == null) {
                windowWidths = ImmutableList.of(DEFAULT_WINDOW_WIDTH);
            }

            return new UrlConfig(this);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlConfig urlConfig = (UrlConfig) o;
        return Float.compare(urlConfig.maxDiff, maxDiff) == 0 &&
                maxScrollHeight == urlConfig.maxScrollHeight &&
                Float.compare(urlConfig.waitAfterPageLoad, waitAfterPageLoad) == 0 &&
                Float.compare(urlConfig.waitAfterScroll, waitAfterScroll) == 0 &&
                Float.compare(urlConfig.waitForNoAnimationAfterScroll, waitForNoAnimationAfterScroll) == 0 &&
                Float.compare(urlConfig.warmupBrowserCacheTime, warmupBrowserCacheTime) == 0 &&
                Float.compare(urlConfig.waitForFontsTime, waitForFontsTime) == 0 &&
                hideImages == urlConfig.hideImages &&
                maxColorDiffPerPixel == urlConfig.maxColorDiffPerPixel &&
                Objects.equals(paths, urlConfig.paths) &&
                Objects.equals(cookies, urlConfig.cookies) &&
                Objects.equals(envMapping, urlConfig.envMapping) &&
                Objects.equals(localStorage, urlConfig.localStorage) &&
                Objects.equals(sessionStorage, urlConfig.sessionStorage) &&
                Objects.equals(windowWidths, urlConfig.windowWidths) &&
                Objects.equals(devices, urlConfig.devices) &&
                Objects.equals(javaScript, urlConfig.javaScript) &&
                Objects.equals(httpCheck, urlConfig.httpCheck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, maxDiff, cookies, envMapping, localStorage, sessionStorage, windowWidths, devices, maxScrollHeight, waitAfterPageLoad, waitAfterScroll, waitForNoAnimationAfterScroll, warmupBrowserCacheTime, waitForFontsTime, javaScript, hideImages, httpCheck, maxColorDiffPerPixel);
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
                ", devices=" + devices +
                ", maxScrollHeight=" + maxScrollHeight +
                ", waitAfterPageLoad=" + waitAfterPageLoad +
                ", waitAfterScroll=" + waitAfterScroll +
                ", waitForNoAnimationAfterScroll=" + waitForNoAnimationAfterScroll +
                ", warmupBrowserCacheTime=" + warmupBrowserCacheTime +
                ", waitForFontsTime=" + waitForFontsTime +
                ", javaScript='" + javaScript + '\'' +
                ", hideImages=" + hideImages +
                ", httpCheck=" + httpCheck +
                ", maxColorDiffPerPixel=" + maxColorDiffPerPixel +
                '}';
    }
}
