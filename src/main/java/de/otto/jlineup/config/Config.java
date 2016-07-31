package de.otto.jlineup.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public final class Config {

    Map<String, UrlConfig> urls;
    String browser;
    @SerializedName("async-wait")
    float asyncWait;

    private final static Gson gson = new Gson();

    public static Config readConfig(String path) throws FileNotFoundException {

        BufferedReader br = new BufferedReader(new FileReader(path));
        return gson.fromJson(br, Config.class);

    }


}
