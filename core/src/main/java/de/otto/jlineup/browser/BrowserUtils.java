package de.otto.jlineup.browser;

import de.otto.jlineup.GlobalOption;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.otto.jlineup.JLineupRunner.LOGFILE_NAME;
import static de.otto.jlineup.config.DeviceConfig.deviceConfigBuilder;
import static de.otto.jlineup.config.JobConfig.DEFAULT_PATH;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.SECONDS;

public class BrowserUtils {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    //These options, hidden behind this flag, try to make Chrome render deterministically
    //See this bug ticket also: https://issues.chromium.org/issues/40039960
    private static final boolean CHROME_DETERMINISTIC_OPTIONS = true;
    public static final String RANDOM_FOLDER_PLACEHOLDER = "{random-folder}";

    public static String buildUrl(String url, String path, final Map<String, String> envMapping) {
        if (envMapping != null && !envMapping.isEmpty()) {
            for (Map.Entry<String, String> envMappingEntry : envMapping.entrySet()) {
                final String fromEnvironment = envMappingEntry.getKey();
                final String toEnvironment = envMappingEntry.getValue();
                url = url.replace("https://" + fromEnvironment + ".", "https://" + toEnvironment + ".");
                url = url.replace("http://" + fromEnvironment + ".", "http://" + toEnvironment + ".");
                url = url.replace("." + fromEnvironment + ".", "." + toEnvironment + ".");
            }
        }
        return buildUrl(url, path);
    }

    static String buildUrl(String url, String path) {
        if (path == null) {
            path = DEFAULT_PATH;
        }
        //TODO: Why?
        if (!url.endsWith("/") && !path.equals("") && !path.startsWith("/")) {
            url = url + "/";
        }
        if (url.endsWith("/") && path.startsWith("/")) {
            path = path.substring(1);
        }
        return url + path;
    }

    synchronized WebDriver getWebDriverByConfig(JobConfig jobConfig, RunStepConfig runStepConfig) {
        return getWebDriverByConfig(jobConfig, runStepConfig, new DeviceConfig());
    }

    synchronized WebDriver getWebDriverByConfig(JobConfig jobConfig, RunStepConfig runStepConfig, DeviceConfig device) {
        WebDriver driver;
        if (jobConfig.browser.isFirefox()) {
            FirefoxOptions options = new FirefoxOptions();
            FirefoxProfile firefoxProfileWithDisabledAnimatedGifs = getFirefoxProfileWithDisabledAnimatedGifs();
            options.setProfile(firefoxProfileWithDisabledAnimatedGifs);
            options.addArguments(runStepConfig.getFirefoxParameters());
            if (jobConfig.browser.isHeadless()) {
                options.addArguments("-headless", "-width", device.width + "", "-height", device.height + "");
            }

            String firefoxVersionOverride = GlobalOptions.getOption(GlobalOption.JLINEUP_FIREFOX_VERSION);
            if (firefoxVersionOverride != null) {
                options.setBrowserVersion(firefoxVersionOverride);
            }

            LOG.debug("Creating firefox with options: {}", options.toString());
            driver = new FirefoxDriver(options);
        } else if (jobConfig.browser.isChrome()) {
            ChromeOptions options = new ChromeOptions();
            //To work in a headless env, this is needed
            options.addArguments("--no-sandbox");
            options.addArguments("--whitelisted-ips");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-search-engine-choice-screen");

            //If there are multiple chrome drivers started with the same user profile dir, chrome will crash,
            //so the JLineupRunnerFactory adds a {random-folder} string to the profile-dir name which is replaced
            //with a UUID here to don't have two drivers running in the same user profile directory.
            options.addArguments(runStepConfig.getChromeParameters().stream()
                    .map(param -> param.replace(RANDOM_FOLDER_PLACEHOLDER, UUID.randomUUID().toString()))
                    .collect(Collectors.toList()));

            //These options my help to convince Chrome to render deterministically
            //This is important for the pixel-perfect comparison of before and after steps
            //There were problems to render Webfonts, SVGs and progressive JPGs deterministically on huge pages (i.e. otto.de)
            //Beware of dragons
            if (CHROME_DETERMINISTIC_OPTIONS) {
                //options.addArguments("--disable-threaded-compositing"); // This option breaks rendering completely as of Chrome 70 (2019-01-07)
                options.addArguments("--deterministic-mode");
                options.addArguments("--disable-checker-imaging");
                options.addArguments("--disable-composited-antialiasing");
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-gpu-rasterization");
                options.addArguments("--disable-histogram-customizer");
                options.addArguments("--disable-partial-raster");
                options.addArguments("--disable-skia-runtime-opts");
                options.addArguments("--disable-smooth-scrolling");
                options.addArguments("--disable-threaded-animation");
                options.addArguments("--disable-threaded-scrolling");
                options.addArguments("--enable-surface-synchronization");
                options.addArguments("--force-color-profile=srgb");
                options.addArguments("--num-raster-threads=1");
                options.addArguments("--run-all-compositor-stages-before-draw");
                options.addArguments("--override-use-software-gl-for-tests");
            }

            if (device.isMobile()) {
                Map<String, Object> mobileEmulation = getMobileEmulationPropertiesForChrome(device);
                options.setExperimentalOption("mobileEmulation", mobileEmulation);
            } else if (device.userAgent != null) {
                options.addArguments("--user-agent='" + device.userAgent + "'");
            }

            if (jobConfig.browser.isHeadless()) {
                options.addArguments("--headless=new");
                options.addArguments("--window-size=" + device.width + "," + device.height);
            }

            String chromeVersionOverride = GlobalOptions.getOption(GlobalOption.JLINEUP_CHROME_VERSION);
            if (chromeVersionOverride != null) {
                options.setBrowserVersion(chromeVersionOverride);
            }

            LOG.debug("Creating chrome with options: {}", options);
            driver = new ChromeDriver(options);
        } else if (jobConfig.browser.isChromium()) {
            driver = new ChromeDriver();
        } else if (jobConfig.browser.isSafari()) {
            driver = new SafariDriver();
        } else {
            LOG.error("You need either Firefox or Chrome / Chromium to make JLineup work. Install one of them and try again.");
            throw new RuntimeException("You need either Firefox or Chrome / Chromium to make JLineup work. Install one of them and try again.");
        }

        if (driver.manage() == null) {
            LOG.error("Browser could not be started or it crashed. :( Something went wrong.");
            throw new RuntimeException("Browser could not be started or it crashed. :(");
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.of(jobConfig.pageLoadTimeout, SECONDS));
        return driver;
    }

    private static Map<String, Object> getMobileEmulationPropertiesForChrome(DeviceConfig device) {
        Map<String, Object> mobileEmulation = new HashMap<>();
        if (!device.deviceName.equalsIgnoreCase("MOBILE")) {
            mobileEmulation.put("deviceName", device.deviceName);
        } else {
            Map<String, Object> deviceMetrics = new HashMap<>();
            deviceMetrics.put("width", device.width);
            deviceMetrics.put("height", device.height);
            deviceMetrics.put("pixelRatio", device.pixelRatio);
            deviceMetrics.put("touch", true);
            mobileEmulation.put("deviceMetrics", deviceMetrics);
            if (device.userAgent != null) mobileEmulation.put("userAgent", device.userAgent);
        }
        return mobileEmulation;
    }

    private FirefoxProfile getFirefoxProfileWithDisabledAnimatedGifs() {
        FirefoxProfile firefoxProfileHeadless = new FirefoxProfile();
        firefoxProfileHeadless.setPreference("image.animation_mode", "none");
        return firefoxProfileHeadless;
    }

    public static List<ScreenshotContext> buildScreenshotContextListFromConfigAndState(RunStepConfig runStepConfig, JobConfig jobConfig) {
        List<ScreenshotContext> screenshotContextList = new ArrayList<>();
        Map<String, UrlConfig> urls = jobConfig.urls == null ? Collections.emptyMap() : jobConfig.urls;

        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final UrlConfig urlConfig = urlConfigEntry.getValue();

            AtomicBoolean dontShareBrowser = new AtomicBoolean(false);
            urlConfig.devices.forEach(config -> {
                if (config.isMobile()) {
                    dontShareBrowser.set(true);
                }
            });

            for (final String path : urlConfig.paths) {
                //In most cases, there are no alternatingCookies
                //TODO: Look for a more beautiful distinction
                if (urlConfig.alternatingCookies.isEmpty()) {
                    screenshotContextList.addAll(
                            urlConfig.devices.stream()
                                    .map(deviceConfig ->
                                            new ScreenshotContext(prepareDomain(runStepConfig, urlConfig.url), path, deviceConfig,
                                                    urlConfig.cookies, runStepConfig.getBrowserStep(), urlConfig, getFullPathOfReportDir(runStepConfig), dontShareBrowser.get(), urlConfigEntry.getKey()))
                                    .toList());
                } else {
                    screenshotContextList.addAll(
                            urlConfig.devices.stream()
                                    .flatMap(deviceConfig ->
                                            urlConfig.alternatingCookies.stream().map(alternatingCookies -> {
                                                ArrayList<Cookie> newCookies = urlConfig.cookies != null ? new ArrayList<>(urlConfig.cookies) : new ArrayList<>();
                                                newCookies.addAll(alternatingCookies);
                                                return new ScreenshotContext(prepareDomain(runStepConfig, urlConfigEntry.getValue().url), path, deviceConfig,
                                                        newCookies, runStepConfig.getBrowserStep(), urlConfig, getFullPathOfReportDir(runStepConfig), dontShareBrowser.get(), urlConfigEntry.getKey());
                                            })).toList());
                }
            }
        }
        return screenshotContextList;
    }

    public static List<ScreenshotContext> buildTestSetupContexts(RunStepConfig runStepConfig, JobConfig jobConfig) {
        return buildTestContexts(runStepConfig, jobConfig, true);
    }

    public static List<ScreenshotContext> buildTestCleanupContexts(RunStepConfig runStepConfig, JobConfig jobConfig) {
        return buildTestContexts(runStepConfig, jobConfig, false);
    }

    private static List<ScreenshotContext> buildTestContexts(RunStepConfig runStepConfig, JobConfig jobConfig, boolean setupPhase) {
        final List<ScreenshotContext> screenshotContextList = new ArrayList<>();
        final Map<String, UrlConfig> urls = jobConfig.urls == null ? Collections.emptyMap() : jobConfig.urls;
        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final List<String> paths = setupPhase ? urlConfigEntry.getValue().setupPaths : urlConfigEntry.getValue().cleanupPaths;
            for (final String path : paths) {
                //TODO: alternatingCookies
                screenshotContextList.add(new ScreenshotContext(prepareDomain(runStepConfig, urlConfigEntry.getValue().url), path, deviceConfigBuilder().build(),
                        urlConfigEntry.getValue().cookies, runStepConfig.getBrowserStep(), urlConfigEntry.getValue(), getFullPathOfReportDir(runStepConfig), false, urlConfigEntry.getKey()));
            }
        }
        return screenshotContextList;
    }

    public static String getFullPathOfReportDir(RunStepConfig runStepConfig) {
        if (runStepConfig.getReportDirectory() == null) {
            return null;
        }
        return Paths.get(runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory()).toAbsolutePath().toString();
    }

    public static String getFullPathToLogFile(RunStepConfig runStepConfig) {
        if (runStepConfig.getReportDirectory() == null) {
            return null;
        }
        return Paths.get(Objects.requireNonNull(getFullPathOfReportDir(runStepConfig)), LOGFILE_NAME).toAbsolutePath().toString();
    }

    public static String prepareDomain(final RunStepConfig runStepConfig, final String url) {
        String processedUrl = url;
        for (Map.Entry<String, String> replacement : runStepConfig.getUrlReplacements().entrySet()) {
            processedUrl = processedUrl.replace(replacement.getKey(), replacement.getValue());
        }
        return processedUrl;
    }

    public static String prependHTTPIfNotThereAndToLowerCase(String url) {
        String ret = url.toLowerCase();
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("ftp://")) {
            ret = "http://" + ret;
        }
        return ret;
    }
}