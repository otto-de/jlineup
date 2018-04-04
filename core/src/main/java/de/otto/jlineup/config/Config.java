package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.browser.Browser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.otto.jlineup.config.Cookie.COOKIE_TIME_FORMAT;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Config {

    public static final String LINEUP_CONFIG_DEFAULT_PATH = "./lineup.json";
    public static final String EXAMPLE_URL = "https://www.example.com";

    public static final int DEFAULT_WARMUP_BROWSER_CACHE_TIME = 0;
    public static final int DEFAULT_REPORT_FORMAT = 2;

    static final Browser.Type DEFAULT_BROWSER = Browser.Type.PHANTOMJS;
    static final float DEFAULT_MAX_DIFF = 0;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    static final float DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD = 0f;
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    static final String DEFAULT_PATH = "/";
    static final int DEFAULT_MAX_SCROLL_HEIGHT = 100000;
    static final int DEFAULT_WAIT_AFTER_PAGE_LOAD = 0;
    static final int DEFAULT_WAIT_AFTER_SCROLL = 0;
    static final int DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL = 0;
    static final int DEFAULT_WAIT_FOR_FONTS_TIME = 0;
    static final int DEFAULT_THREADS = 1;
    static final int DEFAULT_PAGELOAD_TIMEOUT = 120;
    static final int DEFAULT_SCREENSHOT_RETRIES = 0;
    static final int DEFAULT_GLOBAL_TIMEOUT = 600;

    public final Map<String, UrlConfig> urls;
    public final Browser.Type browser;

    @SerializedName(value = "wait-after-page-load", alternate = "async-wait")
    @JsonProperty("wait-after-page-load")
    @JsonAlias({"async-wait"})
    public final Float globalWaitAfterPageLoad;
    @SerializedName(value = "page-load-timeout")
    @JsonProperty("page-load-timeout")
    public final int pageLoadTimeout;
    @SerializedName("window-height")
    @JsonProperty("window-height")
    public final Integer windowHeight;
    @SerializedName("report-format")
    @JsonProperty("report-format")
    public final Integer reportFormat;
    @SerializedName("screenshot-retries")
    @JsonProperty("screenshot-retries")
    public final int screenshotRetries;
    @SerializedName("threads")
    @JsonProperty("threads")
    public int threads;
    @SerializedName("timeout")
    @JsonProperty("timeout")
    public final int globalTimeout;
    @SerializedName("debug")
    @JsonProperty("debug")
    public final boolean debug;
    @SerializedName("log-to-file")
    @JsonProperty("log-to-file")
    public final boolean logToFile;

    private final static Gson gson = new GsonBuilder().setDateFormat(COOKIE_TIME_FORMAT).setPrettyPrinting().create();

    /* Used by GSON to set default values */
    public Config() {
        this(configBuilder());
    }

    private Config(Builder builder) {
        urls = builder.urls;
        browser = builder.browser;
        globalWaitAfterPageLoad = builder.globalWaitAfterPageLoad;
        pageLoadTimeout = builder.pageLoadTimeout;
        windowHeight = builder.windowHeight;
        threads = builder.threads;
        screenshotRetries = builder.screenshotRetries;
        reportFormat = builder.reportFormat;
        globalTimeout = builder.globalTimeout;
        debug = builder.debug;
        logToFile = builder.logToFile;
    }

    public static Config parse(String config) {
        Config parsedConfig = gson.fromJson(config, Config.class);
        if (parsedConfig == null) {
            throw new JsonParseException(String.format("Config is not valid: '%s'", config));
        } else if (parsedConfig.urls == null) {
            throw new JsonParseException("No urls in JLineup config.");
        }
        return parsedConfig;
    }

    public static String prettyPrint(Config config) {
        return gson.toJson(config);
    }

    @Override
    public String toString() {
        return "Config{" +
                "urls=" + urls +
                ", browser=" + browser +
                ", globalWaitAfterPageLoad=" + globalWaitAfterPageLoad +
                ", pageLoadTimeout=" + pageLoadTimeout +
                ", windowHeight=" + windowHeight +
                ", reportFormat=" + reportFormat +
                ", screenshotRetries=" + screenshotRetries +
                ", threads=" + threads +
                ", globalTimeout=" + globalTimeout +
                ", debug=" + debug +
                ", logToFile=" + logToFile +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (pageLoadTimeout != config.pageLoadTimeout) return false;
        if (screenshotRetries != config.screenshotRetries) return false;
        if (threads != config.threads) return false;
        if (globalTimeout != config.globalTimeout) return false;
        if (debug != config.debug) return false;
        if (logToFile != config.logToFile) return false;
        if (urls != null ? !urls.equals(config.urls) : config.urls != null) return false;
        if (browser != config.browser) return false;
        if (globalWaitAfterPageLoad != null ? !globalWaitAfterPageLoad.equals(config.globalWaitAfterPageLoad) : config.globalWaitAfterPageLoad != null)
            return false;
        if (windowHeight != null ? !windowHeight.equals(config.windowHeight) : config.windowHeight != null)
            return false;
        return reportFormat != null ? reportFormat.equals(config.reportFormat) : config.reportFormat == null;
    }

    @Override
    public int hashCode() {
        int result = urls != null ? urls.hashCode() : 0;
        result = 31 * result + (browser != null ? browser.hashCode() : 0);
        result = 31 * result + (globalWaitAfterPageLoad != null ? globalWaitAfterPageLoad.hashCode() : 0);
        result = 31 * result + pageLoadTimeout;
        result = 31 * result + (windowHeight != null ? windowHeight.hashCode() : 0);
        result = 31 * result + (reportFormat != null ? reportFormat.hashCode() : 0);
        result = 31 * result + screenshotRetries;
        result = 31 * result + threads;
        result = 31 * result + globalTimeout;
        result = 31 * result + (debug ? 1 : 0);
        result = 31 * result + (logToFile ? 1 : 0);
        return result;
    }

    public static Config defaultConfig() {
        return defaultConfig(EXAMPLE_URL);
    }

    public static Config defaultConfig(String url) {
        return configBuilder().withUrls(ImmutableMap.of(url, new UrlConfig())).build();
    }

    public static Builder configBuilder() {
        return new Builder();
    }

    public static Config exampleConfig() {
        return configBuilder()
                .withUrls(ImmutableMap.of("http://www.example.com",
                        new UrlConfig(
                                ImmutableList.of("/","someOtherPath"),
                                DEFAULT_MAX_DIFF,
                                ImmutableList.of(
                                        new Cookie("exampleCookieName", "exampleValue", "http://www.example.com", "/", new Date(1000L), true)
                                ),
                                ImmutableMap.of("live", "www"),
                                ImmutableMap.of("exampleLocalStorageKey", "value"),
                                ImmutableMap.of("exampleSessionStorageKey", "value"),
                                ImmutableList.of(600,800,1000),
                                DEFAULT_MAX_SCROLL_HEIGHT,
                                DEFAULT_WAIT_AFTER_PAGE_LOAD,
                                DEFAULT_WAIT_AFTER_SCROLL,
                                DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL,
                                DEFAULT_WARMUP_BROWSER_CACHE_TIME,
                                "console.log('This is JavaScript!')",
                                DEFAULT_WAIT_FOR_FONTS_TIME
                        )))
                .build();
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
                    String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
                    throw new FileNotFoundException("Config file not found. Search locations were: " + Arrays.toString(searchPaths.toArray()) + " - working dir: " + cwd);
                }
            }
        }
        BufferedReader br = new BufferedReader(new FileReader(configFilePath.toString()));
        return gson.fromJson(br, Config.class);
    }

    public static final class Builder {
        private Map<String, UrlConfig> urls = null;
        private Browser.Type browser = DEFAULT_BROWSER;
        private float globalWaitAfterPageLoad = DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD;
        private int pageLoadTimeout = DEFAULT_PAGELOAD_TIMEOUT;
        private int windowHeight = DEFAULT_WINDOW_HEIGHT;
        private int reportFormat = DEFAULT_REPORT_FORMAT;
        private int screenshotRetries = DEFAULT_SCREENSHOT_RETRIES;
        private int threads = DEFAULT_THREADS;
        private int globalTimeout = DEFAULT_GLOBAL_TIMEOUT;
        private boolean debug = false;
        private boolean logToFile = false;

        private Builder() {
        }

        public Builder withUrls(Map<String, UrlConfig> val) {
            urls = val;
            return this;
        }

        public Builder withBrowser(Browser.Type val) {
            browser = val;
            return this;
        }

        public Builder withGlobalWaitAfterPageLoad(float val) {
            globalWaitAfterPageLoad = val;
            return this;
        }

        public Builder withPageLoadTimeout(int val) {
            pageLoadTimeout = val;
            return this;
        }

        public Builder withWindowHeight(int val) {
            windowHeight = val;
            return this;
        }

        public Builder withReportFormat(int val) {
            reportFormat = val;
            return this;
        }

        public Builder withScreenshotRetries(int val) {
            screenshotRetries = val;
            return this;
        }

        public Builder withThreads(int val) {
            threads = val;
            return this;
        }

        public Builder withDebug(boolean val) {
            debug = val;
            return this;
        }

        public Builder withLogToFile(boolean val) {
            logToFile = val;
            return this;
        }

        public Builder withGlobalTimeout(int val) {
            globalTimeout = val;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
