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


    public static Config readConfig(final Parameters parameters) {
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
