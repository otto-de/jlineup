package de.otto.jlineup.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.browser.Browser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public final class Config {

    public final Map<String, UrlConfig> urls;
    public final Browser.Type browser;
    @SerializedName("async-wait")
    public final float asyncWait;

    private final static Gson gson = new Gson();

    public Config(Map<String, UrlConfig> urls, Browser.Type browser, float asyncWait) {
        this.urls = urls;
        this.browser = browser;
        this.asyncWait = asyncWait;
    }

    public static Config readConfig(String path) throws FileNotFoundException {

        BufferedReader br = new BufferedReader(new FileReader(path));
        return gson.fromJson(br, Config.class);

    }


}
