package de.otto.jlineup.browser;

import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.Main;
import de.otto.jlineup.config.Config;
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

    public enum Type {
        @SerializedName(value = "Firefox", alternate = {"firefox", "FIREFOX"})
        FIREFOX,
        @SerializedName(value = "Chrome", alternate = {"chrome", "CHROME"})
        CHROME,
        @SerializedName(value = "PhantomJS", alternate = {"phantomjs", "PHANTOMJS"})
        PHANTOMJS
    }

    public static void browseAndTakeScreenshots(Config config, boolean before) throws IOException, InterruptedException {

        WebDriver driver = getWebDriverByConfig(config);
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        try {

            Map<String, UrlConfig> urls = config.urls;
            for (String url : urls.keySet()) {
                UrlConfig urlConfig = urls.get(url);
                List<Integer> resolutions = urlConfig.resolutions;
                List<String> paths = urlConfig.paths;

                for (String path : paths) {
                    for (Integer resolution : resolutions) {
                        takeSingleScreenshot(driver, config, url, path, resolution, before);
                        if (!before) {
                            Browser.generateDifferenceImage(config, url, path, resolution);
                        }
                    }
                }
            }
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    static WebDriver getWebDriverByConfig(Config config) {
        WebDriver driver;
        switch (config.browser) {
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

    private static void takeSingleScreenshot(WebDriver driver, Config config, String url, String path, int width, boolean before) throws IOException, InterruptedException {

        driver.manage().window().setPosition(new Point(0, 0));
        driver.manage().window().setSize(new Dimension(width, 800));

        driver.get(buildUrl(url, path));
        Long height = (Long) ((JavascriptExecutor) driver).executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);");
        driver.manage().window().setSize(new Dimension(width, height.intValue()));

        Thread.sleep(Math.round(config.asyncWait * 1000));

        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        final BufferedImage image = ImageIO.read(screenshot);

        ImageIO.write(image, "png", new File(Main.getWorkingDirectory() + "/" + generateFileName(url, path, width, before ? "before" : "after")));
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

    static void generateDifferenceImage(Config config, String url, String path, int width) throws IOException {

        BufferedImage imageBefore = null;
        try {
             imageBefore = ImageIO.read(new File(Main.getWorkingDirectory() + "/" + generateFileName(url, path, width, "before")));
        } catch (IIOException e) {
            System.err.println("Cannot read 'before' screenshot. Please run jlineup with parameter --before before you try to run it with --after.");
            throw e;
        }
        BufferedImage imageAfter = ImageIO.read(new File(Main.getWorkingDirectory() + "/" + generateFileName(url, path, width, "after")));

        ImageIO.write(ImageUtils.getDifferenceImage(imageBefore, imageAfter), "png", new File(Main.getWorkingDirectory() + "/" + generateFileName(url, path, width, "DIFFERENCE")));
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
