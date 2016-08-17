package de.otto.jlineup.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.browser.Browser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public final class Config {

    private static final Integer DEFAULT_WINDOW_HEIGHT = 800;
    private static final Float DEFAULT_ASYNC_WAIT = 0f;

    private Map<String, UrlConfig> urls;
    private Browser.Type browser;
    @SerializedName("async-wait")
    private Float asyncWait = DEFAULT_ASYNC_WAIT;
    @SerializedName("window-height")
    private Integer windowHeight = DEFAULT_WINDOW_HEIGHT;

    private final static Gson gson = new Gson();

    /* Used by GSON to set default values */
    public Config() {
        asyncWait = DEFAULT_ASYNC_WAIT;
        windowHeight = DEFAULT_WINDOW_HEIGHT;
    }

    public Config(Map<String, UrlConfig> urls, Browser.Type browser, Float asyncWait, Integer windowHeight) {
        this.urls = urls;
        this.browser = browser;
        this.asyncWait = asyncWait != null ? asyncWait : 0;
        this.windowHeight = windowHeight != null ? windowHeight : DEFAULT_WINDOW_HEIGHT;
    }

    public static Config readConfig(String path) throws FileNotFoundException {

        BufferedReader br = new BufferedReader(new FileReader(path));
        return gson.fromJson(br, Config.class);

    }

    public Browser.Type getBrowser() {
        return browser;
    }

    public Float getAsyncWait() {
        return asyncWait;
    }

    public Map<String, UrlConfig> getUrls() {
        return urls;
    }

    public Integer getWindowHeight() {
        return windowHeight;
    }
}
