package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
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
import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.DeviceConfig.deviceConfigBuilder;
import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = JobConfig.Builder.class)
public final class JobConfig  {

    static final String LINEUP_CONFIG_DEFAULT_PATH = "./lineup.json";
    static final List<String> LINEUP_CONFIG_DEFAULT_PATHS = List.of("./lineup.yaml", "./lineup.yml", "./lineup.json");
    static final String EXAMPLE_URL = "https://www.example.com";

    public static final int DEFAULT_WARMUP_BROWSER_CACHE_TIME = 0;
    static final Browser.Type DEFAULT_BROWSER = Browser.Type.CHROME_HEADLESS;
    static final float DEFAULT_MAX_DIFF = 0;
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    static final float DEFAULT_PIXEL_RATIO = 1.0f;
    static final float DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD = 0f;
    public static final String DEFAULT_PATH = "";
    public static final ImmutableList<String> DEFAULT_PATHS = ImmutableList.of(DEFAULT_PATH);
    public static final ImmutableList<PathConfig> DEFAULT_PATH_CONFIGS = ImmutableList.of(PathConfig.of(DEFAULT_PATH));
    static final int DEFAULT_MAX_SCROLL_HEIGHT = 100000;
    static final float DEFAULT_WAIT_AFTER_PAGE_LOAD = 0;
    static final float DEFAULT_WAIT_AFTER_SCROLL = 0;
    public static final float DEFAULT_SCROLL_DISTANCE_FACTOR = 1.0f;
    static final float DEFAULT_WAIT_FOR_NO_ANIMATION_AFTER_SCROLL = 0;
    static final float DEFAULT_WAIT_FOR_FONTS_TIME = 0;
    public static final double DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE = 2.3d;
    public static final double DEFAULT_MAX_COLOR_DISTANCE = 2.3d;
    static final int DEFAULT_THREADS = 0; // '0' means not set which is transformed to '1' when creating the threadpool
    static final int DEFAULT_PAGELOAD_TIMEOUT = 120;
    static final int DEFAULT_SCREENSHOT_RETRIES = 0;
    static final int DEFAULT_GLOBAL_TIMEOUT = 1800;
    public static final float DEFAULT_WAIT_FOR_SELECTORS_TIMEOUT = 10.0f;
    public static final int DEFAULT_FLAKY_TOLERANCE = 0;

    public static final HttpCheckConfig DEFAULT_HTTP_CHECK_CONFIG = new HttpCheckConfig();

    public final Map<String, UrlConfig> urls;
    public final Browser.Type browser;
    @JsonInclude(Include.NON_NULL)
    public final List<Browser.Type> browsers;

    @JsonInclude(Include.NON_DEFAULT)
    public final String name;

    @JsonInclude(Include.NON_DEFAULT)
    public final String message;

    @JsonInclude(Include.NON_DEFAULT)
    public final String approvalLink;

    @JsonProperty("wait-after-page-load")
    @JsonAlias({"async-wait"})
    public final Float globalWaitAfterPageLoad;
    public final int pageLoadTimeout;
    public final Integer windowHeight;
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

    @JsonInclude(Include.NON_DEFAULT)
    public final int flakyTolerance;

    @JsonInclude(Include.NON_DEFAULT)
    public final JobConfig mergeConfig;

    public JobConfig() {
        this(jobConfigBuilder());
    }

    private JobConfig(Builder builder) {
        name = builder.name;
        message = builder.message;
        approvalLink = builder.approvalLink;
        urls = builder.urls;
        // Resolve browsers vs browser: browsers wins if set, otherwise derive from browser
        if (builder.browsers != null && !builder.browsers.isEmpty()) {
            List<Browser.Type> browsersList = ImmutableList.copyOf(builder.browsers);
            this.browsers = browsersList;
            this.browser = browsersList.get(0);
        } else {
            this.browser = builder.browser;
            this.browsers = null; // null means single browser mode (backwards compat — won't serialize)
        }
        globalWaitAfterPageLoad = builder.globalWaitAfterPageLoad;
        pageLoadTimeout = builder.pageLoadTimeout;
        windowHeight = builder.windowHeight;
        threads = builder.threads;
        screenshotRetries = builder.screenshotRetries;
        globalTimeout = builder.globalTimeout;
        debug = builder.debug;
        logToFile = builder.logToFile;
        checkForErrorsInLog = builder.checkForErrorsInLog;
        httpCheck = builder.httpCheck;
        flakyTolerance = builder.flakyTolerance;
        mergeConfig = builder.mergeConfig;
    }

    public static String prettyPrint(JobConfig jobConfig) {
        return JacksonWrapper.serializeObject(jobConfig);
    }

    public static String prettyPrint(JobConfig jobConfig, JacksonWrapper.ConfigFormat format) {
        return JacksonWrapper.serializeObject(jobConfig, format);
    }

    public static String prettyPrintWithAllFields(JobConfig jobConfig) {
        return prettyPrintWithAllFields(jobConfig, JacksonWrapper.ConfigFormat.JSON);
    }

    public static String prettyPrintWithAllFields(JobConfig jobConfig, JacksonWrapper.ConfigFormat format) {
        try {
            if (format == JacksonWrapper.ConfigFormat.YAML) {
                tools.jackson.dataformat.yaml.YAMLMapper yamlMapper = JacksonWrapper.yamlMapper().rebuild()
                        .addMixIn(JobConfig.class, JobConfigMixIn.class)
                        .addMixIn(UrlConfig.class, UrlConfigMixIn.class)
                        .build();
                return yamlMapper.writeValueAsString(jobConfig);
            }
            JsonMapper jsonMapper = JsonMapper.builder()
                    .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
                    .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                    .configure(ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                    .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
                    .configure(JsonReadFeature.ALLOW_YAML_COMMENTS, true)
                    .configure(INDENT_OUTPUT, true)
                    .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                    .addMixIn(JobConfig.class, JobConfigMixIn.class)
                    .addMixIn(UrlConfig.class, UrlConfigMixIn.class)
                    .build();
            return jsonMapper.writeValueAsString(jobConfig);
        } catch (Exception e) {
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

    public List<Browser.Type> getBrowsers() {
        return browsers;
    }

    /**
     * Returns the effective list of browsers for this config.
     * If browsers (plural) is set, returns it. Otherwise returns a singleton list of browser.
     */
    @JsonIgnore
    public List<Browser.Type> getEffectiveBrowsers() {
        if (browsers != null && !browsers.isEmpty()) {
            return browsers;
        }
        return ImmutableList.of(browser);
    }

    /**
     * Resolves the effective browsers for a specific (url, device) combination using the cascade:
     * device-level overrides site-level overrides global-level.
     * If a device uses mobile emulation (isMobile()), Chrome is forced regardless.
     */
    @JsonIgnore
    public List<Browser.Type> resolveEffectiveBrowsers(UrlConfig urlConfig, DeviceConfig deviceConfig) {
        // Mobile emulation forces Chrome
        if (deviceConfig != null && deviceConfig.isMobile()) {
            return ImmutableList.of(Browser.Type.CHROME_HEADLESS);
        }
        // Device-level overrides site-level
        if (deviceConfig != null && deviceConfig.browsers != null && !deviceConfig.browsers.isEmpty()) {
            return deviceConfig.browsers;
        }
        // Site-level overrides global
        if (urlConfig != null && urlConfig.browsers != null && !urlConfig.browsers.isEmpty()) {
            return urlConfig.browsers;
        }
        // Global
        return getEffectiveBrowsers();
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    @JsonIgnore
    public String getSanitizedMessage() {
        return message != null ? message.replace("\\n", "\n") : null;
    }

    public String getApprovalLink() {
        return approvalLink;
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

    public int getFlakyTolerance() {
        return flakyTolerance;
    }

    public JobConfig getMergeConfig() {
        return mergeConfig;
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
                .withMessage(jobConfig.message)
                .withApprovalLink(jobConfig.approvalLink)
                .withUrls(jobConfig.urls)
                .withHttpCheck(jobConfig.httpCheck)
                .withBrowser(jobConfig.browser)
                .withBrowsers(jobConfig.browsers)
                .withGlobalWaitAfterPageLoad(jobConfig.globalWaitAfterPageLoad)
                .withPageLoadTimeout(jobConfig.pageLoadTimeout)
                .withWindowHeight(jobConfig.windowHeight)
                .withThreads(jobConfig.threads)
                .withScreenshotRetries(jobConfig.screenshotRetries)
                .withGlobalTimeout(jobConfig.globalTimeout)
                .withDebug(jobConfig.debug)
                .withLogToFile(jobConfig.logToFile)
                .withMergeConfig(jobConfig.mergeConfig)
                .withCheckForErrorsInLog(jobConfig.checkForErrorsInLog)
                .withFlakyTolerance(jobConfig.flakyTolerance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobConfig jobConfig = (JobConfig) o;
        return pageLoadTimeout == jobConfig.pageLoadTimeout && screenshotRetries == jobConfig.screenshotRetries && threads == jobConfig.threads && globalTimeout == jobConfig.globalTimeout && flakyTolerance == jobConfig.flakyTolerance && debug == jobConfig.debug && logToFile == jobConfig.logToFile && checkForErrorsInLog == jobConfig.checkForErrorsInLog && Objects.equals(urls, jobConfig.urls) && browser == jobConfig.browser && Objects.equals(browsers, jobConfig.browsers) && Objects.equals(name, jobConfig.name) && Objects.equals(message, jobConfig.message) && Objects.equals(approvalLink, jobConfig.approvalLink) && Objects.equals(globalWaitAfterPageLoad, jobConfig.globalWaitAfterPageLoad) && Objects.equals(windowHeight, jobConfig.windowHeight) && Objects.equals(httpCheck, jobConfig.httpCheck) && Objects.equals(mergeConfig, jobConfig.mergeConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urls, browser, browsers, name, message, approvalLink, globalWaitAfterPageLoad, pageLoadTimeout, windowHeight, screenshotRetries, threads, globalTimeout, flakyTolerance, debug, logToFile, checkForErrorsInLog, httpCheck, mergeConfig);
    }

    @Override
    public String toString() {
        return "JobConfig{" +
                "urls=" + urls +
                ", browser=" + browser +
                ", browsers=" + browsers +
                ", name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", approvalLink='" + approvalLink + '\'' +
                ", globalWaitAfterPageLoad=" + globalWaitAfterPageLoad +
                ", pageLoadTimeout=" + pageLoadTimeout +
                ", windowHeight=" + windowHeight +
                ", screenshotRetries=" + screenshotRetries +
                ", threads=" + threads +
                ", globalTimeout=" + globalTimeout +
                ", debug=" + debug +
                ", logToFile=" + logToFile +
                ", checkForErrorsInLog=" + checkForErrorsInLog +
                ", httpCheck=" + httpCheck +
                ", flakyTolerance=" + flakyTolerance +
                ", mergeConfig=" + mergeConfig +
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
        return exampleConfigBuilder().build().insertDefaults();
    }

    public static JobConfig.Builder exampleConfigBuilder() {
        return jobConfigBuilder()
                .withName("Example")
                .withMessage("This is an example message, which will be shown in the report.")
                .withCheckForErrorsInLog(true)
                .withUrls(ImmutableMap.of("https://www.example.com",

                        urlConfigBuilder()
                                .withPaths(ImmutableList.of(PathConfig.of("/")))
                                .withCookies(ImmutableList.of(
                                        new Cookie("exampleCookieName", "exampleValue", "www.example.com", "/", new Date(1000L), false, false, false)
                                ))
                                .withEnvMapping(ImmutableMap.of("live", "www"))
                                .withLocalStorage(ImmutableMap.of("exampleLocalStorageKey", "value"))
                                .withSessionStorage(ImmutableMap.of("exampleSessionStorageKey", "value"))
                                .withDevices(ImmutableList.of(deviceConfig(850,600), deviceConfig(1000, 850), deviceConfig(1200, 1000)))
                                .withJavaScript("console.log('This is JavaScript!')")
                                .withStyle(".some-selector { display: none !important; }")
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
                // Search default paths: lineup.yaml > lineup.yml > lineup.json
                configFilePath = null;
                for (String defaultPath : LINEUP_CONFIG_DEFAULT_PATHS) {
                    Path candidate = Paths.get(defaultPath);
                    searchPaths.add(candidate.toString());
                    if (Files.exists(candidate)) {
                        configFilePath = candidate;
                        break;
                    }
                }
                if (configFilePath == null) {
                    String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
                    throw new FileNotFoundException("JobConfig file not found. Search locations were: " + Arrays.toString(searchPaths.toArray()) + " - working dir: " + cwd);
                }
            }
        }
        JacksonWrapper.ConfigFormat format = JacksonWrapper.ConfigFormat.fromFilename(configFilePath.toString());
        BufferedReader br = new BufferedReader(new FileReader(configFilePath.toString()));
        return JacksonWrapper.deserializeConfig(br, format);
    }

    /**
     * This function actively checks for unset values that need to be replaced with defaults
     * @return a new JobConfig including the former values with defaults added
     */
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

            final List<PathConfig> paths = urlConfig.paths != null ? urlConfig.paths : DEFAULT_PATH_CONFIGS;
            urlConfigBuilder.withPaths(paths);

            jobConfigBuilder.addUrlConfig(url, urlConfigBuilder.build());
        });

        //Every url gets device config
        jobConfigBuilder.withWindowHeight(null);

        return jobConfigBuilder.build();
    }

    public static final class Builder {
        private String name = null;
        private String message = null;
        private String approvalLink = null;
        private Map<String, UrlConfig> urls = null;
        private Browser.Type browser = DEFAULT_BROWSER;
        private List<Browser.Type> browsers = null;
        private float globalWaitAfterPageLoad = DEFAULT_GLOBAL_WAIT_AFTER_PAGE_LOAD;
        private int pageLoadTimeout = DEFAULT_PAGELOAD_TIMEOUT;
        private Integer windowHeight = null;
        private int screenshotRetries = DEFAULT_SCREENSHOT_RETRIES;
        private int threads = DEFAULT_THREADS;
        private int globalTimeout = DEFAULT_GLOBAL_TIMEOUT;
        private boolean debug = false;
        private boolean logToFile = false;
        private boolean checkForErrorsInLog = false;
        private HttpCheckConfig httpCheck = DEFAULT_HTTP_CHECK_CONFIG;
        private int flakyTolerance = DEFAULT_FLAKY_TOLERANCE;
        public JobConfig mergeConfig;

        private Builder() {
        }

        public Builder withName(String val) {
            name = val;
            return this;
        }

        public Builder withMessage(String val) {
            message = val;
            return this;
        }

        public Builder withApprovalLink(String val) {
            approvalLink = val;
            return this;
        }

        //Use custom deserializer to allow a very basic config looking like this: "urls: https://www.otto.de"
        @JsonProperty("urls")
        @JsonDeserialize(using = UrlsDeserializer.class)
        public Builder withUrls(Map<String, UrlConfig> val) {
            urls = val;
            return this;
        }

        public Builder withBrowser(Browser.Type val) {
            browser = val;
            return this;
        }

        public Builder withBrowsers(List<Browser.Type> val) {
            browsers = val;
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

        public Builder withFlakyTolerance(int val) {
            flakyTolerance = val;
            return this;
        }

        public Builder withMergeConfig(JobConfig val) {
            mergeConfig = val;
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

    /**
     * This removes all cookie values from the config (used to write the config to the report)
     * @return a sanitized JobConfig
     */
    public JobConfig sanitize() {
        return JobConfig.copyOfBuilder(this)
                .withUrls(this.urls.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().sanitize())))
                .build();
    }

    public static class UrlsDeserializer extends StdDeserializer<Map<String, UrlConfig>> {

        public UrlsDeserializer() {
            super(Map.class);
        }

        @Override
        public Map<String, UrlConfig> deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                // "urls: https://www.otto.de" variant
                return Map.of(p.getText(), urlConfigBuilder().build());
            }
            // normal object variant
            return ctxt.readValue(p, new TypeReference<Map<String, UrlConfig>>() {});
        }
    }
}
