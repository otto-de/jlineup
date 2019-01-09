package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
public final class JobConfig {

    static final String LINEUP_CONFIG_DEFAULT_PATH = "./lineup.json";
    static final String EXAMPLE_URL = "https://www.example.com";

    public static final int DEFAULT_WARMUP_BROWSER_CACHE_TIME = 0;
    public static final int DEFAULT_REPORT_FORMAT = 2;

    static final Browser.Type DEFAULT_BROWSER = Browser.Type.PHANTOMJS;
    static final float DEFAULT_MAX_DIFF = 0;
    static final int DEFAULT_WINDOW_HEIGHT = 800;
    static final float DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD = 0f;
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final String DEFAULT_PATH = "";
    static final int DEFAULT_MAX_SCROLL_HEIGHT = 100000;
    static final float DEFAULT_WAIT_AFTER_PAGE_LOAD = 0;
    static final float DEFAULT_WAIT_AFTER_SCROLL = 0;
    static final float DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL = 0;
    static final float DEFAULT_WAIT_FOR_FONTS_TIME = 0;
    static final int DEFAULT_THREADS = 0; // '0' means not set which is transformed to '1' when creating the threadpool
    static final int DEFAULT_PAGELOAD_TIMEOUT = 120;
    static final int DEFAULT_SCREENSHOT_RETRIES = 0;
    static final int DEFAULT_GLOBAL_TIMEOUT = 600;
    static final int DEFAULT_MAX_COLOR_DIFF_PER_PIXEL = 1;

    public final Map<String, UrlConfig> urls;
    public final Browser.Type browser;

    @SerializedName(value = "name")
    @JsonProperty("name")
    public final String name;

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
    public final int threads;
    @SerializedName("timeout")
    @JsonProperty("timeout")
    public final int globalTimeout;
    @SerializedName("debug")
    @JsonProperty("debug")
    public final boolean debug;
    @SerializedName("log-to-file")
    @JsonProperty("log-to-file")
    public final boolean logToFile;
    @SerializedName("check-for-errors-in-log")
    @JsonProperty("check-for-errors-in-log")
    public final boolean checkForErrorsInLog;
    @SerializedName("http-check")
    @JsonProperty("http-check")
    public final HttpCheckConfig httpCheck;

    private final static Gson gson =
            new GsonBuilder()
                    .setDateFormat(COOKIE_TIME_FORMAT)
                    .setPrettyPrinting()
                    .create();

    /* Used by GSON to set default values */
    public JobConfig() {
        this(configBuilder());
    }

    private JobConfig(Builder builder) {
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
        checkForErrorsInLog = builder.checkForErrorsInLog;
        httpCheck = builder.httpCheck;
        name = builder.name;
    }

    public static String prettyPrint(JobConfig jobConfig) {
        return gson.toJson(jobConfig);
    }

    public static Builder copyOfBuilder(JobConfig jobConfig) {
        return configBuilder()
                .withName(jobConfig.name)
                .withUrls(jobConfig.urls)
                .withHttpCheck(jobConfig.httpCheck)
                .withBrowser(jobConfig.browser)
                .withGlobalWaitAfterPageLoad(jobConfig.globalWaitAfterPageLoad)
                .withPageLoadTimeout(jobConfig.pageLoadTimeout)
                .withWindowHeight(jobConfig.windowHeight)
                .withThreads(jobConfig.threads)
                .withScreenshotRetries(jobConfig.screenshotRetries)
                .withReportFormat(jobConfig.reportFormat)
                .withGlobalTimeout(jobConfig.globalTimeout)
                .withDebug(jobConfig.debug)
                .withLogToFile(jobConfig.logToFile)
                .withCheckForErrorsInLog(jobConfig.checkForErrorsInLog);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobConfig jobConfig = (JobConfig) o;
        return pageLoadTimeout == jobConfig.pageLoadTimeout &&
                screenshotRetries == jobConfig.screenshotRetries &&
                threads == jobConfig.threads &&
                globalTimeout == jobConfig.globalTimeout &&
                debug == jobConfig.debug &&
                logToFile == jobConfig.logToFile &&
                checkForErrorsInLog == jobConfig.checkForErrorsInLog &&
                Objects.equals(urls, jobConfig.urls) &&
                browser == jobConfig.browser &&
                Objects.equals(name, jobConfig.name) &&
                Objects.equals(globalWaitAfterPageLoad, jobConfig.globalWaitAfterPageLoad) &&
                Objects.equals(windowHeight, jobConfig.windowHeight) &&
                Objects.equals(reportFormat, jobConfig.reportFormat) &&
                Objects.equals(httpCheck, jobConfig.httpCheck);
    }

    @Override
    public int hashCode() {

        return Objects.hash(urls, browser, name, globalWaitAfterPageLoad, pageLoadTimeout, windowHeight, reportFormat, screenshotRetries, threads, globalTimeout, debug, logToFile, checkForErrorsInLog, httpCheck);
    }

    @Override
    public String toString() {
        return "JobConfig{" +
                "urls=" + urls +
                ", browser=" + browser +
                ", name='" + name + '\'' +
                ", globalWaitAfterPageLoad=" + globalWaitAfterPageLoad +
                ", pageLoadTimeout=" + pageLoadTimeout +
                ", windowHeight=" + windowHeight +
                ", reportFormat=" + reportFormat +
                ", screenshotRetries=" + screenshotRetries +
                ", threads=" + threads +
                ", globalTimeout=" + globalTimeout +
                ", debug=" + debug +
                ", logToFile=" + logToFile +
                ", checkForErrorsInLog=" + checkForErrorsInLog +
                ", httpCheck=" + httpCheck +
                '}';
    }

    public static JobConfig defaultConfig() {
        return defaultConfig(EXAMPLE_URL);
    }

    public static JobConfig defaultConfig(String url) {
        return configBuilder().withUrls(ImmutableMap.of(url, new UrlConfig())).build();
    }

    public static Builder configBuilder() {
        return new Builder();
    }

    public static JobConfig exampleConfig() {
        return exampleConfigBuilder().build();
    }

    public static JobConfig.Builder exampleConfigBuilder() {
        return configBuilder()
                .withUrls(ImmutableMap.of("http://www.example.com",
                        new UrlConfig(
                                ImmutableList.of("/", "someOtherPath"),
                                DEFAULT_MAX_DIFF,
                                ImmutableList.of(
                                        new Cookie("exampleCookieName", "exampleValue", "http://www.example.com", "/", new Date(1000L), true)
                                ),
                                ImmutableMap.of("live", "www"),
                                ImmutableMap.of("exampleLocalStorageKey", "value"),
                                ImmutableMap.of("exampleSessionStorageKey", "value"),
                                ImmutableList.of(600, 800, 1000),
                                DEFAULT_MAX_SCROLL_HEIGHT,
                                DEFAULT_WAIT_AFTER_PAGE_LOAD,
                                DEFAULT_WAIT_AFTER_SCROLL,
                                DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL,
                                DEFAULT_WARMUP_BROWSER_CACHE_TIME,
                                "console.log('This is JavaScript!')",
                                DEFAULT_WAIT_FOR_FONTS_TIME,
                                new HttpCheckConfig(true),
                                DEFAULT_MAX_COLOR_DIFF_PER_PIXEL,
                                false
                        )));
    }


    public static JobConfig readConfig(final String workingDir, final String configFileName) throws FileNotFoundException {
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
                    throw new FileNotFoundException("JobConfig file not found. Search locations were: " + Arrays.toString(searchPaths.toArray()) + " - working dir: " + cwd);
                }
            }
        }
        BufferedReader br = new BufferedReader(new FileReader(configFilePath.toString()));
        return gson.fromJson(br, JobConfig.class);
    }

    public static final class Builder {
        private String name = null;
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
        private boolean checkForErrorsInLog = true;
        public HttpCheckConfig httpCheck = new HttpCheckConfig();

        private Builder() {
        }

        public Builder withName(String val) {
            name = val;
            return this;
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

        public Builder withCheckForErrorsInLog(boolean val) {
            checkForErrorsInLog = val;
            return this;
        }

        public Builder withHttpCheck(HttpCheckConfig val) {
            httpCheck = val;
            return this;
        }

        public JobConfig build() {
            return new JobConfig(this);
        }
    }
}
