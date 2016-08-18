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
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Browser {

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


    public void browseAndTakeScreenshots(final Config config, final boolean before) throws IOException, InterruptedException {
        WebDriver driver = getWebDriverByConfig(config);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        takeScreenshots(driver, config, before);
    }

    private void takeScreenshots(WebDriver driver, Config config, boolean before) throws IOException, InterruptedException {
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
            takeScreenshots(driver, config, url, path, resolution, before);
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

    private void takeScreenshots(WebDriver driver, Config config, String url, String path, int width, boolean before) throws IOException, InterruptedException {

        driver.manage().window().setPosition(new Point(0, 0));
        driver.manage().window().setSize(new Dimension(width, config.getWindowHeight()));

        driver.get(buildUrl(url, path));

        JavascriptExecutor jse = (JavascriptExecutor)driver;
        Long pageHeight = (Long)(jse.executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);"));
        Long viewportHeight = (Long)(jse.executeScript("return document.documentElement.clientHeight"));

        Thread.sleep(Math.round(config.getAsyncWait() * 1000));
        for (int yPosition = 0; yPosition < pageHeight; yPosition += viewportHeight) {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            final BufferedImage image = ImageIO.read(screenshot);
            ImageIO.write(image, "png", new File(getFullScreenshotFileNameWithPath(url, path, width, yPosition, before ? "before" : "after")));
            if (!before) {
                generateDifferenceImage(url, path, width, yPosition);
            }
            jse.executeScript("window.scrollBy(0," + viewportHeight + ")", "");
            Thread.sleep(50);
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

    void generateDifferenceImage(String url, String path, int width, int yPosition) throws IOException {

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
                return;
            }
        }
        final String after = "after";
        BufferedImage imageAfter = ImageIO.read(new File(getFullScreenshotFileNameWithPath(url, path, width, yPosition, after)));
        ImageIO.write(ImageUtils.getDifferenceImage(imageBefore, imageAfter), "png", new File(getFullScreenshotFileNameWithPath(url, path, width, yPosition, "DIFFERENCE")));
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
