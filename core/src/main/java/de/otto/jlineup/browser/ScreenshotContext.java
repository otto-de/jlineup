package de.otto.jlineup.browser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;

import java.util.Objects;

@JsonDeserialize(builder = ScreenshotContext.Builder.class)
public final class ScreenshotContext  {
    public final String url;
    public final String urlSubPath;
    public final DeviceConfig deviceConfig;
    @JsonIgnore
    public final Step step;
    @JsonIgnore
    public final UrlConfig urlConfig;
    @JsonIgnore
    final String fullPathOfReportDir;
    @JsonIgnore
    final boolean dontShareBrowser;

    ScreenshotContext(String url, String urlSubPath, DeviceConfig deviceConfig, Step step, UrlConfig urlConfig, String fullPathOfReportDir, boolean dontShareBrowser) {
        this.url = url;
        this.urlSubPath = urlSubPath;
        this.deviceConfig = deviceConfig;
        this.step = step;
        this.urlConfig = urlConfig;
        this.fullPathOfReportDir = fullPathOfReportDir;
        this.dontShareBrowser = dontShareBrowser;
    }

    //Used by Jackson
    private ScreenshotContext() {
        this(screenshotContextBuilder());
    }

    private ScreenshotContext(Builder builder) {
        url = builder.url;
        urlSubPath = builder.urlSubPath;
        deviceConfig = builder.deviceConfig;
        step = builder.step;
        urlConfig = builder.urlConfig;
        fullPathOfReportDir = builder.fullPathOfReportDir;
        dontShareBrowser = builder.dontShareBrowser;
    }

    //Used in Tests only
    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, Step step, UrlConfig urlConfig) {
        return new ScreenshotContext(url, path, deviceConfig, step, urlConfig, null, false);
    }

    public static Builder screenshotContextBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(ScreenshotContext copy) {
        Builder builder = new Builder();
        builder.url = copy.url;
        builder.urlSubPath = copy.urlSubPath;
        builder.deviceConfig = copy.deviceConfig;
        builder.step = copy.step;
        builder.urlConfig = copy.urlConfig;
        builder.fullPathOfReportDir = copy.fullPathOfReportDir;
        builder.dontShareBrowser = copy.dontShareBrowser;
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

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
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

    public int contextHash() {
        return Objects.hash(url, urlSubPath, deviceConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, urlSubPath, deviceConfig, step, urlConfig, fullPathOfReportDir, dontShareBrowser);
    }

    public boolean equalsIgnoreStep(ScreenshotContext that) {
        if (this == that) return true;
        if (that == null) return false;
        return Objects.equals(url, that.url) &&
                Objects.equals(urlSubPath, that.urlSubPath) &&
                Objects.equals(deviceConfig, that.deviceConfig) &&
                Objects.equals(urlConfig, that.urlConfig) &&
                Objects.equals(fullPathOfReportDir, that.fullPathOfReportDir);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenshotContext that = (ScreenshotContext) o;
        return dontShareBrowser == that.dontShareBrowser &&
                Objects.equals(url, that.url) &&
                Objects.equals(urlSubPath, that.urlSubPath) &&
                Objects.equals(deviceConfig, that.deviceConfig) &&
                step == that.step &&
                Objects.equals(urlConfig, that.urlConfig) &&
                Objects.equals(fullPathOfReportDir, that.fullPathOfReportDir);
    }

    @Override
    public String toString() {
        return "ScreenshotContext{" +
                "url='" + url + '\'' +
                ", urlSubPath='" + urlSubPath + '\'' +
                ", deviceConfig=" + deviceConfig +
                ", step=" + step +
                ", urlConfig=" + urlConfig +
                ", fullPathOfReportDir='" + fullPathOfReportDir + '\'' +
                ", dontShareBrowser=" + dontShareBrowser +
                '}';
    }

    public static final class Builder {
        private String url;
        private String urlSubPath;
        private DeviceConfig deviceConfig;
        private Step step;
        private UrlConfig urlConfig;
        private String fullPathOfReportDir;
        private boolean dontShareBrowser;

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

        public Builder withDeviceConfig(DeviceConfig val) {
            deviceConfig = val;
            return this;
        }

        public Builder withStep(Step val) {
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

        public ScreenshotContext build() {
            return new ScreenshotContext(this);
        }
    }
}
