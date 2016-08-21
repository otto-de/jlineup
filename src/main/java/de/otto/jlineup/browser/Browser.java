package de.otto.jlineup.browser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.config.*;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.image.ImageUtils;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.MarionetteDriverManager;
import io.github.bonigarcia.wdm.PhantomJsDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Browser {

    public enum Type {
        @SerializedName(value = "Firefox", alternate = {"firefox", "FIREFOX"})
        FIREFOX,
        @SerializedName(value = "Chrome", alternate = {"chrome", "CHROME"})
        CHROME,
        @SerializedName(value = "PhantomJS", alternate = {"phantomjs", "PHANTOMJS"})
        PHANTOMJS
    }

    final private Parameters parameters;
    final private Config config;
    final private WebDriver driver;

    public Browser(Parameters parameters, Config config, WebDriver driver) {
        this.parameters = parameters;
        this.config = config;
        this.driver = driver;
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    final static class ScreenshotParameters {
        final String url;
        final String path;
        final int windowWidth;
        final boolean before;
        private ScreenshotParameters(String url, String path, int windowWidth, boolean before) {
            this.url = url;
            this.path = path;
            this.windowWidth = windowWidth;
            this.before = before;
        }

        public static ScreenshotParameters of(String url, String path, int windowWidth, boolean before) {
            return new ScreenshotParameters(url, path, windowWidth, before);
        }

        @Override
        public String toString() {
            return "ScreenshotParameters{" +
                    "url='" + url + '\'' +
                    ", path='" + path + '\'' +
                    ", windowWidth=" + windowWidth +
                    ", before=" + before +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScreenshotParameters that = (ScreenshotParameters) o;
            return windowWidth == that.windowWidth &&
                    before == that.before &&
                    Objects.equals(url, that.url) &&
                    Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, path, windowWidth, before);
        }
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
        List<ScreenshotParameters> screenshotParametersList = generateScreenshotsParametersFromConfig(config, before);
        takeScreenshots(screenshotParametersList);
    }

    static List<ScreenshotParameters> generateScreenshotsParametersFromConfig(Config config, boolean before) {
        List<ScreenshotParameters> screenshotParametersList = new ArrayList<>();
        Map<String, UrlConfig> urls = config.getUrls();
        for (final Map.Entry<String, UrlConfig> urlConfigEntry : urls.entrySet()) {
            final UrlConfig urlConfig = urlConfigEntry.getValue();
            final List<Integer> resolutions = urlConfig.windowWidths;
            final List<String> paths = urlConfig.paths;
            for (final String path : paths) {
                screenshotParametersList.addAll(
                        resolutions.stream().map(windowWidth -> new ScreenshotParameters(urlConfigEntry.getKey(), path, windowWidth, before)).collect(Collectors.toList()));
            }
        }
        return screenshotParametersList;
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

    private void takeScreenshots(final List<ScreenshotParameters> parameters) throws IOException, InterruptedException {

        for (final ScreenshotParameters parameter : parameters) {

            driver.manage().window().setPosition(new Point(0, 0));
            driver.manage().window().setSize(new Dimension(parameter.windowWidth, config.getWindowHeight()));

            UrlConfig configForCurrentUrl = config.getUrls().get(parameter.url);
            List<Cookie> cookies = configForCurrentUrl.cookies;

            driver.get(buildUrl(parameter.url, parameter.path));
            for (Cookie cookie : cookies) {
                org.openqa.selenium.Cookie.Builder cookieBuilder = new org.openqa.selenium.Cookie.Builder(cookie.name, cookie.value);
                if (cookie.domain != null) cookieBuilder.domain(cookie.domain);
                if (cookie.path != null) cookieBuilder.path(cookie.path);
                if (cookie.secure) cookieBuilder.isSecure(cookie.secure);
                driver.manage().addCookie(cookieBuilder.build());
            }

            JavascriptExecutor jse = (JavascriptExecutor) driver;

            Map<String, String> localStorage = configForCurrentUrl.localStorage;
            for (Map.Entry<String, String> localStorageEntry : localStorage.entrySet()) {
                String jsCall = "localStorage.setItem('" + localStorageEntry.getKey() + "','" + localStorageEntry.getValue() + "')";
                jse.executeScript(jsCall);
            }

            Long pageHeight = (Long) (jse.executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);"));
            Long viewportHeight = (Long) (jse.executeScript("return document.documentElement.clientHeight"));

            Thread.sleep(Math.round(config.getAsyncWait() * 1000));
            for (int yPosition = 0; yPosition < pageHeight; yPosition += viewportHeight) {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                final BufferedImage image = ImageIO.read(screenshot);
                ImageIO.write(image, "png", new File(getFullScreenshotFileNameWithPath(parameter.url, parameter.path, parameter.windowWidth, yPosition, parameter.before ? "before" : "after")));
                if (!parameter.before) {
                    generateDifferenceImage(parameter.url, parameter.path, parameter.windowWidth, yPosition, viewportHeight.intValue());
                }
                //PhantomJS always makes full page screenshots, so no scrolling and multi-screenshooting
                if (config.getBrowser() == Type.PHANTOMJS) break;
                jse.executeScript("window.scrollBy(0," + viewportHeight + ")", "");
                Thread.sleep(50);
            }
        }
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
     *
     * @param url the url
     * @param path the path that is appended to the url
     * @param width the window width
     * @param yPosition the current vertical scroll position
     * @param viewportHeight is needed to calculate the difference level
     * @return a double between 0 and 1 that measures the difference between the two pictures. 1 means 100% difference,
     *         0 means, that both pictures are identical
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
