package de.otto.jlineup.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.otto.jlineup.config.Config.DEFAULT_PATHS;
import static de.otto.jlineup.config.Config.DEFAULT_WINDOW_WIDTHS;

public class UrlConfig {

    public final List<String> paths;

    @SerializedName("max-diff")
    public final float maxDiff;

    public final List<Cookie> cookies;

    @SerializedName("env-mapping")
    public final Map<String, String> envMapping;

    @SerializedName("local-storage")
    public final Map<String, String> localStorage;

    @SerializedName(value = "window-widths", alternate = {"resolutions","widths"})
    public final List<Integer> windowWidths;

    @SerializedName("max-scroll-height")
    private final Integer maxScrollHeight;

    @SerializedName("wait-after-page-load")
    private final Integer waitAfterPageLoad;

    @SerializedName("wait-for-no-animation-after-scroll")
    private final Float waitForNoAnimationAfterScroll;

    //Default constructor for GSON
    public UrlConfig() {
        this.paths = DEFAULT_PATHS;
        this.windowWidths = DEFAULT_WINDOW_WIDTHS;
        this.maxDiff = 0;
        this.cookies = null;
        this.localStorage = null;
        this.maxScrollHeight = null;
        this.waitAfterPageLoad = null;
        this.waitForNoAnimationAfterScroll = null;
        this.envMapping = null;
    }

    public UrlConfig(List<String> paths, float maxDiff, List<Cookie> cookies, Map<String, String> envMapping, Map<String, String> localStorage, List<Integer> windowWidths, Integer maxScrollHeight, Integer waitAfterPageLoad, Float waitForNoAnimationAfterScroll) {
        this.paths = paths != null ? paths : DEFAULT_PATHS;
        this.windowWidths = windowWidths != null ? windowWidths : DEFAULT_WINDOW_WIDTHS;
        this.maxDiff = maxDiff;
        this.cookies = cookies;
        this.envMapping = envMapping;
        this.localStorage = localStorage;
        this.maxScrollHeight = maxScrollHeight;
        this.waitAfterPageLoad = waitAfterPageLoad;
        this.waitForNoAnimationAfterScroll = waitForNoAnimationAfterScroll;
    }

    @Override
    public String toString() {
        return "UrlConfig{" +
                "paths=" + paths +
                ", maxDiff=" + maxDiff +
                ", cookies=" + cookies +
                ", envMapping=" + envMapping +
                ", localStorage=" + localStorage +
                ", windowWidths=" + windowWidths +
                ", maxScrollHeight=" + maxScrollHeight +
                ", waitAfterPageLoad=" + waitAfterPageLoad +
                ", waitForNoAnimationAfterScroll=" + waitForNoAnimationAfterScroll +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlConfig urlConfig = (UrlConfig) o;
        return Float.compare(urlConfig.maxDiff, maxDiff) == 0 &&
                Objects.equals(paths, urlConfig.paths) &&
                Objects.equals(cookies, urlConfig.cookies) &&
                Objects.equals(envMapping, urlConfig.envMapping) &&
                Objects.equals(localStorage, urlConfig.localStorage) &&
                Objects.equals(windowWidths, urlConfig.windowWidths) &&
                Objects.equals(maxScrollHeight, urlConfig.maxScrollHeight) &&
                Objects.equals(waitAfterPageLoad, urlConfig.waitAfterPageLoad) &&
                Objects.equals(waitForNoAnimationAfterScroll, urlConfig.waitForNoAnimationAfterScroll);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, maxDiff, cookies, envMapping, localStorage, windowWidths, maxScrollHeight, waitAfterPageLoad, waitForNoAnimationAfterScroll);
    }

    public Optional<Integer> getMaxScrollHeight() {
        return Optional.ofNullable(maxScrollHeight);
    }

    public Optional<Integer> getWaitAfterPageLoad() {
        return Optional.ofNullable(waitAfterPageLoad);
    }

    public Optional<Float> getWaitForNoAnimationAfterScroll() {
        return Optional.ofNullable(waitForNoAnimationAfterScroll);
    }
}
