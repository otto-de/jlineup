package de.otto.jlineup.browser;

import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.io.Files.equal;
import static de.otto.jlineup.browser.Browser.Type.CHROME;
import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static de.otto.jlineup.browser.Browser.Type.PHANTOMJS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class BrowserTest {

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = Browser.generateScreenshotFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        assertThat(outputString, is("www_otto_de_multimedia_1000_2000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = Browser.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("www_otto_de_root_1000_2000_before.png"));
    }

    @Test
    @Ignore //TODO: Find out why this doesn't work in conjunction with other tests
    public void shouldGetFirefoxDriver() {
        final Config config = new Config(null, FIREFOX, 5f, 800);
        assertSetDriverType(config, MarionetteDriver.class);
    }

    @Test
    public void shouldGetChromeDriver() throws InterruptedException {
        final Config config = new Config(null, CHROME, 5f, 800);
        assertSetDriverType(config, ChromeDriver.class);
    }

    @Test
    public void shouldGetPhantomJSDriver() throws InterruptedException {
        final Config config = new Config(null, PHANTOMJS, 5f, 800);
        assertSetDriverType(config, PhantomJSDriver.class);
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

    @Test
    public void shouldGenerateFullPathToPngFile() {
        Parameters parameters = Mockito.mock(Parameters.class);
        when(parameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");
        Browser browser = new Browser(parameters);
        final String fullFileNameWithPath = browser.getFullScreenshotFileNameWithPath("testurl", "/", 1001, 2002, "step");
        assertThat(fullFileNameWithPath, is("some/working/dir/screenshots/testurl_root_1001_2002_step.png"));
    }

    @Test
    public void shouldGenerateDifferenceImage() throws IOException {
        Parameters parameters = Mockito.mock(Parameters.class);
        Browser browser = new Browser(parameters);
        when(parameters.getWorkingDirectory()).thenReturn("src/test/resources/");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");

        browser.generateDifferenceImage("url", "/", 1001, 2002);

        final String generatedDifferenceImagePath = browser.getFullScreenshotFileNameWithPath("url", "/", 1001, 2002, "DIFFERENCE");
        final String referenceDifferenceImagePath = browser.getFullScreenshotFileNameWithPath("url", "/", 1001, 2002, "DIFFERENCE_reference");

        assertThat(equal(new File(generatedDifferenceImagePath), new File(referenceDifferenceImagePath)), is(true));

        Files.delete(Paths.get(generatedDifferenceImagePath));
    }

}