package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.otto.jlineup.RunStepConfig.runStepConfigBuilder;
import static de.otto.jlineup.browser.Browser.Type.*;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.JobConfig.DEFAULT_WINDOW_HEIGHT;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
        JobConfig jobConfig = JobConfig.readConfig(".", "src/test/resources/lineup_test.json").insertDefaults();

        RunStepConfig runStepConfig = runStepConfigBuilder()
                .withWorkingDirectory("some/working/dir")
                .withScreenshotsDirectory("screenshots")
                .withUrlReplacements(ImmutableMap.of("google", "doodle"))
                .withStep(RunStep.before)
                .build();

        UrlConfig expectedUrlConfigForOttoDe = getExpectedUrlConfigForOttoDe();
        UrlConfig expectedUrlConfigForGoogleDe = getExpectedUrlConfigForGoogleDe();
        int expectedHeight = DEFAULT_WINDOW_HEIGHT;

        final List<ScreenshotContext> expectedScreenshotContextList = ImmutableList.of(
                ScreenshotContext.of("https://www.otto.de", "/", deviceConfig(600, expectedHeight), before, expectedUrlConfigForOttoDe, expectedUrlConfigForOttoDe.cookies),
                ScreenshotContext.of("https://www.otto.de", "/", deviceConfig(800, expectedHeight), before, expectedUrlConfigForOttoDe, expectedUrlConfigForOttoDe.cookies),
                ScreenshotContext.of("https://www.otto.de", "/", deviceConfig(1200, expectedHeight), before, expectedUrlConfigForOttoDe, expectedUrlConfigForOttoDe.cookies),
                ScreenshotContext.of("https://www.otto.de", "multimedia", deviceConfig(600, expectedHeight), before, expectedUrlConfigForOttoDe, expectedUrlConfigForOttoDe.cookies),
                ScreenshotContext.of("https://www.otto.de", "multimedia", deviceConfig(800, expectedHeight), before, expectedUrlConfigForOttoDe, expectedUrlConfigForOttoDe.cookies),
                ScreenshotContext.of("https://www.otto.de", "multimedia", deviceConfig(1200, expectedHeight), before, expectedUrlConfigForOttoDe, expectedUrlConfigForOttoDe.cookies),
                ScreenshotContext.of("http://www.doodle.de", "/", deviceConfig(1200, expectedHeight), before, expectedUrlConfigForGoogleDe, Stream.concat(expectedUrlConfigForGoogleDe.cookies.stream(), expectedUrlConfigForGoogleDe.alternatingCookies.get(0).stream()).collect(Collectors.toList()), "http://www.google.de"),
                ScreenshotContext.of("http://www.doodle.de", "/", deviceConfig(1200, expectedHeight), before, expectedUrlConfigForGoogleDe, Stream.concat(expectedUrlConfigForGoogleDe.cookies.stream(), expectedUrlConfigForGoogleDe.alternatingCookies.get(1).stream()).collect(Collectors.toList()), "http://www.google.de")
        );

        //when
        final List<ScreenshotContext> screenshotContextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);

        //then
        assertThat(screenshotContextList, containsInAnyOrder(expectedScreenshotContextList.toArray()));
    }

    @Test
    public void shouldHaveTheSameContextHashForBeforeAndAfterSteps() throws IOException {
        //given
        JobConfig jobConfigBefore = JobConfig.readConfig(".", "src/test/resources/lineup_test_context_before.json").insertDefaults();
        JobConfig jobConfigAfter = JobConfig.readConfig(".", "src/test/resources/lineup_test_context_after.json").insertDefaults();

        RunStepConfig runStepConfigBefore = runStepConfigBuilder()
                .withStep(RunStep.before)
                .build();

        RunStepConfig runStepConfigAfter = runStepConfigBuilder()
                .withStep(RunStep.after)
                .build();

        //when
        final List<ScreenshotContext> screenshotContextListBefore = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfigBefore, jobConfigBefore);
        final List<ScreenshotContext> screenshotContextListAfter = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfigAfter, jobConfigAfter);

        //then
        assertThat(screenshotContextListBefore.stream().map(ScreenshotContext::contextHash).collect(Collectors.toList()),
                is(screenshotContextListAfter.stream().map(ScreenshotContext::contextHash).collect(Collectors.toList())));
    }

    @Test
    public void shouldPrepareDomain() {
        //given
        RunStepConfig runStepConfig = runStepConfigBuilder()
                .withUrlReplacements(ImmutableMap.of(".otto.", ".bonprix."))
                .build();
        //when
        String result = BrowserUtils.prepareDomain(runStepConfig, "www.otto.de");
        //then
        assertThat(result, is("www.bonprix.de"));
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
    public void shouldAddCookiesToTestSetupCalls() {
        final JobConfig jobConfig = jobConfigBuilder().addUrlConfig(getExpectedUrlConfigForOttoDe().url, getExpectedUrlConfigForOttoDe()).build();
        List<ScreenshotContext> screenshotContexts = BrowserUtils.buildTestSetupContexts(runStepConfigBuilder().withStep(RunStep.before).build(), jobConfig);
        assertThat(screenshotContexts.size(), is(1));
        assertThat(screenshotContexts.get(0).cookies.size(), is(2));
        assertThat(screenshotContexts.get(0).cookies.get(0).name, is("testcookie1"));
        assertThat(screenshotContexts.get(0).cookies.get(0).value, is("true"));
        assertThat(screenshotContexts.get(0).cookies.get(1).name, is("testcookie2"));
        assertThat(screenshotContexts.get(0).cookies.get(1).value, is("1"));
    }

    @Test
    public void shouldAddCookiesToTestCleanupCalls() {
        final JobConfig jobConfig = jobConfigBuilder().addUrlConfig(getExpectedUrlConfigForOttoDe().url, getExpectedUrlConfigForOttoDe()).build();
        List<ScreenshotContext> screenshotContexts = BrowserUtils.buildTestCleanupContexts(runStepConfigBuilder().withStep(RunStep.before).build(), jobConfig);
        assertThat(screenshotContexts.size(), is(1));
        assertThat(screenshotContexts.get(0).cookies.size(), is(2));
        assertThat(screenshotContexts.get(0).cookies.get(0).name, is("testcookie1"));
        assertThat(screenshotContexts.get(0).cookies.get(0).value, is("true"));
        assertThat(screenshotContexts.get(0).cookies.get(1).name, is("testcookie2"));
        assertThat(screenshotContexts.get(0).cookies.get(1).value, is("1"));
    }

    public static UrlConfig getExpectedUrlConfigForOttoDe() {

        return UrlConfig.urlConfigBuilder()
                .withUrl("https://www.otto.de")
                .withPaths(ImmutableList.of("/", "multimedia"))
                .withSetupPaths(ImmutableList.of("setup"))
                .withCleanupPaths(ImmutableList.of("cleanup"))
                .withMaxDiff(0.05d)
                .withCookies(ImmutableList.of(new Cookie("testcookie1", "true"), new Cookie("testcookie2", "1")))
                .withEnvMapping(ImmutableMap.of("live", "www"))
                .withLocalStorage(ImmutableMap.of("teststorage", "{'testkey':{'value':true,'timestamp':9467812242358}}"))
                .withSessionStorage(ImmutableMap.of("testsession", "{'testkey':{'value':true,'timestamp':9467812242358}}"))
                .withDevices(ImmutableList.of(deviceConfig(600, 800), deviceConfig(800, 800), deviceConfig(1200, 800)))
                .withMaxScrollHeight(50000)
                .withWaitAfterPageLoad(2)
                .withWaitAfterScroll(1)
                .withWarmupBrowserCacheTime(3)
                .withJavaScript("console.log('Moin!');")
                .build();
    }

    public static UrlConfig getExpectedUrlConfigForGoogleDe() {

        return UrlConfig.urlConfigBuilder()
                .withUrl("http://www.google.de")
                .withPath("/")
                .withMaxDiff(0.05d)
                .withCookies(ImmutableList.of(new Cookie("classic", "true")))
                .withAlternatingCookies(ImmutableList.of(
                        ImmutableList.of(
                                new Cookie("alternating", "case1", null, null, null, false, null, true)),
                        ImmutableList.of(
                                new Cookie("alternating", "case2", null, null, null, false, null, true))
                ))
                .withDevices(ImmutableList.of(deviceConfig(1200, 800)))
                .withMaxScrollHeight(100000)
                .build();
    }

    private void assertSetDriverType(JobConfig jobConfig, Class<? extends WebDriver> driverClass) {
        WebDriver driver = null;
        BrowserUtils realBrowserUtils = new BrowserUtils();
        try {
            driver = realBrowserUtils.getWebDriverByConfig(jobConfig, runStepConfigBuilder().build());
            assertTrue(driverClass.isInstance(driver));
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

}