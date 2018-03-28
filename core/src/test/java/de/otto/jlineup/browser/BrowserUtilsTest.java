package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.*;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.JLineupRunConfiguration.jLineupRunConfigurationBuilder;
import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void shouldGenerateScreenshotsParameters() throws FileNotFoundException {
        //given
        Config config = Config.readConfig(".", "src/test/resources/lineup_test.json");

        JLineupRunConfiguration jLineupRunConfiguration = jLineupRunConfigurationBuilder()
                .withWorkingDirectory("some/working/dir")
                .withScreenshotDirectory("screenshots")
                .withUrlReplacements(ImmutableMap.of("google", "doodle"))
                .withStep(Step.before)
                .build();

        UrlConfig expectedUrlConfigForOttoDe = getExpectedUrlConfigForOttoDe();
        UrlConfig expectedUrlConfigForGoogleDe = getExpectedUrlConfigForGoogleDe();

        final List<ScreenshotContext> expectedScreenshotContextList = ImmutableList.of(
                ScreenshotContext.of("https://www.otto.de", "/", 600, true, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "/", 800, true, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "/", 1200, true, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "multimedia", 600, true, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "multimedia", 800, true, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("https://www.otto.de", "multimedia", 1200, true, expectedUrlConfigForOttoDe),
                ScreenshotContext.of("http://www.doodle.de", "/", 1200, true, expectedUrlConfigForGoogleDe)
        );

        //when
        final List<ScreenshotContext> screenshotContextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(jLineupRunConfiguration, config);

        //then
        assertThat(screenshotContextList, containsInAnyOrder(expectedScreenshotContextList.toArray()));
    }

    @Test
    public void shouldPrepareDomain() {
        //given
        JLineupRunConfiguration jLineupRunConfiguration = jLineupRunConfigurationBuilder()
                .withUrlReplacements(ImmutableMap.of(".otto.", ".bonprix."))
                .build();
        //when
        String result = BrowserUtils.prepareDomain(jLineupRunConfiguration, "www.otto.de");
        //then
        assertThat(result, is("www.bonprix.de"));
    }

    public static UrlConfig getExpectedUrlConfigForOttoDe() {
        return new UrlConfig(ImmutableList.of("/","multimedia"), 0.05f, ImmutableList.of(new Cookie("testcookie1", "true"), new Cookie("testcookie2", "1")), ImmutableMap.of("live", "www"), ImmutableMap.of("teststorage", "{'testkey':{'value':true,'timestamp':9467812242358}}"), ImmutableMap.of("testsession", "{'testkey':{'value':true,'timestamp':9467812242358}}"), ImmutableList.of(600,800,1200),50000,2, 1, 0,3, "console.log('Moin!');", 0);
    }

    public static UrlConfig getExpectedUrlConfigForGoogleDe() {
        return new UrlConfig(ImmutableList.of("/"), 0.05f, null, null, null, null, ImmutableList.of(1200), 100000, 0, 0, 0, 0, null, 0);
    }

}