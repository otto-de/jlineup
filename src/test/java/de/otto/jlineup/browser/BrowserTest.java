package de.otto.jlineup.browser;

import de.otto.jlineup.config.Config;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;

import static de.otto.jlineup.browser.Browser.Type.CHROME;
import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class BrowserTest {

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = Browser.generateFileName("https://www.otto.de/", "multimedia", 1000, "after");
        assertThat(outputString, is("www_otto_de_multimedia_1000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = Browser.generateFileName("https://www.otto.de/", "/", 1000, "before");
        assertThat(outputString, is("www_otto_de_root_1000_before.png"));
    }

    @Test
    @Ignore
    public void shouldGetFirefoxDriver() {
        final Config config = new Config(null, FIREFOX, 5);
        assertSetDriverType(config, MarionetteDriver.class);
    }

    @Test
    public void shouldGetChromeDriver() throws InterruptedException {
        final Config config = new Config(null, CHROME, 5);
        assertSetDriverType(config, ChromeDriver.class);
    }

    private void assertSetDriverType(Config config, Class<? extends WebDriver> driverClass) {
        WebDriver driver = null;
        try {
            driver = Browser.getWebDriverByConfig(config);
            assertTrue(driverClass.isInstance(driver));
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }
}