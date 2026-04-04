package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.BrowserUtilsTest;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.JobConfig.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobConfigTest {

    @Test
    void shouldReadConfig() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_test.json").insertDefaults();
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test
    void shouldReadYamlConfig() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_test.yaml").insertDefaults();
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldReadConfigFromDifferentPathThanWorkingDir() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("someWorkingDir", "src/test/resources/lineup_test.json").insertDefaults();
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldReadYamlConfigFromDifferentPathThanWorkingDir() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("someWorkingDir", "src/test/resources/lineup_test.yaml").insertDefaults();
        assertThatConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldThrowFileNotFoundExceptionWhenConfigFileIsNotFound() throws IOException {
        //assert that following line throws FileNotFoundException
        assertThrows(FileNotFoundException.class, () -> JobConfig.readConfig("someWorkingDir", "nonexisting.json"));
    }

    @Test
    public void shouldReadMinimalConfigAndInsertDefaults() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_minimal_test.json").insertDefaults();
        assertThatMinimalConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldReadMinimalYamlConfigAndInsertDefaults() throws IOException {
        JobConfig jobConfig = JobConfig.readConfig("src/test/resources/", "lineup_minimal_test.yaml").insertDefaults();
        assertThatMinimalConfigContentsAreCorrect(jobConfig);
    }

    @Test
    public void shouldExcludeDefaultsFromSerializedConfig() {
        JobConfig jobConfig = JobConfig.defaultConfig();
        String printedConfig = JobConfig.prettyPrint(jobConfig);

        JobConfig jobConfig1 = new JobConfig();

        assertThat(jobConfig.httpCheck, is(jobConfig1.httpCheck));

        assertThat(printedConfig, not(containsString("http-check")));
    }

    @Test
    public void shouldExcludeDefaultsFromSerializedYamlConfig() {
        JobConfig jobConfig = JobConfig.defaultConfig();
        String printedConfig = JobConfig.prettyPrint(jobConfig, JacksonWrapper.ConfigFormat.YAML);

        assertThat(printedConfig, not(containsString("http-check")));
    }

    @Test
    public void shouldRoundTripJsonToYamlAndBack() throws IOException {
        JobConfig original = JobConfig.readConfig("src/test/resources/", "lineup_test.json");

        // Serialize to YAML
        String yaml = JobConfig.prettyPrint(original, JacksonWrapper.ConfigFormat.YAML);
        assertThat(yaml, containsString("urls:"));
        assertThat(yaml, not(startsWith("{")));

        // Deserialize from YAML
        JobConfig fromYaml = JacksonWrapper.deserializeConfig(new StringReader(yaml), JacksonWrapper.ConfigFormat.YAML);

        // Serialize back to JSON
        String json = JobConfig.prettyPrint(fromYaml, JacksonWrapper.ConfigFormat.JSON);

        // Deserialize from JSON
        JobConfig fromJson = JacksonWrapper.deserializeConfig(new StringReader(json), JacksonWrapper.ConfigFormat.JSON);

        assertThat(fromYaml, is(original));
        assertThat(fromJson, is(original));
    }

    @Test
    public void shouldSerializeExampleConfigAsYaml() {
        JobConfig exampleConfig = JobConfig.exampleConfig();
        String yaml = JobConfig.prettyPrintWithAllFields(exampleConfig, JacksonWrapper.ConfigFormat.YAML);

        assertThat(yaml, containsString("urls:"));
        assertThat(yaml, containsString("https://www.example.com:"));
        assertThat(yaml, containsString("browser:"));
        assertThat("YAML output should not start with '{' (would indicate JSON format)",
                yaml, not(startsWith("{")));
    }

    @Test
    public void shouldDetectFormatFromFilename() {
        assertThat(JacksonWrapper.ConfigFormat.fromFilename("lineup.json"), is(JacksonWrapper.ConfigFormat.JSON));
        assertThat(JacksonWrapper.ConfigFormat.fromFilename("lineup.yaml"), is(JacksonWrapper.ConfigFormat.YAML));
        assertThat(JacksonWrapper.ConfigFormat.fromFilename("lineup.yml"), is(JacksonWrapper.ConfigFormat.YAML));
        assertThat(JacksonWrapper.ConfigFormat.fromFilename("config.YAML"), is(JacksonWrapper.ConfigFormat.YAML));
        assertThat(JacksonWrapper.ConfigFormat.fromFilename("config.YML"), is(JacksonWrapper.ConfigFormat.YAML));
        assertThat(JacksonWrapper.ConfigFormat.fromFilename(null), is(JacksonWrapper.ConfigFormat.JSON));
        assertThat(JacksonWrapper.ConfigFormat.fromFilename("config.txt"), is(JacksonWrapper.ConfigFormat.JSON));
    }

    private void assertThatMinimalConfigContentsAreCorrect(JobConfig jobConfig) {
        assertThat(jobConfig.browser.isHeadlessRealBrowser(), is(true));
        assertThat(jobConfig.windowHeight, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").windowWidths, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").devices, is(ImmutableList.of(deviceConfig(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT))));
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of(DEFAULT_PATH)));
        assertThat(jobConfig.globalWaitAfterPageLoad, is(0F));
        assertThat(jobConfig.pageLoadTimeout, is(120));
        assertThat(jobConfig.screenshotRetries, is(0));
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