package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.config.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;

import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigTest {

    @Test
    public void shouldReadConfig() throws FileNotFoundException {
        Config config = Config.readConfig("src/test/resources/lineup.json");

        assertThat(config.browser, is(FIREFOX));
        assertThat(config.asyncWait, is(2f));
        assertThat(config.urls.get("https://www.otto.de").resolutions, is(ImmutableList.of(600, 800, 1200)));
        assertThat(config.urls.get("https://www.otto.de").paths, is(ImmutableList.of("/","multimedia")));
    }

}