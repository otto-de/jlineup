package de.otto.jlineup.browser;

import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.io.Files.equal;
import static de.otto.jlineup.browser.Browser.Type.CHROME;
import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class BrowserTest {

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = Browser.generateFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        assertThat(outputString, is("www_otto_de_multimedia_1000_2000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = Browser.generateFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("www_otto_de_root_1000_2000_before.png"));
    }

    @Test
    @Ignore
    public void shouldGetFirefoxDriver() {
        final Config config = new Config(null, FIREFOX, 5f, 800);
        assertSetDriverType(config, MarionetteDriver.class);
    }

    @Test
    public void shouldGetChromeDriver() throws InterruptedException {
        final Config config = new Config(null, CHROME, 5f, 800);
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

    @Test
    public void shouldGenerateFullPathToPngFile() {
        Parameters parameters = Mockito.mock(Parameters.class);
        when(parameters.getWorkingDirectory()).thenReturn("/src/test/resources/");
        Browser browser = new Browser(parameters);
        final String fullFileNameWithPath = browser.getFullFileNameWithPath("testurl", "/", 1001, 2002, "step");
        assertThat(fullFileNameWithPath, is("/src/test/resources/testurl_root_1001_2002_step.png"));
    }

    @Test
    public void shouldGenerateDifferenceImage() throws IOException {
        Parameters parameters = Mockito.mock(Parameters.class);
        Browser browser = new Browser(parameters);
        when(parameters.getWorkingDirectory()).thenReturn("src/test/resources/");

        browser.generateDifferenceImage("url", "/", 1001, 2002);

        final String generatedDifferenceImagePath = browser.getFullFileNameWithPath("url", "/", 1001, 2002, "DIFFERENCE");
        final String referenceDifferenceImagePath = browser.getFullFileNameWithPath("url", "/", 1001, 2002, "DIFFERENCE_reference");

        assertThat(equal(new File(generatedDifferenceImagePath), new File(referenceDifferenceImagePath)), is(true));

        Files.delete(Paths.get(generatedDifferenceImagePath));
    }

}