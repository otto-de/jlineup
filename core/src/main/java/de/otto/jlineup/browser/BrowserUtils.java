package de.otto.jlineup.browser;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.otto.jlineup.config.DeviceConfig.deviceConfigBuilder;
import static de.otto.jlineup.config.JobConfig.DEFAULT_PATH;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.SECONDS;

public class BrowserUtils {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private static final boolean CHROME_DETERMINISTIC_OPTIONS = true;

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
                options.setHeadless(true);
                options.addArguments("-width", device.width + "", "-height", device.height + "");
            }
            LOG.debug("Creating firefox with options: {}", options.toString());
            WebDriverManager webDriverManager = WebDriverManager.firefoxdriver().capabilities(options);
            if (runStepConfig.getWebDriverCachePath() != null) {
                webDriverManager.cachePath(runStepConfig.getWebDriverCachePath());
            }


            driver = webDriverManager.create();
        } else if (jobConfig.browser.isChrome()) {
            ChromeOptions options = new ChromeOptions();
            //To work in a headless env, this is needed
            options.addArguments("--no-sandbox");
            options.addArguments("--whitelisted-ips");
            options.addArguments(runStepConfig.getChromeParameters());

            //These options my help to convince Chrome to render deterministically
            //This is important for the pixel-perfect comparison of before and after steps
            //There were problems to render Webfonts, SVGs and progressive JPGs deterministically on huge pages (i.e. otto.de)
            //Beware of dragons
            if (CHROME_DETERMINISTIC_OPTIONS) {
                options.addArguments("--disable-threaded-animation");
                //options.addArguments("--disable-threaded-compositing"); // This option breaks rendering completely as of Chrome 70 (2019-01-07)
                options.addArguments("--disable-threaded-scrolling");
                options.addArguments("--num-raster-threads=1");
                options.addArguments("--disable-histogram-customizer");
                options.addArguments("--disable-composited-antialiasing");
                options.addArguments("--deterministic-mode");
                options.addArguments("--disable-checker-imaging");
                options.addArguments("--run-all-compositor-stages-before-draw");
                options.addArguments("--enable-surface-synchronization");
            }

            if (device.isMobile()) {
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
                options.setExperimentalOption("mobileEmulation", mobileEmulation);
            } else if (device.userAgent != null) {
                options.addArguments("--user-agent='" + device.userAgent + "'");
            }

            if (jobConfig.browser.isHeadless()) {
                options.addArguments("--headless=new");
                options.addArguments("--window-size=" + device.width + "," + device.height);
            }

            LOG.debug("Creating chrome with options: {}", options);
            WebDriverManager webDriverManager = WebDriverManager.chromedriver().capabilities(options);
            if (runStepConfig.getWebDriverCachePath() != null) {
                webDriverManager.cachePath(runStepConfig.getWebDriverCachePath());
            }
            driver = webDriverManager.create();
        } else if (jobConfig.browser.isChromium()) {
            driver = WebDriverManager.chromiumdriver().create();
        } else if (jobConfig.browser.isSafari()) {
            driver = WebDriverManager.safaridriver().create();
        } else {
            LOG.error("You need either Firefox or Chrome / Chromium to make JLineup work. Install one of them and try again.");
            throw new RuntimeException("You need either Firefox or Chrome / Chromium to make JLineup work. Install one of them and try again.");
        }

        if (driver == null || driver.manage() == null) {
            LOG.error("Browser could not be started or it crashed. :( Something went wrong.");
            throw new RuntimeException("Browser could not be started or it crashed. :(");
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.of(jobConfig.pageLoadTimeout, SECONDS));
        return driver;
    }

    private FirefoxProfile getFirefoxProfileWithDisabledAnimatedGifs() {
        FirefoxProfile firefoxProfileHeadless = new FirefoxProfile();
        firefoxProfileHeadless.setPreference("image.animation_mode", "none");
        return firefoxProfileHeadless;
    }

    public static List<ScreenshotContext> buildScreenshotContextListFromConfigAndState(RunStepConfig runStepConfig, JobConfig jobConfig) {
        List<ScreenshotContext> screenshotContextList = new ArrayList<>();
        Map<String, UrlConfig> urls = jobConfig.urls;

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
                                    .collect(Collectors.toList()));
                } else {
                    screenshotContextList.addAll(
                            urlConfig.devices.stream()
                                    .flatMap(deviceConfig ->
                                            urlConfig.alternatingCookies.stream().map(alternatingCookies -> {
                                                ArrayList<Cookie> newCookies = urlConfig.cookies != null ? new ArrayList<>(urlConfig.cookies) : new ArrayList<>();
                                                newCookies.addAll(alternatingCookies);
                                                return new ScreenshotContext(prepareDomain(runStepConfig, urlConfigEntry.getValue().url), path, deviceConfig,
                                                        newCookies, runStepConfig.getBrowserStep(), urlConfig, getFullPathOfReportDir(runStepConfig), dontShareBrowser.get(), urlConfigEntry.getKey());
                                            })).collect(Collectors.toList()));
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
        final Map<String, UrlConfig> urls = jobConfig.urls;
        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final List<String> paths = setupPhase ? urlConfigEntry.getValue().setupPaths : urlConfigEntry.getValue().cleanupPaths;
            for (final String path : paths) {
                //TODO: alternatingCookies
                screenshotContextList.add(new ScreenshotContext(prepareDomain(runStepConfig, urlConfigEntry.getValue().url), path, deviceConfigBuilder().build(),
                        Collections.emptyList(), runStepConfig.getBrowserStep(), urlConfigEntry.getValue(), getFullPathOfReportDir(runStepConfig), false, urlConfigEntry.getKey()));
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