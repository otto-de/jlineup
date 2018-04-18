package de.otto.jlineup.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.browser.BrowserUtilsTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigTest {

    @Test
    public void shouldReadConfig() throws FileNotFoundException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_test.json");
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldReadConfigAndParsedWithJackson() throws IOException {
        JobConfig jobConfigGson = JobConfig.readConfig("src/test/resources/", "lineup_test.json");
        JobConfig jobConfigJackson = new ObjectMapper()
                .readValue(new File("src/test/resources/lineup_test.json"), JobConfig.class);
        Assert.assertEquals(jobConfigGson, jobConfigJackson);
    }

    @Test
    public void shouldReadConfigFromDifferentPathThanWorkingDir() throws FileNotFoundException {
        JobConfig jobConfig = JobConfig.readConfig("someWorkingDir", "src/test/resources/lineup_test.json");
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionWhenConfigFileIsNotFound() throws FileNotFoundException {
        JobConfig.readConfig("someWorkingDir", "nonexisting.json");
        //assert that FileNotFoundException is thrown (see expected above)
    }

    @Test
    public void shouldReadMinimalConfigAndInsertDefaults() throws FileNotFoundException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_minimal_test.json");
        assertThat(jobConfig.browser.isPhantomJS(), is(true));
        assertThat(jobConfig.windowHeight, is(800));
        assertThat(jobConfig.urls.get("https://www.otto.de").windowWidths, is(ImmutableList.of(800)));
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/")));
        assertThat(jobConfig.globalWaitAfterPageLoad, is(0F));
        assertThat(jobConfig.pageLoadTimeout, is(120));
        assertThat(jobConfig.screenshotRetries, is(0));
    }

    private void assertThatConfigContentsAreCorrect(JobConfig jobConfig) {
        assertThat(jobConfig.browser.isFirefox(), is(true));
        assertThat(jobConfig.browser.isHeadless(), is(false));
        assertThat(jobConfig.globalWaitAfterPageLoad, is(1f));
        assertThat(jobConfig.urls.get("https://www.otto.de").windowWidths, is(ImmutableList.of(600, 800, 1200)));
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/","multimedia")));
        assertThat(jobConfig.urls.get("https://www.otto.de"), is(BrowserUtilsTest.getExpectedUrlConfigForOttoDe()));
        assertThat(jobConfig.urls.get("http://www.google.de"), is(BrowserUtilsTest.getExpectedUrlConfigForGoogleDe()));
        assertThat(jobConfig.pageLoadTimeout, is(60));
        assertThat(jobConfig.screenshotRetries, is(2));
    }

}