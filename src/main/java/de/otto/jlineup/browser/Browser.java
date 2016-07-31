package de.otto.jlineup.browser;

import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.image.ImageUtils;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.MarionetteDriverManager;
import io.github.bonigarcia.wdm.PhantomJsDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

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

    public static void browseAndTakeScreenshots(Config config, boolean before) throws IOException {

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

        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        Map<String, UrlConfig> urls = config.urls;
        for (String url : urls.keySet()) {
            UrlConfig urlConfig = urls.get(url);
            List<Integer> resolutions = urlConfig.resolutions;
            List<String> paths = urlConfig.paths;

            for (String path : paths) {
                for (Integer resolution : resolutions) {
                    takeSingleScreenshot(driver, config, url, path, resolution, before);
                }
            }
        }

        driver.close();
        driver.quit();

    }

    private static void takeSingleScreenshot(WebDriver driver, Config config, String url, String path, int width, boolean before) throws IOException {

        if (!url.endsWith("/")) {
            url = url + "/";
        }

        driver.get(url + path);
        driver.manage().window().setPosition(new Point(0, 0));

        Long height = (Long) ((JavascriptExecutor) driver).executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);");
        driver.manage().window().setSize(new Dimension(width, height.intValue()));
        System.out.println(driver.getPageSource());
        System.out.println(driver.getTitle());
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        final BufferedImage image = ImageIO.read(screenshot);

        ImageIO.write(image, "png", new File(config.workingDir + "/" + generateFileName(url, path, width, before)));
    }

    private static void generateDifferenceImage(Config config, String url, String path, int width) throws IOException {

        BufferedImage imageBefore = ImageIO.read(new File(config.workingDir + "/" + generateFileName(url, path, width, true)));
        BufferedImage imageAfter = ImageIO.read(new File(config.workingDir + "/" + generateFileName(url, path, width, false)));

        ImageIO.write(ImageUtils.getDifferenceImage(imageBefore, imageAfter), "png", new File("build/diff.png"));
    }

    static String generateFileName(String url, String path, int width, boolean before) {

        if (path.equals("/") || path.equals("")) {
            path = "root";
        }

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String fileName = url + "_" + path + "_" + width + "_" + (before ? "before" : "after");

        fileName = fileName.replace("http://", "");
        fileName = fileName.replace("https://", "");
        fileName = fileName.replace("/", "_");
        fileName = fileName.replace("..", "");
        fileName = fileName.replace(".", "_");

        fileName = fileName + ".png";

        return fileName;
    }


}
