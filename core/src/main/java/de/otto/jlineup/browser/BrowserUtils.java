package de.otto.jlineup.browser;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;
import io.github.bonigarcia.wdm.PhantomJsDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.otto.jlineup.config.JobConfig.DEFAULT_PATH;

public class BrowserUtils {

    private final static Logger LOG = LoggerFactory.getLogger(BrowserUtils.class);

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
        return getWebDriverByConfig(jobConfig, runStepConfig, JobConfig.DEFAULT_WINDOW_WIDTH);
    }

    synchronized WebDriver getWebDriverByConfig(JobConfig jobConfig, RunStepConfig runStepConfig, int width) {
        WebDriver driver;
        switch (jobConfig.browser) {
            case FIREFOX:
                FirefoxDriverManager.getInstance().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.setProfile(getFirefoxProfileWithDisabledAnimatedGifs());
                firefoxOptions.addArguments(runStepConfig.getFirefoxParameters());
                LOG.debug("Creating firefox with options: {}", firefoxOptions.toString());
                driver = new FirefoxDriver(firefoxOptions);
                break;
            case FIREFOX_HEADLESS:
                FirefoxDriverManager.getInstance().forceCache().setup();
                FirefoxOptions firefoxOptionsForHeadless = new FirefoxOptions();
                //Headless parameter is supported with Firefox >= 55
                firefoxOptionsForHeadless.addArguments("--headless");
                firefoxOptionsForHeadless.addArguments("-width", width + "" , "-height", jobConfig.windowHeight + "");
                firefoxOptionsForHeadless.addArguments(runStepConfig.getFirefoxParameters());
                firefoxOptionsForHeadless.setProfile(getFirefoxProfileWithDisabledAnimatedGifs());
                LOG.debug("Creating headless firefox with options: {}", firefoxOptionsForHeadless.toString());
                driver = new FirefoxDriver(firefoxOptionsForHeadless);
                break;
            case CHROME:
                ChromeDriverManager.getInstance().setup();
                ChromeOptions options = new ChromeOptions();
                //To work in a headless env, this is needed
                options.addArguments("--no-sandbox");
                options.addArguments(runStepConfig.getChromeParameters());
                LOG.debug("Creating chrome with options: {}", options.toString());
                driver = new ChromeDriver(options);
                break;
            case CHROME_HEADLESS:
                ChromeDriverManager.getInstance().forceCache().setup();
                ChromeOptions options_headless = new ChromeOptions();
                //To work in a headless env, this is needed
                options_headless.addArguments("--no-sandbox","--headless","--disable-gpu");
                options_headless.addArguments("--window-size=" + width + "," + jobConfig.windowHeight);
                options_headless.addArguments(runStepConfig.getChromeParameters());
                LOG.debug("Creating headless chrome with options: {}", options_headless.toString());
                driver = new ChromeDriver(options_headless);
                break;
            case PHANTOMJS:
            default:
                PhantomJsDriverManager.getInstance().setup();
                driver = new PhantomJSDriver();
                break;
        }
        driver.manage().timeouts().pageLoadTimeout(jobConfig.pageLoadTimeout, TimeUnit.SECONDS);
        return driver;
    }

    private FirefoxProfile getFirefoxProfileWithDisabledAnimatedGifs() {
        FirefoxProfile firefoxProfileHeadless = new FirefoxProfile();
        firefoxProfileHeadless.setPreference("image.animation_mode", "none");
        return firefoxProfileHeadless;
    }

    static List<ScreenshotContext> buildScreenshotContextListFromConfigAndState(RunStepConfig runStepConfig, JobConfig jobConfig) throws JLineupException {
        List<ScreenshotContext> screenshotContextList = new ArrayList<>();
        Map<String, UrlConfig> urls = jobConfig.urls;

        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final UrlConfig urlConfig = urlConfigEntry.getValue();
            final List<Integer> resolutions = urlConfig.windowWidths;
            final List<String> paths = urlConfig.paths;
            for (final String path : paths) {
                screenshotContextList.addAll(
                        resolutions.stream()
                                .map(windowWidth ->
                                        new ScreenshotContext(prepareDomain(runStepConfig, urlConfigEntry.getKey()), path, windowWidth,
                                                runStepConfig.getStep() == Step.before, urlConfigEntry.getValue()))
                                .collect(Collectors.toList()));
            }
        }
        return screenshotContextList;
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