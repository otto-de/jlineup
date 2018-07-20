package de.otto.jlineup.browser;

import de.otto.jlineup.config.UrlConfig;

import java.util.Objects;

public final class ScreenshotContext {
    public final String url;
    public final String urlSubPath;
    public final int windowWidth;
    public final boolean before;
    public final UrlConfig urlConfig;
    public final String fullPathOfReportDir;

    ScreenshotContext(String url, String urlSubPath, int windowWidth, boolean before, UrlConfig urlConfig, String fullPathOfReportDir) {
        this.url = url;
        this.urlSubPath = urlSubPath;
        this.windowWidth = windowWidth;
        this.before = before;
        this.urlConfig = urlConfig;
        this.fullPathOfReportDir = fullPathOfReportDir;
    }

    public static ScreenshotContext of(String url, String path, int windowWidth, boolean before, UrlConfig urlConfig) {
        return new ScreenshotContext(url, path, windowWidth, before, urlConfig, null);
    }

    @Override
    public String toString() {
        return "ScreenshotContext{" +
                "url='" + url + '\'' +
                ", urlSubPath='" + urlSubPath + '\'' +
                ", windowWidth=" + windowWidth +
                ", before=" + before +
                ", urlConfig=" + urlConfig +
                ", fullPathOfReportDir='" + fullPathOfReportDir + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenshotContext that = (ScreenshotContext) o;
        return windowWidth == that.windowWidth &&
                before == that.before &&
                Objects.equals(url, that.url) &&
                Objects.equals(urlSubPath, that.urlSubPath) &&
                Objects.equals(urlConfig, that.urlConfig) &&
                Objects.equals(fullPathOfReportDir, that.fullPathOfReportDir);
    }

    @Override
    public int hashCode() {

        return Objects.hash(url, urlSubPath, windowWidth, before, urlConfig, fullPathOfReportDir);
    }

}
