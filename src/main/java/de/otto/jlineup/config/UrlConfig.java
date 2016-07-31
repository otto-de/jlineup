package de.otto.jlineup.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class UrlConfig {

    public List<String> paths;
    @SerializedName("max-diff")
    float maxDiff;
    public List<Cookie> cookies;
    @SerializedName("env-mapping")
    public Map<String, String> envMapping;

    @SerializedName("local-storage")
    public Map<String, String> localStorage;

    public List<Integer> resolutions;

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
