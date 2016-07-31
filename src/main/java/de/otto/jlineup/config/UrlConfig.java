package de.otto.jlineup.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class UrlConfig {

    List<String> paths;
    @SerializedName("max-diff")
    float maxDiff;
    List<Cookie> cookies;
    @SerializedName("env-mapping")
    Map<String, String> envMapping;

    @SerializedName("local-storage")
    Map<String, String> localStorage;

    List<Integer> resolutions;

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
