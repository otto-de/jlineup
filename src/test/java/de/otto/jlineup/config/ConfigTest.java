package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.FileNotFoundException;

import static de.otto.jlineup.browser.Browser.Type.CHROME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigTest {

    @Test
    public void shouldReadConfig() throws FileNotFoundException {
        Config config = Config.readConfig("src/test/resources/lineup_test.json");

        assertThat(config.getBrowser(), is(CHROME));
        assertThat(config.getAsyncWait(), is(2f));
        assertThat(config.getUrls().get("https://www.otto.de").resolutions, is(ImmutableList.of(600, 800, 1200)));
        assertThat(config.getUrls().get("https://www.otto.de").paths, is(ImmutableList.of("/","multimedia")));
    }

}