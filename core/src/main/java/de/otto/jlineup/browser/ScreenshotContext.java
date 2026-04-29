package de.otto.jlineup.browser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.annotation.JsonDeserialize;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.UrlConfig;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonDeserialize(builder = ScreenshotContext.Builder.class)
public final class ScreenshotContext  {
    public final String url;
    public final String urlSubPath;
    public final String urlSubPathTitle;
    public final DeviceConfig deviceConfig;
    public final List<Cookie> cookies;
    @JsonIgnore
    public final BrowserStep step;
    @JsonIgnore
    public final UrlConfig urlConfig;
    @JsonIgnore
    final String fullPathOfReportDir;
    @JsonIgnore
    final boolean dontShareBrowser;
    @JsonIgnore
    public final String urlKey;
    public final Browser.Type browserType;

    @JsonCreator
    ScreenshotContext(String url, String urlSubPath, DeviceConfig deviceConfig, List<Cookie> cookies, BrowserStep step, UrlConfig urlConfig, String fullPathOfReportDir, boolean dontShareBrowser, String urlKey, Browser.Type browserType) {
        this(url, urlSubPath, deviceConfig, cookies, step, urlConfig, fullPathOfReportDir, dontShareBrowser, urlKey, browserType, null);
    }

    ScreenshotContext(String url, String urlSubPath, DeviceConfig deviceConfig, List<Cookie> cookies, BrowserStep step, UrlConfig urlConfig, String fullPathOfReportDir, boolean dontShareBrowser, String urlKey, Browser.Type browserType, String urlSubPathTitle) {
        this.url = url;
        this.urlSubPath = urlSubPath;
        this.urlSubPathTitle = urlSubPathTitle;
        this.deviceConfig = deviceConfig;
        this.cookies = cookies;
        this.step = step;
        this.urlConfig = urlConfig;
        this.fullPathOfReportDir = fullPathOfReportDir;
        this.dontShareBrowser = dontShareBrowser;
        this.urlKey = urlKey;
        this.browserType = browserType;
    }

    private ScreenshotContext(Builder builder) {
        url = builder.url;
        urlSubPath = builder.urlSubPath;
        urlSubPathTitle = builder.urlSubPathTitle;
        deviceConfig = builder.deviceConfig;
        cookies = builder.cookies;
        step = builder.step;
        urlConfig = builder.urlConfig;
        fullPathOfReportDir = builder.fullPathOfReportDir;
        dontShareBrowser = builder.dontShareBrowser;
        urlKey = builder.urlKey;
        browserType = builder.browserType;
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, BrowserStep step, UrlConfig urlConfig, List<Cookie> cookies, String urlKey) {
        return new ScreenshotContext(url, path, deviceConfig, cookies, step, urlConfig, null, false, urlKey, null);
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, BrowserStep step, UrlConfig urlConfig, List<Cookie> cookies, String urlKey, Browser.Type browserType) {
        return new ScreenshotContext(url, path, deviceConfig, cookies, step, urlConfig, null, false, urlKey, browserType);
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, BrowserStep step, UrlConfig urlConfig, List<Cookie> cookies) {
        return new ScreenshotContext(url, path, deviceConfig, cookies, step, urlConfig, null, false, url, null);
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, BrowserStep step, UrlConfig urlConfig, List<Cookie> cookies, Browser.Type browserType) {
        return new ScreenshotContext(url, path, deviceConfig, cookies, step, urlConfig, null, false, url, browserType);
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, BrowserStep step, UrlConfig urlConfig) {
        return new ScreenshotContext(url, path, deviceConfig, urlConfig.cookies, step, urlConfig, null, false, url, null);
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, BrowserStep step, UrlConfig urlConfig, Browser.Type browserType) {
        return new ScreenshotContext(url, path, deviceConfig, urlConfig.cookies, step, urlConfig, null, false, url, browserType);
    }

    public static Builder screenshotContextBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(ScreenshotContext copy) {
        Builder builder = new Builder();
        builder.url = copy.url;
        builder.urlSubPath = copy.urlSubPath;
        builder.urlSubPathTitle = copy.urlSubPathTitle;
        builder.deviceConfig = copy.deviceConfig;
        builder.cookies = copy.cookies;
        builder.step = copy.step;
        builder.urlConfig = copy.urlConfig;
        builder.fullPathOfReportDir = copy.fullPathOfReportDir;
        builder.dontShareBrowser = copy.dontShareBrowser;
        builder.urlKey = copy.urlKey;
        builder.browserType = copy.browserType;
        return builder;
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

    public String getUrl() {
        return url;
    }

    public String getUrlSubPath() {
        return urlSubPath;
    }

    public String getUrlSubPathTitle() {
        return urlSubPathTitle;
    }

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public Browser.Type getBrowserType() {
        return browserType;
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

    public String contextHash() {
        return Integer.toHexString(Objects.hash(urlKey, urlSubPath, deviceConfig, cookies != null ? cookies.stream().filter(Cookie::isScreenshotContextGiving).collect(Collectors.toList()) : null, browserType != null ? browserType.name() : null));
    }

    /**
     * Returns a key that identifies the "sibling group" for flaky-tolerance matching.
     * Two contexts are siblings if they share the same urlKey, urlSubPath, and set of
     * screenshot-context-giving cookies — but may differ in device config.
     */
    public String flakySiblingKey() {
        return Integer.toHexString(Objects.hash(urlKey, urlSubPath, cookies != null ? cookies.stream().filter(Cookie::isScreenshotContextGiving).collect(Collectors.toList()) : null));
    }

    public boolean equalsIgnoreStep(ScreenshotContext that) {
        if (this == that) return true;
        if (that == null) return false;
        return Objects.equals(url, that.url) &&
                Objects.equals(urlSubPath, that.urlSubPath) &&
                Objects.equals(deviceConfig, that.deviceConfig) &&
                Objects.equals(urlConfig, that.urlConfig) &&
                Objects.equals(fullPathOfReportDir, that.fullPathOfReportDir) &&
                Objects.equals(urlKey, that.urlKey) &&
                browserType == that.browserType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenshotContext that = (ScreenshotContext) o;
        return dontShareBrowser == that.dontShareBrowser && Objects.equals(url, that.url) && Objects.equals(urlSubPath, that.urlSubPath) && Objects.equals(deviceConfig, that.deviceConfig) && Objects.equals(cookies, that.cookies) && step == that.step && Objects.equals(urlConfig, that.urlConfig) && Objects.equals(fullPathOfReportDir, that.fullPathOfReportDir) && Objects.equals(urlKey, that.urlKey) && browserType == that.browserType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, urlSubPath, deviceConfig, cookies, step, urlConfig, fullPathOfReportDir, dontShareBrowser, urlKey, browserType);
    }

    @Override
    public String toString() {
        return "ScreenshotContext{" +
                "url='" + url + '\'' +
                ", urlSubPath='" + urlSubPath + '\'' +
                ", deviceConfig=" + deviceConfig +
                ", cookies=" + cookies +
                ", step=" + step +
                ", urlConfig=" + urlConfig +
                ", fullPathOfReportDir='" + fullPathOfReportDir + '\'' +
                ", dontShareBrowser=" + dontShareBrowser +
                ", urlKey='" + urlKey + '\'' +
                ", browserType=" + browserType +
                '}';
    }

    @JsonIgnore
    public String getShortDescription() {
        if (!deviceConfig.isSpecificMobile()) {
            return url + " with device " + deviceConfig.deviceName + " (" + deviceConfig.width + "x" + deviceConfig.height + ")";
        } else {
            return url + " with device " + deviceConfig.deviceName;
        }
    }

    public static final class Builder {
        private String url;
        private String urlSubPath;
        private String urlSubPathTitle;
        private DeviceConfig deviceConfig;
        private List<Cookie> cookies = Collections.emptyList();
        private BrowserStep step;
        private UrlConfig urlConfig;
        private String fullPathOfReportDir;
        private boolean dontShareBrowser;
        private String urlKey;
        private Browser.Type browserType;

        private Builder() {
        }

        public Builder withUrl(String val) {
            url = val;
            return this;
        }

        public Builder withUrlSubPath(String val) {
            urlSubPath = val;
            return this;
        }

        public Builder withUrlSubPathTitle(String val) {
            urlSubPathTitle = val;
            return this;
        }

        public Builder withDeviceConfig(DeviceConfig val) {
            deviceConfig = val;
            return this;
        }

        public Builder withCookies(List<Cookie> val) {
            cookies = val;
            return this;
        }

        public Builder withStep(BrowserStep val) {
            step = val;
            return this;
        }

        public Builder withUrlConfig(UrlConfig val) {
            urlConfig = val;
            return this;
        }

        public Builder withFullPathOfReportDir(String val) {
            fullPathOfReportDir = val;
            return this;
        }

        public Builder withDontShareBrowser(boolean val) {
            dontShareBrowser = val;
            return this;
        }

        public Builder withUrlKey(String val) {
            urlKey = val;
            return this;
        }

        public Builder withBrowserType(Browser.Type val) {
            browserType = val;
            return this;
        }

        public ScreenshotContext build() {
            return new ScreenshotContext(this);
        }
    }
}
