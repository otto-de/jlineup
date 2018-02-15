package de.otto.jlineup.browser;

import de.otto.jlineup.config.UrlConfig;

public final class ScreenshotContext {
    public final String url;
    public final String urlSubPath;
    public final int windowWidth;
    public final boolean before;
    public final UrlConfig urlConfig;

    ScreenshotContext(String url, String urlSubPath, int windowWidth, boolean before, UrlConfig urlConfig) {
        this.url = url;
        this.urlSubPath = urlSubPath;
        this.windowWidth = windowWidth;
        this.before = before;
        this.urlConfig = urlConfig;
    }

    public static ScreenshotContext of(String url, String path, int windowWidth, boolean before, UrlConfig urlConfig) {
        return new ScreenshotContext(url, path, windowWidth, before, urlConfig);
    }

    @Override
    public String toString() {
        return "ScreenshotContext{" +
                "url='" + url + '\'' +
                ", urlSubPath='" + urlSubPath + '\'' +
                ", windowWidth=" + windowWidth +
                ", before=" + before +
                ", urlConfig=" + urlConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScreenshotContext that = (ScreenshotContext) o;

        if (windowWidth != that.windowWidth) return false;
        if (before != that.before) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (urlSubPath != null ? !urlSubPath.equals(that.urlSubPath) : that.urlSubPath != null) return false;
        return urlConfig != null ? urlConfig.equals(that.urlConfig) : that.urlConfig == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (urlSubPath != null ? urlSubPath.hashCode() : 0);
        result = 31 * result + windowWidth;
        result = 31 * result + (before ? 1 : 0);
        result = 31 * result + (urlConfig != null ? urlConfig.hashCode() : 0);
        return result;
    }
}
