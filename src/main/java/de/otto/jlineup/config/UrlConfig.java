package de.otto.jlineup.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class UrlConfig {

    public final List<String> paths;
    @SerializedName("max-diff")
    public final float maxDiff;
    public final List<Cookie> cookies;
    @SerializedName("env-mapping")
    public final Map<String, String> envMapping;

    @SerializedName("local-storage")
    public final Map<String, String> localStorage;

    public final List<Integer> resolutions;

    public UrlConfig(List<String> paths, float maxDiff, List<Cookie> cookies, Map<String, String> envMapping, Map<String, String> localStorage, List<Integer> resolutions) {
        this.paths = paths;
        this.maxDiff = maxDiff;
        this.cookies = cookies;
        this.envMapping = envMapping;
        this.localStorage = localStorage;
        this.resolutions = resolutions;
    }

    @Override
    public String toString() {
        return "UrlConfig{" +
                "paths=" + paths +
                ", maxDiff=" + maxDiff +
                ", cookies=" + cookies +
                ", envMapping=" + envMapping +
                ", localStorage=" + localStorage +
                ", resolutions=" + resolutions +
                '}';
    }
}
