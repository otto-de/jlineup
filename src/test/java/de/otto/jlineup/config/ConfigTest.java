package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtilsTest;
import org.junit.Test;

import java.io.FileNotFoundException;

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
    }

    private void assertThatConfigContentsAreCorrect(Config config) {
        assertThat(config.browser, is(FIREFOX));
        assertThat(config.globalWaitAfterPageLoad, is(1f));
        assertThat(config.urls.get("https://www.otto.de").windowWidths, is(ImmutableList.of(600, 800, 1200)));
        assertThat(config.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/","multimedia")));
        assertThat(config.urls.get("https://www.otto.de"), is(BrowserUtilsTest.getExpectedUrlConfigForOttoDe()));
        assertThat(config.urls.get("http://www.google.de"), is(BrowserUtilsTest.getExpectedUrlConfigForGoogleDe()));
    }

}