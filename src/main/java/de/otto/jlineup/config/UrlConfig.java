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

    public Optional<Integer> getMaxScrollHeight() {
        return Optional.ofNullable(maxScrollHeight);
    }

    public Optional<Integer> getWaitAfterPageLoad() {
        return Optional.ofNullable(waitAfterPageLoad);
    }
}
