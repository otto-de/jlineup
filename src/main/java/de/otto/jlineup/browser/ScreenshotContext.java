package de.otto.jlineup.browser;

import java.util.Objects;

final class ScreenshotContext {
    final String url;
    final String path;
    final int windowWidth;
    final boolean before;

    ScreenshotContext(String url, String path, int windowWidth, boolean before) {
        this.url = url;
        this.path = path;
        this.windowWidth = windowWidth;
        this.before = before;
    }

    public static ScreenshotContext of(String url, String path, int windowWidth, boolean before) {
        return new ScreenshotContext(url, path, windowWidth, before);
    }

    @Override
    public String toString() {
        return "ScreenshotContext{" +
                "url='" + url + '\'' +
                ", path='" + path + '\'' +
                ", windowWidth=" + windowWidth +
                ", before=" + before +
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
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, path, windowWidth, before);
    }
}
