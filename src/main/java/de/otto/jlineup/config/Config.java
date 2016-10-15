package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.browser.Browser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Config {

    public static final String LINEUP_CONFIG_DEFAULT_PATH = "./lineup.json";

    public static final String EXAMPLE_URL = "https://www.example.com";

    public static final Browser.Type DEFAULT_BROWSER = Browser.Type.PHANTOMJS;
    public static final float DEFAULT_MAX_DIFF = 0;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    public static final float DEFAULT_ASYNC_WAIT = 0f;
    public static final List<Integer> DEFAULT_WINDOW_WIDTHS = ImmutableList.of(800);
    public static final List<String> DEFAULT_PATHS = ImmutableList.of("/");
    public static final int DEFAULT_MAX_SCROLL_HEIGHT = 100000;
    public static final int DEFAULT_WAIT_AFTER_PAGE_LOAD = 0;
    public static final int DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL = 0;

    public final Map<String, UrlConfig> urls;
    public final Browser.Type browser;
    @SerializedName("async-wait")
    public final Float asyncWait;
    @SerializedName("window-height")
    public final Integer windowHeight;

    private final static Gson gson = new Gson();

    /* Used by GSON to set default values */
    public Config() {
        urls = null;
        browser = DEFAULT_BROWSER;
        asyncWait = DEFAULT_ASYNC_WAIT;
        windowHeight = DEFAULT_WINDOW_HEIGHT;

    }

    public Config(final Map<String, UrlConfig> urls, final Browser.Type browser, final Float asyncWait, final Integer windowHeight) {
        this.urls = urls;
        this.browser = browser != null ? browser : DEFAULT_BROWSER;
        this.asyncWait = asyncWait != null ? asyncWait : DEFAULT_ASYNC_WAIT;
        this.windowHeight = windowHeight != null ? windowHeight : DEFAULT_WINDOW_HEIGHT;
    }

    public static Config defaultConfig() {
        return defaultConfig(EXAMPLE_URL);
    }

    public static Config defaultConfig(String url) {
        return new Config(ImmutableMap.of(url, new UrlConfig()), null, null, null);
    }

    public static Config readConfig(final Parameters parameters) throws FileNotFoundException {
        return Config.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
    }

    public static Config readConfig(final String workingDir, final String configFileName) throws FileNotFoundException {

        List<String> searchPaths = new ArrayList<>();
        Path configFilePath = Paths.get(workingDir + "/" + configFileName);
        searchPaths.add(configFilePath.toString());
        if (!Files.exists(configFilePath)) {
            configFilePath = Paths.get(configFileName);
            searchPaths.add(configFilePath.toString());
            if (!Files.exists(configFilePath)) {
                configFilePath = Paths.get(LINEUP_CONFIG_DEFAULT_PATH);
                searchPaths.add(configFilePath.toString());
                if (!Files.exists(configFilePath)) {
                    throw new FileNotFoundException("Config file not found. Search locations were: " + Arrays.toString(searchPaths.toArray()));
                }
            }
        }

        BufferedReader br = new BufferedReader(new FileReader(configFilePath.toString()));
        return gson.fromJson(br, Config.class);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (urls != null ? !urls.equals(config.urls) : config.urls != null) return false;
        if (browser != config.browser) return false;
        if (asyncWait != null ? !asyncWait.equals(config.asyncWait) : config.asyncWait != null) return false;
        return windowHeight != null ? windowHeight.equals(config.windowHeight) : config.windowHeight == null;

    }

    @Override
    public int hashCode() {
        int result = urls != null ? urls.hashCode() : 0;
        result = 31 * result + (browser != null ? browser.hashCode() : 0);
        result = 31 * result + (asyncWait != null ? asyncWait.hashCode() : 0);
        result = 31 * result + (windowHeight != null ? windowHeight.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Config{" +
                "urls=" + urls +
                ", browser=" + browser +
                ", asyncWait=" + asyncWait +
                ", windowHeight=" + windowHeight +
                '}';
    }
}
