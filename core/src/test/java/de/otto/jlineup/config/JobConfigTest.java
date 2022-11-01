package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.browser.BrowserUtilsTest;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.JobConfig.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobConfigTest {

    @Test
    public void shouldReadConfig() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_test.json").insertDefaults();
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldReadConfigFromDifferentPathThanWorkingDir() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("someWorkingDir", "src/test/resources/lineup_test.json").insertDefaults();
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionWhenConfigFileIsNotFound() throws IOException {
        JobConfig.readConfig("someWorkingDir", "nonexisting.json");
        //assert that FileNotFoundException is thrown (see expected above)
    }

    @Test
    public void shouldReadMinimalConfigAndInsertDefaults() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_minimal_test.json").insertDefaults();
        assertThat(jobConfig.browser.isHeadlessRealBrowser(), is(true));
        assertThat(jobConfig.windowHeight, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").windowWidths, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").devices, is(ImmutableList.of(deviceConfig(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT))));
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of(DEFAULT_PATH)));
        assertThat(jobConfig.globalWaitAfterPageLoad, is(0F));
        assertThat(jobConfig.pageLoadTimeout, is(120));
        assertThat(jobConfig.screenshotRetries, is(0));
    }

    @Test
    public void shouldExcludeDefaultsFromSerializedConfig() {
        JobConfig jobConfig = JobConfig.defaultConfig();
        String printedConfig = JobConfig.prettyPrint(jobConfig);

        JobConfig jobConfig1 = new JobConfig();

        assertThat(jobConfig.httpCheck, is(jobConfig1.httpCheck));

        assertThat(printedConfig, not(containsString("http-check")));
    }

    private void assertThatConfigContentsAreCorrect(JobConfig jobConfig) {
        assertThat(jobConfig.browser.isFirefox(), is(true));
        assertThat(jobConfig.browser.isHeadless(), is(true));
        assertThat(jobConfig.globalWaitAfterPageLoad, is(1f));
        assertThat(jobConfig.urls.get("https://www.otto.de").windowWidths, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").devices, is(ImmutableList.of(deviceConfig(600, DEFAULT_WINDOW_HEIGHT), deviceConfig(800, DEFAULT_WINDOW_HEIGHT), deviceConfig(1200, DEFAULT_WINDOW_HEIGHT))));
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/","multimedia")));
        assertThat(jobConfig.urls.get("https://www.otto.de"), is(BrowserUtilsTest.getExpectedUrlConfigForOttoDe()));
        assertThat(jobConfig.urls.get("http://www.google.de"), is(BrowserUtilsTest.getExpectedUrlConfigForGoogleDe()));
        assertThat(jobConfig.pageLoadTimeout, is(60));
        assertThat(jobConfig.screenshotRetries, is(2));
    }

}