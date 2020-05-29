package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.*;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.RunStepConfig.jLineupRunConfigurationBuilder;
import static de.otto.jlineup.browser.Browser.Type.*;
import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.JobConfig.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class BrowserUtilsTest {

    @Test
    public void shouldReplaceEnvMappingsCorrectly() {

        Map<String, String> envMapping = ImmutableMap.of("originalOne", "replacementOne", "originalTwo", "replacementTwo");

        final String urlOne = buildUrl("https://originalOne.otto.de", "/", envMapping);
        final String urlTwo = buildUrl("http://originalTwo.otto.de", "/", envMapping);
        final String urlThree = buildUrl("http://mega.originalOne.otto.de", "/", envMapping);

        assertThat(urlOne, is("https://replacementOne.otto.de/"));
        assertThat(urlTwo, is("http://replacementTwo.otto.de/"));
        assertThat(urlThree, is("http://mega.replacementOne.otto.de/"));
    }

    @Test
    public void shouldBuildUrl() {
        final String url = buildUrl("url", "path");
        assertThat(url, is("url/path"));
    }

    @Test
    public void shouldStripUnnecessarySlashesFromUrl() {
        final String url = buildUrl("url/", "/path");
        assertThat(url, is("url/path"));
    }

    @Test
    public void shouldNotAddSlashWhenNoneIsConfigured() {
        final String url = buildUrl("lalal?lo=1", "");
        assertThat(url, is("lalal?lo=1"));
    }

    @Test
    public void shouldGenerateScreenshotsParameters() throws IOException {
        //given
        JobConfig jobConfig = JobConfig.readConfig(".", "src/test/resources/lineup_test.json");

        RunStepConfig runStepConfig = jLineupRunConfigurationBuilder()
                .withWorkingDirectory("some/working/dir")
                .withScreenshotsDirectory("screenshots")
                .withUrlReplacements(ImmutableMap.of("google", "doodle"))
                .withStep(Step.before)
                .build();

        UrlConfig expectedUrlConfigForOttoDe = getExpectedUrlConfigForOttoDe();
        UrlConfig expectedUrlConfigForGoogleDe = getExpectedUrlConfigForGoogleDe();
        int expectedHeight = jobConfig.windowHeight;

        final List<ScreenshotContext> expectedScreenshotContextList = ImmutableList.of(
                ScreenshotContext.of("https://www.otto.de", "/", deviceConfig(600, expectedHeight), Step.before, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "/", deviceConfig(800, expectedHeight), Step.before, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "/", deviceConfig(1200, expectedHeight), Step.before, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "multimedia", deviceConfig(600, expectedHeight), Step.before, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "multimedia", deviceConfig(800, expectedHeight), Step.before, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "multimedia", deviceConfig(1200, expectedHeight), Step.before, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("http://www.doodle.de", "/",         deviceConfig(1200, expectedHeight), Step.before, expectedUrlConfigForGoogleDe, "http://www.google.de")
        );

        //when
        final List<ScreenshotContext> screenshotContextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);

        //then
        assertThat(screenshotContextList, containsInAnyOrder(expectedScreenshotContextList.toArray()));
    }

    @Test
    public void shouldPrepareDomain() {
        //given
        RunStepConfig runStepConfig = jLineupRunConfigurationBuilder()
                .withUrlReplacements(ImmutableMap.of(".otto.", ".bonprix."))
                .build();
        //when
        String result = BrowserUtils.prepareDomain(runStepConfig, "www.otto.de");
        //then
        assertThat(result, is("www.bonprix.de"));
    }

    public static UrlConfig getExpectedUrlConfigForOttoDe() {

        return UrlConfig.urlConfigBuilder()
                .withPaths(ImmutableList.of("/", "multimedia"))
                .withMaxDiff(0.05f)
                .withCookies(ImmutableList.of(new Cookie("testcookie1", "true"), new Cookie("testcookie2", "1")))
                .withEnvMapping(ImmutableMap.of("live", "www"))
                .withLocalStorage(ImmutableMap.of("teststorage", "{'testkey':{'value':true,'timestamp':9467812242358}}"))
                .withSessionStorage(ImmutableMap.of("testsession", "{'testkey':{'value':true,'timestamp':9467812242358}}"))
                .withWindowWidths(ImmutableList.of(600, 800, 1200))
                .withMaxScrollHeight(50000)
                .withWaitAfterPageLoad(2)
                .withWaitAfterScroll(1)
                .withWarmupBrowserCacheTime(3)
                .withJavaScript("console.log('Moin!');")
                .build();
    }

    public static UrlConfig getExpectedUrlConfigForGoogleDe() {

        return UrlConfig.urlConfigBuilder()
                .withPath("/")
                .withMaxDiff(0.05f)
                .withWindowWidths(ImmutableList.of(1200))
                .withMaxScrollHeight(100000)
                .build();
    }

    @Test
    public void shouldGetFirefoxDriver() {
        final JobConfig jobConfig = jobConfigBuilder().withBrowser(FIREFOX).build();
        assertSetDriverType(jobConfig, FirefoxDriver.class);
    }

    @Test
    public void shouldGetChromeDriver() {
        final JobConfig jobConfig = jobConfigBuilder().withBrowser(CHROME).build();
        assertSetDriverType(jobConfig, ChromeDriver.class);
    }

    @Test
    public void shouldGetChromeDriverForHeadlessChrome() {
        final JobConfig jobConfig = jobConfigBuilder().withBrowser(CHROME_HEADLESS).build();
        assertSetDriverType(jobConfig, ChromeDriver.class);
    }

    @Test
    public void shouldGetPhantomJSDriver() {
        final JobConfig jobConfig = jobConfigBuilder().withBrowser(PHANTOMJS).build();
        assertSetDriverType(jobConfig, PhantomJSDriver.class);
    }

    private void assertSetDriverType(JobConfig jobConfig, Class<? extends WebDriver> driverClass) {
        WebDriver driver = null;
        BrowserUtils realBrowserUtils = new BrowserUtils();
        try {
            driver = realBrowserUtils.getWebDriverByConfig(jobConfig, jLineupRunConfigurationBuilder().build());
            assertTrue(driverClass.isInstance(driver));
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

}