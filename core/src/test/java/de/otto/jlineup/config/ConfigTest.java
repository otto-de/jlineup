package de.otto.jlineup.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtilsTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigTest {

    @Test
    public void shouldReadConfig() throws FileNotFoundException {
        Config config = Config.readConfig("src/test/resources/", "lineup_test.json");
        assertThatConfigContentsAreCorrect(config);
    }

    @Test
    public void shouldReadConfigAndParsedWithJackson() throws IOException {
        Config configGson = Config.readConfig("src/test/resources/", "lineup_test.json");
        Config configJackson = new ObjectMapper()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .readValue(new File("src/test/resources/lineup_test.json"), Config.class);
        Assert.assertEquals(configGson, configJackson);
    }

    @Test
    public void shouldReadConfigFromDifferentPathThanWorkingDir() throws FileNotFoundException {
        Config config = Config.readConfig("someWorkingDir", "src/test/resources/lineup_test.json");
        assertThatConfigContentsAreCorrect(config);
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionWhenConfigFileIsNotFound() throws FileNotFoundException {
        Config.readConfig("someWorkingDir", "nonexisting.json");
        //assert that FileNotFoundException is thrown (see expected above)
    }

    @Test
    public void shouldReadMinimalConfigAndInsertDefaults() throws FileNotFoundException {
        Config config = Config.readConfig("src/test/resources/", "lineup_minimal_test.json");
        assertThat(config.browser, is(Browser.Type.PHANTOMJS));
        assertThat(config.windowHeight, is(800));
        assertThat(config.urls.get("https://www.otto.de").windowWidths, is(ImmutableList.of(800)));
        assertThat(config.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/")));
        assertThat(config.globalWaitAfterPageLoad, is(0F));
        assertThat(config.pageLoadTimeout, is(120));
        assertThat(config.screenshotRetries, is(0));
    }

    private void assertThatConfigContentsAreCorrect(Config config) {
        assertThat(config.browser, is(FIREFOX));
        assertThat(config.globalWaitAfterPageLoad, is(1f));
        assertThat(config.urls.get("https://www.otto.de").windowWidths, is(ImmutableList.of(600, 800, 1200)));
        assertThat(config.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/","multimedia")));
        assertThat(config.urls.get("https://www.otto.de"), is(BrowserUtilsTest.getExpectedUrlConfigForOttoDe()));
        assertThat(config.urls.get("http://www.google.de"), is(BrowserUtilsTest.getExpectedUrlConfigForGoogleDe()));
        assertThat(config.pageLoadTimeout, is(60));
        assertThat(config.screenshotRetries, is(2));
    }

}