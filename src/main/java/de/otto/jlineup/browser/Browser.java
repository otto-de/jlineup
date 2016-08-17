package de.otto.jlineup.browser;

import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import de.otto.jlineup.config.Config;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Browser {

    public static final int WINDOW_HEIGHT = 800;

    public enum Type {
        @SerializedName(value = "Firefox", alternate = {"firefox", "FIREFOX"})
        FIREFOX,
        @SerializedName(value = "Chrome", alternate = {"chrome", "CHROME"})
        CHROME,
        @SerializedName(value = "PhantomJS", alternate = {"phantomjs", "PHANTOMJS"})
        PHANTOMJS
    }

    private Parameters parameters;

    @Inject
    public Browser(Parameters parameters) {
        this.parameters = parameters;
    }


    public void browseAndTakeScreenshots(Config config, boolean before) throws IOException, InterruptedException {

        WebDriver driver = getWebDriverByConfig(config);

        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        takeScreenshots(config, before, driver);
        if (!before) {
            generateDifferenceImages(config);
        }
    }

    private void generateDifferenceImages(Config config) throws IOException {

        Map<String, UrlConfig> urls = config.getUrls();
        for (String url : urls.keySet()) {
            generateDifferenceImagesForUrl(config, url);
        }
    }


    private void generateDifferenceImagesForUrl(Config config, String url) throws IOException {
        UrlConfig urlConfig = config.getUrls().get(url);
        List<Integer> resolutions = urlConfig.resolutions;
        List<String> paths = urlConfig.paths;
        for (String path : paths) {
            generateDifferenceImagesForPath(config, url, resolutions, path);
        }
    }

    private void generateDifferenceImagesForPath(Config config, String url, List<Integer> resolutions, String path) throws IOException {
        for (Integer resolution : resolutions) {
            generateDifferenceImage(url, path, resolution);
        }
    }

    private void takeScreenshots(Config config, boolean before, WebDriver driver) throws IOException, InterruptedException {
        try {
            Map<String, UrlConfig> urls = config.getUrls();
            for (String url : urls.keySet()) {
                takeScreenshotsForUrl(config, before, driver, urls, url);
            }
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    private void takeScreenshotsForUrl(Config config, boolean before, WebDriver driver, Map<String, UrlConfig> urls, String url) throws IOException, InterruptedException {
        UrlConfig urlConfig = urls.get(url);
        List<Integer> resolutions = urlConfig.resolutions;
        List<String> paths = urlConfig.paths;

        for (String path : paths) {
            takeScreenshotsForPath(config, before, driver, url, resolutions, path);
        }
    }

    private void takeScreenshotsForPath(Config config, boolean before, WebDriver driver, String url, List<Integer> resolutions, String path) throws IOException, InterruptedException {
        for (Integer resolution : resolutions) {
            takeSingleScreenshot(driver, config, url, path, resolution, before);
            if (!before) {
                generateDifferenceImage(url, path, resolution);
            }
        }
    }

    static WebDriver getWebDriverByConfig(Config config) {
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

    private void takeSingleScreenshot(WebDriver driver, Config config, String url, String path, int width, boolean before) throws IOException, InterruptedException {

        driver.manage().window().setPosition(new Point(0, 0));
        driver.manage().window().setSize(new Dimension(width, config.getWindowHeight()));

        driver.get(buildUrl(url, path));

        JavascriptExecutor jse = (JavascriptExecutor)driver;
        Long pageHeight = (Long)(jse.executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);"));
        Long viewportHeight = (Long)(jse.executeScript("return document.documentElement.clientHeight"));

        Thread.sleep(Math.round(config.getAsyncWait() * 1000));
        for (long yPosition = 0; yPosition < pageHeight; yPosition += viewportHeight) {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            final BufferedImage image = ImageIO.read(screenshot);
            ImageIO.write(image, "png", new File(getFullFileNameWithPath(url, path, width, yPosition + (before ? "_before" : "_after"))));
            jse.executeScript("window.scrollBy(0," + viewportHeight + ")", "");
            Thread.sleep(500);
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

    void generateDifferenceImage(String url, String path, int width) throws IOException {

        BufferedImage imageBefore;
        final String before = "before";
        final String fullFileNameWithPath = getFullFileNameWithPath(url, path, width, before);
        try {
            imageBefore = ImageIO.read(new File(fullFileNameWithPath));
        } catch (IIOException e) {
            System.err.println("Cannot read 'before' screenshot (" + fullFileNameWithPath + "). Please run jlineup with parameter --before before you try to run it with --after.");
            throw e;
        }
        final String after = "after";
        BufferedImage imageAfter = ImageIO.read(new File(getFullFileNameWithPath(url, path, width, after)));
        ImageIO.write(ImageUtils.getDifferenceImage(imageBefore, imageAfter), "png", new File(getFullFileNameWithPath(url, path, width, "DIFFERENCE")));
    }

    String getFullFileNameWithPath(String url, String path, int width, String step) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/") + generateFileName(url, path, width, step);
    }

    static String generateFileName(String url, String path, int width, String type) {

        if (path.equals("/") || path.equals("")) {
            path = "root";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String fileName = url + "_" + path + "_" + width + "_" + type;
        fileName = fileName.replace("http://", "");
        fileName = fileName.replace("https://", "");
        fileName = fileName.replace("/", "_");
        fileName = fileName.replace("..", "");
        fileName = fileName.replace(".", "_");
        fileName = fileName + ".png";
        return fileName;
    }


}
