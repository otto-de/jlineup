package de.otto.jlineup.browser;

import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;

import java.util.Objects;

public final class ScreenshotContext {
    public final String url;
    public final String urlSubPath;
    public final DeviceConfig deviceConfig;
    public final Step step;
    public final UrlConfig urlConfig;
    public final String fullPathOfReportDir;

    ScreenshotContext(String url, String urlSubPath, DeviceConfig deviceConfig, boolean before, UrlConfig urlConfig, String fullPathOfReportDir) {
        this.url = url;
        this.urlSubPath = urlSubPath;
        this.deviceConfig = deviceConfig;
        this.step = before ? Step.before : Step.after;
        this.urlConfig = urlConfig;
        this.fullPathOfReportDir = fullPathOfReportDir;
    }

    public static ScreenshotContext of(String url, String path, DeviceConfig deviceConfig, boolean before, UrlConfig urlConfig) {
        return new ScreenshotContext(url, path, deviceConfig, before, urlConfig, null);
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
