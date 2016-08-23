package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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

}