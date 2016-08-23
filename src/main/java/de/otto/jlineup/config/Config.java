package de.otto.jlineup.config;

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

    private static final Integer DEFAULT_WINDOW_HEIGHT = 800;
    private static final Float DEFAULT_ASYNC_WAIT = 0f;
    public static final String LINEUP_CONFIG_DEFAULT_PATH = "./lineup.json";

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
