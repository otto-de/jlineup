package de.otto.jlineup.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public UrlConfig(List<String> paths, float maxDiff, List<Cookie> cookies, Map<String, String> envMapping, Map<String, String> localStorage, List<Integer> windowWidths, Integer maxScrollHeight, Integer waitAfterPageLoad) {
        this.paths = paths;
        this.maxDiff = maxDiff;
        this.cookies = cookies;
        this.envMapping = envMapping;
        this.localStorage = localStorage;
        this.windowWidths = windowWidths;
        this.maxScrollHeight = maxScrollHeight;
        this.waitAfterPageLoad = waitAfterPageLoad;
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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UrlConfig urlConfig = (UrlConfig) o;

        if (Float.compare(urlConfig.maxDiff, maxDiff) != 0) return false;
        if (paths != null ? !paths.equals(urlConfig.paths) : urlConfig.paths != null) return false;
        if (cookies != null ? !cookies.equals(urlConfig.cookies) : urlConfig.cookies != null) return false;
        if (envMapping != null ? !envMapping.equals(urlConfig.envMapping) : urlConfig.envMapping != null) return false;
        if (localStorage != null ? !localStorage.equals(urlConfig.localStorage) : urlConfig.localStorage != null)
            return false;
        if (windowWidths != null ? !windowWidths.equals(urlConfig.windowWidths) : urlConfig.windowWidths != null)
            return false;
        if (maxScrollHeight != null ? !maxScrollHeight.equals(urlConfig.maxScrollHeight) : urlConfig.maxScrollHeight != null)
            return false;
        return waitAfterPageLoad != null ? waitAfterPageLoad.equals(urlConfig.waitAfterPageLoad) : urlConfig.waitAfterPageLoad == null;

    }

    @Override
    public int hashCode() {
        int result = paths != null ? paths.hashCode() : 0;
        result = 31 * result + (maxDiff != +0.0f ? Float.floatToIntBits(maxDiff) : 0);
        result = 31 * result + (cookies != null ? cookies.hashCode() : 0);
        result = 31 * result + (envMapping != null ? envMapping.hashCode() : 0);
        result = 31 * result + (localStorage != null ? localStorage.hashCode() : 0);
        result = 31 * result + (windowWidths != null ? windowWidths.hashCode() : 0);
        result = 31 * result + (maxScrollHeight != null ? maxScrollHeight.hashCode() : 0);
        result = 31 * result + (waitAfterPageLoad != null ? waitAfterPageLoad.hashCode() : 0);
        return result;
    }

    public Optional<Integer> getMaxScrollHeight() {
        return Optional.ofNullable(maxScrollHeight);
    }

    public Optional<Integer> getWaitAfterPageLoad() {
        return Optional.ofNullable(waitAfterPageLoad);
    }
}
