package de.otto.jlineup.browser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public final class ScreenshotContext {
    public final String url;
    public final String urlSubPath;
    public final DeviceConfig deviceConfig;
    @JsonIgnore
    public final Step step;
    @JsonIgnore
    public final UrlConfig urlConfig;
    @JsonIgnore
    final String fullPathOfReportDir;

    ScreenshotContext(String url, String urlSubPath, DeviceConfig deviceConfig, Step step, UrlConfig urlConfig, String fullPathOfReportDir) {
        this.url = url;
        this.urlSubPath = urlSubPath;
        this.deviceConfig = deviceConfig;
        this.step = step;
        this.urlConfig = urlConfig;
        this.fullPathOfReportDir = fullPathOfReportDir;
    }

    //Used by Jackson
    private ScreenshotContext() {
        this.url = null;
        this.urlSubPath = null;
        this.deviceConfig = null;
        this.step = null;
        this.urlConfig = null;
        this.fullPathOfReportDir = null;
    }

    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, Step step, UrlConfig urlConfig) {
        return new ScreenshotContext(url, path, deviceConfig, step, urlConfig, null);
    }

    public int contextHash() {
        return Objects.hash(url, urlSubPath, deviceConfig, urlConfig);
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
        return Objects.equals(url, that.url) &&
                Objects.equals(urlSubPath, that.urlSubPath) &&
                Objects.equals(deviceConfig, that.deviceConfig) &&
                step == that.step &&
                Objects.equals(urlConfig, that.urlConfig) &&
                Objects.equals(fullPathOfReportDir, that.fullPathOfReportDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, urlSubPath, deviceConfig, step, urlConfig, fullPathOfReportDir);
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
                '}';
    }
}
