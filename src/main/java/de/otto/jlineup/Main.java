package de.otto.jlineup;

import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.MarionetteDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.*;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {


        ChromeDriverManager.getInstance().setup();
        MarionetteDriverManager.getInstance().setup();

        //System.out.println("Hallo!");
        //System.setProperty("java.net.preferIPv4Stack" , "true");
        //System.setProperty("webdriver.chrome.driver", "src/main/resources/chromedriver");
        //FirefoxProfile profile = new FirefoxProfile();
        //profile.setPreference("webdriver.firefox.port", 15000);

        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        driver.get("http://www.otto.de");
        driver.manage().window().setPosition(new Point(0, 0));

        Long height = (Long) ((JavascriptExecutor)driver).executeScript("return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);");
        driver.manage().window().setSize(new Dimension(1200, height.intValue()));
        System.out.println(driver.getPageSource());
        System.out.println(driver.getTitle());
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        final BufferedImage image = ImageIO.read(screenshot);
        driver.get("http://www.otto.de/damenmode");
        driver.manage().window().setPosition(new Point(0, 0));
        height = (Long) ((JavascriptExecutor) driver).executeScript("return document.body.clientHeight;");
        driver.manage().window().setSize(new Dimension(1200, height.intValue()));

        File screenshot2 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        final BufferedImage image2 = ImageIO.read(screenshot2);
        ImageIO.write(getDifferenceImage(image, image2), "png", new File("build/diff.png"));
        driver.close();
        driver.quit();
    }

    public static BufferedImage getDifferenceImage(BufferedImage img1, BufferedImage img2) {
        // convert images to pixel arrays...
        final int w = img1.getWidth();
        final int h = img1.getHeight() > img2.getHeight() ? img2.getHeight() : img1.getHeight();
        final int highlight = Color.MAGENTA.getRGB();
        final int[] p1 = img1.getRGB(0, 0, w, h, null, 0, w);
        final int[] p2 = img2.getRGB(0, 0, w, h, null, 0, w);
        // compare img1 to img2, pixel by pixel. If different, highlight img1's pixel...
        for (int i = 0; i < p1.length; i++) {
            if (p1[i] != p2[i]) {
                p1[i] = highlight;
            }
        }
        // save img1's pixels to a new BufferedImage, and return it...
        // (May require TYPE_INT_ARGB)
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, p1, 0, w);
        return out;
    }

}
