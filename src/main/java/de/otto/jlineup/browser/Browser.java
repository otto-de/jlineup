package de.otto.jlineup.browser;

import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.image.ImageUtils;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.MarionetteDriverManager;
import io.github.bonigarcia.wdm.PhantomJsDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Browser {


    public enum Type {
        @SerializedName(value = "Firefox", alternate = {"firefox", "FIREFOX"})
        FIREFOX,
        @SerializedName(value = "Chrome", alternate = {"chrome", "CHROME"})
        CHROME,
        @SerializedName(value = "PhantomJS", alternate = {"phantomjs", "PHANTOMJS"})
        PHANTOMJS;
    }

    public static final String JS_DOCUMENT_HEIGHT_CALL = "return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);";
    public static final String JS_CLIENT_VIEWPORT_HEIGHT_CALL = "return document.documentElement.clientHeight";
    public static final String JS_SET_LOCAL_STORAGE_CALL = "localStorage.setItem('%s','%s')";
    public static final String JS_SCROLL_CALL = "window.scrollBy(0,%d)";

    final private Parameters parameters;
    final private Config config;
    final private WebDriver driver;

    public Browser(Parameters parameters, Config config, WebDriver driver) {
        this.parameters = parameters;
        this.config = config;
        this.driver = driver;
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    public void browseAndTakeScreenshots() throws IOException, InterruptedException {
        takeScreenshots(config, !parameters.isAfter());
    }

    public void close() {
        if (driver != null) {
            driver.close();
            driver.quit();
        }
    }

    private void takeScreenshots(Config config, boolean before) throws IOException, InterruptedException {
        List<ScreenshotContext> screenshotContextList = generateScreenshotsParametersFromConfig(config, before);
        takeScreenshots(screenshotContextList);
    }

    static List<ScreenshotContext> generateScreenshotsParametersFromConfig(Config config, boolean before) {
        List<ScreenshotContext> screenshotContextList = new ArrayList<>();
        Map<String, UrlConfig> urls = config.getUrls();
        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final UrlConfig urlConfig = urlConfigEntry.getValue();
            final List<Integer> resolutions = urlConfig.windowWidths;
            final List<String> paths = urlConfig.paths;
            for (final String path : paths) {
                screenshotContextList.addAll(
                        resolutions.stream().map(windowWidth -> new ScreenshotContext(urlConfigEntry.getKey(), path, windowWidth, before)).collect(Collectors.toList()));
            }
        }
        return screenshotContextList;
    }

    public static WebDriver getWebDriverByConfig(Config config) {
        WebDriver driver;
        switch (config.getBrowser()) {
            case FIREFOX:
                MarionetteDriverManager.getInstance().setup();
                driver = new MarionetteDriver();
                break;
            case CHROME:
                ChromeDriverManager.getInstance().setup();
                driver = new ChromeDriver();
                break;
            case PHANTOMJS:
            default:
                PhantomJsDriverManager.getInstance().setup();
                driver = new PhantomJSDriver();
                break;
        }
        return driver;
    }

    void takeScreenshots(final List<ScreenshotContext> screenshotContextList) throws IOException, InterruptedException {

        for (final ScreenshotContext screenshotContext : screenshotContextList) {

            driver.manage().window().setPosition(new Point(0, 0));
            driver.manage().window().setSize(new Dimension(screenshotContext.windowWidth, config.getWindowHeight()));

            UrlConfig configForCurrentUrl = config.getUrls().get(screenshotContext.url);

            driver.get(buildUrl(screenshotContext.url, "/"));

            setCookies(configForCurrentUrl.cookies);

            driver.get(buildUrl(screenshotContext.url, screenshotContext.path, configForCurrentUrl.envMapping));

            JavascriptExecutor jse = (JavascriptExecutor) driver;

            Map<String, String> localStorage = configForCurrentUrl.localStorage;
            setLocalStorage(jse, localStorage);

            Long pageHeight = (Long) (jse.executeScript(JS_DOCUMENT_HEIGHT_CALL));
            Long viewportHeight = (Long) (jse.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL));

            Thread.sleep(Math.round(config.getAsyncWait() * 1000));
            for (int yPosition = 0; yPosition < pageHeight; yPosition += viewportHeight) {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                final BufferedImage image = ImageIO.read(screenshot);
                ImageIO.write(image, "png", new File(getFullScreenshotFileNameWithPath(screenshotContext.url, screenshotContext.path, screenshotContext.windowWidth, yPosition, screenshotContext.before ? "before" : "after")));
                if (!screenshotContext.before) {
                    generateDifferenceImage(screenshotContext.url, screenshotContext.path, screenshotContext.windowWidth, yPosition, viewportHeight.intValue());
                }
                //PhantomJS (until now) always makes full page screenshots, so no scrolling and multi-screenshooting
                //This is subject to change because W3C standard wants viewport screenshots
                if (config.getBrowser() == Type.PHANTOMJS) break;
                scrollBy(jse, viewportHeight.intValue());
                Thread.sleep(50);
            }
        }
    }

    void scrollBy(JavascriptExecutor jse, int viewportHeight) {
        jse.executeScript(String.format(JS_SCROLL_CALL, viewportHeight));
    }

    void setLocalStorage(JavascriptExecutor jse, Map<String, String> localStorage) {
        if (localStorage == null) return;
        for (Map.Entry<String, String> localStorageEntry : localStorage.entrySet()) {

            final String entry = localStorageEntry.getValue().replace("'", "\"");

            String jsCall = String.format(JS_SET_LOCAL_STORAGE_CALL, localStorageEntry.getKey(), entry);
            jse.executeScript(jsCall);
        }
    }

    void setCookies(List<Cookie> cookies) {
        if (cookies == null) return;
        for (Cookie cookie : cookies) {
            org.openqa.selenium.Cookie.Builder cookieBuilder = new org.openqa.selenium.Cookie.Builder(cookie.name, cookie.value);
            if (cookie.domain != null) cookieBuilder.domain(cookie.domain);
            if (cookie.path != null) cookieBuilder.path(cookie.path);
            if (cookie.secure) cookieBuilder.isSecure(cookie.secure);
            driver.manage().addCookie(cookieBuilder.build());
        }
    }

    static String buildUrl(String url, String path, final Map<String, String> envMapping) {
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
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url + path;
    }

    /**
     * @param url            the url
     * @param path           the path that is appended to the url
     * @param width          the window width
     * @param yPosition      the current vertical scroll position
     * @param viewportHeight is needed to calculate the difference level
     * @return a double between 0 and 1 that measures the difference between the two pictures. 1 means 100% difference,
     * 0 means, that both pictures are identical
     * @throws IOException
     */
    double generateDifferenceImage(String url, String path, int width, int yPosition, int viewportHeight) throws IOException {

        BufferedImage imageBefore;
        final String before = "before";
        final String fullFileNameWithPath = getFullScreenshotFileNameWithPath(url, path, width, yPosition, before);
        try {
            imageBefore = ImageIO.read(new File(fullFileNameWithPath));
        } catch (IIOException e) {
            if (yPosition == 0) {
                System.err.println("Cannot read 'before' screenshot (" + fullFileNameWithPath + "). Did you run jlineup with parameter --before before you tried to run it with --after?");
                throw e;
            } else {
                //There is a difference in the amount of vertical screenshots, this means the page's vertical size changed
                return 1;
            }
        }
        final String after = "after";
        BufferedImage imageAfter = ImageIO.read(new File(getFullScreenshotFileNameWithPath(url, path, width, yPosition, after)));
        final ImageUtils.BufferedImageComparisonResult differenceImage = ImageUtils.getDifferenceImage(imageBefore, imageAfter, viewportHeight);
        ImageIO.write(differenceImage.differenceImage, "png", new File(getFullScreenshotFileNameWithPath(url, path, width, yPosition, "DIFFERENCE")));
        return differenceImage.difference;
    }

    String getFullScreenshotFileNameWithPath(String url, String path, int width, int yPosition, String step) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + generateScreenshotFileName(url, path, width, yPosition, step);
    }

    static String generateScreenshotFileName(String url, String path, int width, int yPosition, String type) {

        if (path.equals("/") || path.equals("")) {
            path = "root";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String fileName = url + "_" + path + "_" + width + "_" + yPosition + "_" + type;
        fileName = fileName.replace("http://", "");
        fileName = fileName.replace("https://", "");
        fileName = fileName.replace("/", "_");
        fileName = fileName.replace("..", "");
        fileName = fileName.replace(".", "_");
        fileName = fileName + ".png";

        return fileName;
    }


}
