package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;

import java.util.*;

import static de.otto.jlineup.config.JobConfig.*;

@JsonDeserialize(builder = UrlConfig.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UrlConfig {

    public final List<String> paths;
    public final List<String> setupPaths;
    public final List<String> cleanupPaths;
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
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final float waitForNoAnimationAfterScroll;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final float warmupBrowserCacheTime;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final float waitForFontsTime;
    @JsonProperty("javascript")
    public final String javaScript;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = HttpCheckFilter.class)
    public final HttpCheckConfig httpCheck;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean hideImages;

    public final Set<String> removeSelectors;
    public final Set<String> waitForSelectors;
    public final float waitForSelectorsTimeout;
    public final boolean failIfSelectorsNotFound;

    public final boolean ignoreAntiAliasing;
    public final boolean strictColorComparison;
    public final float maxColorDistance;

    private UrlConfig(Builder builder) {
        paths = builder.paths;
        setupPaths = builder.setupPaths;
        cleanupPaths = builder.cleanupPaths;
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
        hideImages = builder.hideImages;
        ignoreAntiAliasing = builder.ignoreAntiAliasing;
        strictColorComparison = builder.strictColorComparison;
        maxColorDistance = builder.maxColorDistance;
        removeSelectors = builder.removeSelectors;
        waitForSelectors = builder.waitForSelectors;
        waitForSelectorsTimeout = builder.waitForSelectorsTimeout;
        failIfSelectorsNotFound = builder.failIfSelectorsNotFound;

    }

    /*
     *
     *
     *
     *  BEGIN of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    public List<String> getPaths() {
        return paths;
    }

    public float getMaxDiff() {
        return maxDiff;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public Map<String, String> getEnvMapping() {
        return envMapping;
    }

    public Map<String, String> getLocalStorage() {
        return localStorage;
    }

    public Map<String, String> getSessionStorage() {
        return sessionStorage;
    }

    public List<Integer> getWindowWidths() {
        return windowWidths;
    }

    public List<DeviceConfig> getDevices() {
        return devices;
    }

    public int getMaxScrollHeight() {
        return maxScrollHeight;
    }

    public float getWaitAfterPageLoad() {
        return waitAfterPageLoad;
    }

    public float getWaitAfterScroll() {
        return waitAfterScroll;
    }

    public float getWaitForNoAnimationAfterScroll() {
        return waitForNoAnimationAfterScroll;
    }

    public float getWarmupBrowserCacheTime() {
        return warmupBrowserCacheTime;
    }

    public float getWaitForFontsTime() {
        return waitForFontsTime;
    }

    public String getJavaScript() {
        return javaScript;
    }

    public HttpCheckConfig getHttpCheck() {
        return httpCheck;
    }

    public boolean isHideImages() {
        return hideImages;
    }

    public boolean isIgnoreAntiAliasing() {
        return ignoreAntiAliasing;
    }

    public boolean isStrictColorComparison() {
        return strictColorComparison;
    }

    public float getMaxColorDistance() {
        return maxColorDistance;
    }

    public Set<String> getRemoveSelectors() {
        return removeSelectors;
    }

    public Set<String> getWaitForSelectors() {
        return waitForSelectors;
    }

    public float getWaitForSelectorsTimeout() {
        return waitForSelectorsTimeout;
    }

    public boolean isFailIfSelectorsNotFound() {
        return failIfSelectorsNotFound;
    }

    /*
     *
     *
     *
     *  END of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

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
        builder.hideImages = copy.hideImages;
        builder.ignoreAntiAliasing = copy.ignoreAntiAliasing;
        builder.strictColorComparison = copy.strictColorComparison;
        builder.maxColorDistance = copy.maxColorDistance;
        builder.removeSelectors = copy.removeSelectors;
        builder.waitForSelectors = copy.waitForSelectors;
        builder.waitForSelectorsTimeout = copy.waitForSelectorsTimeout;
        builder.failIfSelectorsNotFound = copy.failIfSelectorsNotFound;

        return builder;
    }

    //@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
    public static final class Builder {
        private List<String> paths = ImmutableList.of(DEFAULT_PATH);
        private List<String> setupPaths = Collections.emptyList();
        private List<String> cleanupPaths = Collections.emptyList();
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
        private boolean ignoreAntiAliasing;
        private boolean strictColorComparison;
        private float maxColorDistance = DEFAULT_MAX_COLOR_DISTANCE;
        private Set<String> removeSelectors;
        public Set<String> waitForSelectors;
        public float waitForSelectorsTimeout = DEFAULT_WAIT_FOR_SELECTORS_TIMEOUT;
        public boolean failIfSelectorsNotFound;

        private Builder() {
        }

        public Builder withPaths(List<String> val) {
            paths = val;
            return this;
        }

        public Builder withSetupPaths(List<String> val) {
            setupPaths = val;
            return this;
        }

        public Builder withCleanupPaths(List<String> val) {
            cleanupPaths = val;
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

        public Builder withHideImages(boolean val) {
            hideImages = val;
            return this;
        }

        public Builder withIgnoreAntiAliasing(boolean val) {
            ignoreAntiAliasing = val;
            return this;
        }

        public Builder withStrictColorComparison(boolean val) {
            strictColorComparison = val;
            return this;
        }

        public Builder withMaxColorDistance(float val) {
            maxColorDistance = val;
            return this;
        }

        public Builder withRemoveSelectors(Set<String> val) {
            removeSelectors = val;
            return this;
        }

        public Builder withWaitForSelectors(Set<String> val) {
            waitForSelectors = val;
            return this;
        }

        public Builder withWaitForSelectorsTimeout(float val) {
            waitForSelectorsTimeout = val;
            return this;
        }

        public Builder withFailIfSelectorsNotFound(boolean val) {
            failIfSelectorsNotFound = val;
            return this;
        }

        public Builder withPath(String val) {
            paths = ImmutableList.of(val);
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
                Float.compare(urlConfig.waitForSelectorsTimeout, waitForSelectorsTimeout) == 0 &&
                failIfSelectorsNotFound == urlConfig.failIfSelectorsNotFound &&
                ignoreAntiAliasing == urlConfig.ignoreAntiAliasing &&
                strictColorComparison == urlConfig.strictColorComparison &&
                Float.compare(urlConfig.maxColorDistance, maxColorDistance) == 0 &&
                Objects.equals(paths, urlConfig.paths) &&
                Objects.equals(setupPaths, urlConfig.setupPaths) &&
                Objects.equals(cleanupPaths, urlConfig.cleanupPaths) &&
                Objects.equals(cookies, urlConfig.cookies) &&
                Objects.equals(envMapping, urlConfig.envMapping) &&
                Objects.equals(localStorage, urlConfig.localStorage) &&
                Objects.equals(sessionStorage, urlConfig.sessionStorage) &&
                Objects.equals(windowWidths, urlConfig.windowWidths) &&
                Objects.equals(devices, urlConfig.devices) &&
                Objects.equals(javaScript, urlConfig.javaScript) &&
                Objects.equals(httpCheck, urlConfig.httpCheck) &&
                Objects.equals(removeSelectors, urlConfig.removeSelectors) &&
                Objects.equals(waitForSelectors, urlConfig.waitForSelectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, setupPaths, cleanupPaths, maxDiff, cookies, envMapping, localStorage, sessionStorage, windowWidths, devices, maxScrollHeight, waitAfterPageLoad, waitAfterScroll, waitForNoAnimationAfterScroll, warmupBrowserCacheTime, waitForFontsTime, javaScript, httpCheck, hideImages, removeSelectors, waitForSelectors, waitForSelectorsTimeout, failIfSelectorsNotFound, ignoreAntiAliasing, strictColorComparison, maxColorDistance);
    }

    @Override
    public String toString() {
        return "UrlConfig{" +
                "paths=" + paths +
                ", setupPaths=" + setupPaths +
                ", cleanupPaths=" + cleanupPaths +
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
                ", httpCheck=" + httpCheck +
                ", hideImages=" + hideImages +
                ", removeSelectors=" + removeSelectors +
                ", waitForSelectors=" + waitForSelectors +
                ", waitForSelectorsTimeout=" + waitForSelectorsTimeout +
                ", failIfSelectorsNotFound=" + failIfSelectorsNotFound +
                ", ignoreAntiAliasing=" + ignoreAntiAliasing +
                ", strictColorComparison=" + strictColorComparison +
                ", maxColorDistance=" + maxColorDistance +
                '}';
    }

}
