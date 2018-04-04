package de.otto.jlineup.browser;

import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Config;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BrowserUtils {

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
            path = "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url + path;
    }

    synchronized WebDriver getWebDriverByConfig(Config config) {
        return getWebDriverByConfig(config, Config.DEFAULT_WINDOW_WIDTH);
    }

    synchronized WebDriver getWebDriverByConfig(Config config, int width) {
        WebDriver driver;
        switch (config.browser) {
            case FIREFOX:
                FirefoxDriverManager.getInstance().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.setProfile(getFirefoxProfileWithDisabledAnimatedGifs());
                driver = new FirefoxDriver(firefoxOptions);
                break;
            case FIREFOX_HEADLESS:
                FirefoxDriverManager.getInstance().setup();
                FirefoxOptions firefoxOptionsForHeadless = new FirefoxOptions();
                //Headless parameter is supported with Firefox >= 55
                firefoxOptionsForHeadless.addArguments("--headless");
                firefoxOptionsForHeadless.addArguments("-width", width + "" , "-height", config.windowHeight + "");
                firefoxOptionsForHeadless.setProfile(getFirefoxProfileWithDisabledAnimatedGifs());
                driver = new FirefoxDriver(firefoxOptionsForHeadless);
                break;
            case CHROME:
                ChromeDriverManager.getInstance().setup();
                ChromeOptions options = new ChromeOptions();
                //To work in a headless env, this is needed
                options.addArguments("--no-sandbox");
                driver = new ChromeDriver(options);
                break;
            case CHROME_HEADLESS:
                ChromeDriverManager.getInstance().setup();
                ChromeOptions options_headless = new ChromeOptions();
                //To work in a headless env, this is needed
                options_headless.addArguments("--no-sandbox","--headless","--disable-gpu");
                options_headless.addArguments("--window-size=" + width + "," + config.windowHeight);
                driver = new ChromeDriver(options_headless);
                break;
            case PHANTOMJS:
            default:
                PhantomJsDriverManager.getInstance().setup();
                driver = new PhantomJSDriver();
                break;
        }
        driver.manage().timeouts().pageLoadTimeout(config.pageLoadTimeout, TimeUnit.SECONDS);
        return driver;
    }

    private FirefoxProfile getFirefoxProfileWithDisabledAnimatedGifs() {
        FirefoxProfile firefoxProfileHeadless = new FirefoxProfile();
        firefoxProfileHeadless.setPreference("image.animation_mode", "none");
        return firefoxProfileHeadless;
    }

    static List<ScreenshotContext> buildScreenshotContextListFromConfigAndState(JLineupRunConfiguration jLineupRunConfiguration, Config config) throws JLineupException {
        List<ScreenshotContext> screenshotContextList = new ArrayList<>();
        Map<String, UrlConfig> urls = config.urls;

        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final UrlConfig urlConfig = urlConfigEntry.getValue();
            final List<Integer> resolutions = urlConfig.windowWidths;
            final List<String> paths = urlConfig.paths;
            for (final String path : paths) {
                screenshotContextList.addAll(
                        resolutions.stream()
                                .map(windowWidth ->
                                        new ScreenshotContext(prepareDomain(jLineupRunConfiguration, urlConfigEntry.getKey()), path, windowWidth,
                                                jLineupRunConfiguration.getStep() == Step.before, urlConfigEntry.getValue()))
                                .collect(Collectors.toList()));
            }
        }
        return screenshotContextList;
    }

    public static String prepareDomain(final JLineupRunConfiguration jLineupRunConfiguration, final String url) {
        String processedUrl = url;
        for (Map.Entry<String, String> replacement : jLineupRunConfiguration.getUrlReplacements().entrySet()) {
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