package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
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

    public static final Browser.Type DEFAULT_BROWSER = Browser.Type.PHANTOMJS;
    public static final Integer DEFAULT_WINDOW_HEIGHT = 800;
    public static final Float DEFAULT_ASYNC_WAIT = 0f;
    public static final List<Integer> DEFAULT_WINDOW_WIDTHS = ImmutableList.of(800);
    public static final List<String> DEFAULT_PATHS = ImmutableList.of("/");

    private final Map<String, UrlConfig> urls;
    private final Browser.Type browser;
    @SerializedName("async-wait")
    private final Float asyncWait;
    @SerializedName("window-height")
    private final Integer windowHeight;

    private final static Gson gson = new Gson();

    /* Used by GSON to set default values */
    public Config() {
        urls = null;
        browser = DEFAULT_BROWSER;
        asyncWait = DEFAULT_ASYNC_WAIT;
        windowHeight = DEFAULT_WINDOW_HEIGHT;

    }

    public Config(Map<String, UrlConfig> urls, Browser.Type browser, Float asyncWait, Integer windowHeight) {
        this.urls = urls;
        this.browser = browser != null ? browser : DEFAULT_BROWSER;
        this.asyncWait = asyncWait != null ? asyncWait : DEFAULT_ASYNC_WAIT;
        this.windowHeight = windowHeight != null ? windowHeight : DEFAULT_WINDOW_HEIGHT;
    }


    public static Config readConfig(Parameters parameters) {
        Config config = null;
        try {
            config = Config.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return config;
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
