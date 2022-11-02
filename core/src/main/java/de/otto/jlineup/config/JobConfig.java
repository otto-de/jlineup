package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.Browser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.DeviceConfig.deviceConfigBuilder;
import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = JobConfig.Builder.class)
public final class JobConfig  {

    static final String LINEUP_CONFIG_DEFAULT_PATH = "./lineup.json";
    static final String EXAMPLE_URL = "https://www.example.com";

    public static final int DEFAULT_WARMUP_BROWSER_CACHE_TIME = 0;
    public static final int DEFAULT_REPORT_FORMAT = 2;

    static final Browser.Type DEFAULT_BROWSER = Browser.Type.CHROME_HEADLESS;
    static final float DEFAULT_MAX_DIFF = 0;
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    static final float DEFAULT_PIXEL_RATIO = 1.0f;
    static final float DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD = 0f;
    public static final String DEFAULT_PATH = "";
    public static final ImmutableList<String> DEFAULT_PATHS = ImmutableList.of(DEFAULT_PATH);
    static final int DEFAULT_MAX_SCROLL_HEIGHT = 100000;
    static final float DEFAULT_WAIT_AFTER_PAGE_LOAD = 0;
    static final float DEFAULT_WAIT_AFTER_SCROLL = 0;
    static final float DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL = 0;
    static final float DEFAULT_WAIT_FOR_FONTS_TIME = 0;
    public static final float DEFAULT_MAX_COLOR_DISTANCE = 2.3f;
    static final int DEFAULT_THREADS = 0; // '0' means not set which is transformed to '1' when creating the threadpool
    static final int DEFAULT_PAGELOAD_TIMEOUT = 120;
    static final int DEFAULT_SCREENSHOT_RETRIES = 0;
    static final int DEFAULT_GLOBAL_TIMEOUT = 600;
    public static final float DEFAULT_WAIT_FOR_SELECTORS_TIMEOUT = 10.0f;

    public static final HttpCheckConfig DEFAULT_HTTP_CHECK_CONFIG = new HttpCheckConfig();

    public final Map<String, UrlConfig> urls;
    public final Browser.Type browser;

    @JsonInclude(Include.NON_DEFAULT)
    public final String name;

    @JsonProperty("wait-after-page-load")
    @JsonAlias({"async-wait"})
    public final Float globalWaitAfterPageLoad;
    public final int pageLoadTimeout;
    public final Integer windowHeight;
    @JsonInclude(value = Include.CUSTOM, valueFilter = ReportFormatFilter.class)
    public final Integer reportFormat;
    @JsonInclude(Include.NON_DEFAULT)
    public final int screenshotRetries;
    @JsonInclude(Include.NON_DEFAULT)
    public final int threads;
    @JsonProperty("timeout")
    public final int globalTimeout;
    public final boolean debug;
    @JsonInclude(Include.NON_DEFAULT)
    public final boolean logToFile;
    public final boolean checkForErrorsInLog;
    @JsonInclude(value = Include.CUSTOM, valueFilter = HttpCheckFilter.class)
    public final HttpCheckConfig httpCheck;

    public JobConfig() {
        this(jobConfigBuilder());
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
        return JacksonWrapper.serializeObject(jobConfig);
    }

    public static String prettyPrintWithAllFields(JobConfig jobConfig) {
        ObjectMapper objectMapper = JsonMapper.builder()
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(ALLOW_COMMENTS, true)
                .configure(INDENT_OUTPUT, true)
                .build();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        objectMapper.addMixIn(JobConfig.class, JobConfigMixIn.class);
        objectMapper.addMixIn(UrlConfig.class, UrlConfigMixIn.class);
        try {
            return objectMapper.writeValueAsString(jobConfig);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("There is a problem while writing the " + jobConfig.getClass().getCanonicalName() + " with Jackson.", e);
        }
    }

    /*
     *
     *
     *
     *  BEGIN of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    public Map<String, UrlConfig> getUrls() {
        return urls;
    }

    public Browser.Type getBrowser() {
        return browser;
    }

    public String getName() {
        return name;
    }

    public Float getGlobalWaitAfterPageLoad() {
        return globalWaitAfterPageLoad;
    }

    public int getPageLoadTimeout() {
        return pageLoadTimeout;
    }

    public Integer getWindowHeight() {
        return windowHeight;
    }

    public Integer getReportFormat() {
        return reportFormat;
    }

    public int getScreenshotRetries() {
        return screenshotRetries;
    }

    public int getThreads() {
        return threads;
    }

    public int getGlobalTimeout() {
        return globalTimeout;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public boolean isCheckForErrorsInLog() {
        return checkForErrorsInLog;
    }

    public HttpCheckConfig getHttpCheck() {
        return httpCheck;
    }

    /*
     *
     *
     *
     *  END of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    public static Builder copyOfBuilder(JobConfig jobConfig) {
        return jobConfigBuilder()
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
        return jobConfigBuilder().withName("Default").withUrls(ImmutableMap.of(url, urlConfigBuilder().build())).build();
    }

    public static Builder jobConfigBuilder() {
        return new Builder();
    }

    public static JobConfig exampleConfig() {
        return exampleConfigBuilder().build();
    }

    public static JobConfig.Builder exampleConfigBuilder() {
        return jobConfigBuilder()
                .withName("Example")
                .withCheckForErrorsInLog(true)
                .withUrls(ImmutableMap.of("http://www.example.com",

                        urlConfigBuilder()
                                .withPaths(ImmutableList.of("/"))
                                .withCookies(ImmutableList.of(
                                        new Cookie("exampleCookieName", "exampleValue", "www.example.com", "/", new Date(1000L), false, false, false)
                                ))
                                .withEnvMapping(ImmutableMap.of("live", "www"))
                                .withLocalStorage(ImmutableMap.of("exampleLocalStorageKey", "value"))
                                .withSessionStorage(ImmutableMap.of("exampleSessionStorageKey", "value"))
                                .withDevices(ImmutableList.of(deviceConfig(850,600), deviceConfig(1000, 850), deviceConfig(1200, 1000)))
                                .withJavaScript("console.log('This is JavaScript!')")
                                .withHttpCheck(new HttpCheckConfig(true))
                                .withStrictColorComparison(false)
                                .withRemoveSelectors(ImmutableSet.of("#removeNodeWithThisId", ".removeNodesWithThisClass"))
                                .withWaitForSelectors(ImmutableSet.of("h1"))
                                .withFailIfSelectorsNotFound(false)
                                .build())
                );
    }


    public static JobConfig readConfig(final String workingDir, final String configFileName) throws IOException {
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
        return JacksonWrapper.deserializeConfig(br);
    }

    public JobConfig insertDefaults() {

        //If no urls are configured, insertDefaults can't really work, validation will fail in a later step
        if (this.urls == null) {
            return this;
        }

        Builder jobConfigBuilder = copyOfBuilder(this).withUrls(ImmutableMap.of());
        this.urls.forEach((url, urlConfig) -> {

            UrlConfig.Builder urlConfigBuilder = UrlConfig.copyOfBuilder(urlConfig);

            if (urlConfig.url == null) {
                urlConfigBuilder.withUrl(url);
            }

            //If both are not set, use default window width
            List<Integer> windowWidths = urlConfig.windowWidths;
            if (windowWidths == null && urlConfig.devices == null) {
                windowWidths = ImmutableList.of(DEFAULT_WINDOW_WIDTH);
            }

            int windowHeight = this.windowHeight != null ? this.windowHeight : DEFAULT_WINDOW_HEIGHT;

            final List<DeviceConfig> deviceConfigs;
            if (urlConfig.devices == null) {
                deviceConfigs = new ArrayList<>();
                windowWidths.forEach(width -> deviceConfigs.add(deviceConfigBuilder().withWidth(width).withHeight(windowHeight).build()));
            } else {
                deviceConfigs = urlConfig.devices;
            }
            urlConfigBuilder.withDevices(deviceConfigs);
            //Remove window widths because devices were generated above
            urlConfigBuilder.withWindowWidths(null);

            final List<String> paths = urlConfig.paths != null ? urlConfig.paths : DEFAULT_PATHS;
            urlConfigBuilder.withPaths(paths);

            jobConfigBuilder.addUrlConfig(url, urlConfigBuilder.build());
        });

        //Every url gets device config
        jobConfigBuilder.withWindowHeight(null);

        return jobConfigBuilder.build();
    }

    public static final class Builder {
        private String name = null;
        private Map<String, UrlConfig> urls = null;
        private Browser.Type browser = DEFAULT_BROWSER;
        private float globalWaitAfterPageLoad = DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD;
        private int pageLoadTimeout = DEFAULT_PAGELOAD_TIMEOUT;
        private Integer windowHeight = null;
        private int reportFormat = DEFAULT_REPORT_FORMAT;
        private int screenshotRetries = DEFAULT_SCREENSHOT_RETRIES;
        private int threads = DEFAULT_THREADS;
        private int globalTimeout = DEFAULT_GLOBAL_TIMEOUT;
        private boolean debug = false;
        private boolean logToFile = false;
        private boolean checkForErrorsInLog = false;
        private HttpCheckConfig httpCheck = DEFAULT_HTTP_CHECK_CONFIG;

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

        @JsonProperty("wait-after-page-load")
        @JsonAlias({"async-wait"})
        public Builder withGlobalWaitAfterPageLoad(float val) {
            globalWaitAfterPageLoad = val;
            return this;
        }

        public Builder withPageLoadTimeout(int val) {
            pageLoadTimeout = val;
            return this;
        }

        public Builder withWindowHeight(Integer val) {
            windowHeight = val;
            return this;
        }

        @JsonInclude(value = Include.CUSTOM, valueFilter = ReportFormatFilter.class)
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

        @JsonProperty("timeout")
        public Builder withGlobalTimeout(int val) {
            globalTimeout = val;
            return this;
        }

        public Builder withCheckForErrorsInLog(boolean val) {
            checkForErrorsInLog = val;
            return this;
        }

        @JsonInclude(value = Include.CUSTOM, valueFilter = HttpCheckFilter.class)
        public Builder withHttpCheck(HttpCheckConfig val) {
            httpCheck = val;
            return this;
        }

        public JobConfig build() {
            return new JobConfig(this);
        }

        public Builder addUrlConfig(String url, UrlConfig urlConfig) {
            if (urls == null) {
                urls = ImmutableMap.of(url, urlConfig);
            }
            else {
                urls = ImmutableMap.<String, UrlConfig>builder().putAll(urls).put(url, urlConfig).build();
            }
            return this;
        }
    }
}
