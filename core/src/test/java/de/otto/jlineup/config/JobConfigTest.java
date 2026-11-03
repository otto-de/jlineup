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
    public void shouldUseDefaultReportLabelsIfNotConfigured() {
        JobConfig jobConfig = JobConfig.defaultConfig();

        assertThat(jobConfig.beforeLabel, is(nullValue()));
        assertThat(jobConfig.afterLabel, is(nullValue()));
        assertThat(jobConfig.getEffectiveBeforeLabel(), is("Before"));
        assertThat(jobConfig.getEffectiveAfterLabel(), is("After"));
    }

    @Test
    public void shouldUseDefaultReportLabelsIfConfiguredLabelsAreBlank() {
        JobConfig jobConfig = JobConfig.copyOfBuilder(JobConfig.defaultConfig()).withBeforeLabel("").withAfterLabel("  ").build();

        assertThat(jobConfig.getEffectiveBeforeLabel(), is("Before"));
        assertThat(jobConfig.getEffectiveAfterLabel(), is("After"));
    }

    @Test
    public void shouldReadReportLabelsFromYamlAndJsonConfig() {
        JobConfig fromYaml = JacksonWrapper.deserializeConfig(new StringReader("""
                urls: https://www.example.com
                before-label: Reference (local)
                after-label: Current (dev)
                """), JacksonWrapper.ConfigFormat.YAML);
        JobConfig fromJson = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": {} }, "before-label": "Reference (local)", "after-label": "Current (dev)" }
                """), JacksonWrapper.ConfigFormat.JSON);

        for (JobConfig jobConfig : new JobConfig[]{fromYaml, fromJson}) {
            assertThat(jobConfig.beforeLabel, is("Reference (local)"));
            assertThat(jobConfig.afterLabel, is("Current (dev)"));
            assertThat(jobConfig.getEffectiveBeforeLabel(), is("Reference (local)"));
            assertThat(jobConfig.getEffectiveAfterLabel(), is("Current (dev)"));
        }
    }

    @Test
    public void shouldReadCompressionArtifactObjectFromYamlAndJsonConfig() {
        JobConfig fromYaml = JacksonWrapper.deserializeConfig(new StringReader("""
                urls:
                  https://www.example.com:
                    ignore-compression-artifacts:
                      radius: 7
                      max-pixel-color-distance: 15
                      texture-tolerance: 0.15
                """), JacksonWrapper.ConfigFormat.YAML);
        JobConfig fromJson = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": { "radius": 7, "max-pixel-color-distance": 15, "texture-tolerance": 0.15 } } } }
                """), JacksonWrapper.ConfigFormat.JSON);

        for (JobConfig jobConfig : new JobConfig[]{fromYaml, fromJson}) {
            IgnoreCompressionArtifactsConfig config = jobConfig.urls.get("https://www.example.com").ignoreCompressionArtifacts;
            assertThat(config.enabled, is(true));
            assertThat(config.radius, is(7));
            assertThat(config.maxPixelColorDistance, is(15d));
            assertThat(config.textureTolerance, is(0.15d));
        }
    }

    @Test
    public void shouldReadCompressionArtifactBooleanFromYamlAndJsonConfig() {
        JobConfig fromYaml = JacksonWrapper.deserializeConfig(new StringReader("""
                urls:
                  https://www.example.com:
                    ignore-compression-artifacts: true
                  https://www.example.org:
                    ignore-compression-artifacts: false
                """), JacksonWrapper.ConfigFormat.YAML);
        JobConfig fromJson = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": true }, "https://www.example.org": { "ignore-compression-artifacts": false } } }
                """), JacksonWrapper.ConfigFormat.JSON);

        for (JobConfig jobConfig : new JobConfig[]{fromYaml, fromJson}) {
            assertThat(jobConfig.urls.get("https://www.example.com").ignoreCompressionArtifacts, is(IgnoreCompressionArtifactsConfig.ENABLED));
            assertThat(jobConfig.urls.get("https://www.example.org").ignoreCompressionArtifacts, is(IgnoreCompressionArtifactsConfig.DISABLED));
        }
    }

    @Test
    public void shouldReadDisabledCompressionArtifactObjectWithValues() {
        JobConfig jobConfig = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": { "enabled": false, "radius": 12 } } } }
                """), JacksonWrapper.ConfigFormat.JSON);

        assertThat(jobConfig.urls.get("https://www.example.com").ignoreCompressionArtifacts, is(new IgnoreCompressionArtifactsConfig(false, 12, DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE)));
    }

    @Test
    public void shouldSerializeCompressionArtifactsAsBooleanObjectOrNotAtAll() {
        JobConfig enabled = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": { "enabled": true } } } }
                """), JacksonWrapper.ConfigFormat.JSON);
        JobConfig custom = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": { "radius": 12, "texture-tolerance": 0.15 } } } }
                """), JacksonWrapper.ConfigFormat.JSON);
        JobConfig disabledWithValues = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": { "enabled": false, "radius": 12 } } } }
                """), JacksonWrapper.ConfigFormat.JSON);
        JobConfig disabled = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-compression-artifacts": false } } }
                """), JacksonWrapper.ConfigFormat.JSON);

        assertThat(JobConfig.prettyPrint(enabled), containsString("\"ignore-compression-artifacts\" : true"));
        String customPrinted = JobConfig.prettyPrint(custom);
        assertThat(customPrinted, containsString("\"ignore-compression-artifacts\" : {"));
        assertThat(customPrinted, containsString("\"radius\" : 12"));
        assertThat(customPrinted, containsString("\"texture-tolerance\" : 0.15"));
        assertThat(customPrinted, not(containsString("max-pixel-color-distance")));
        assertThat(customPrinted, not(containsString("\"enabled\"")));
        assertThat(JobConfig.prettyPrint(disabledWithValues), containsString("\"enabled\" : false"));
        assertThat(JobConfig.prettyPrint(disabled), not(containsString("ignore-compression-artifacts")));

        for (JobConfig jobConfig : new JobConfig[]{enabled, custom, disabledWithValues}) {
            JobConfig roundTripped = JacksonWrapper.deserializeConfig(new StringReader(JobConfig.prettyPrint(jobConfig)), JacksonWrapper.ConfigFormat.JSON);
            assertThat(roundTripped.urls.get("https://www.example.com").ignoreCompressionArtifacts, is(jobConfig.urls.get("https://www.example.com").ignoreCompressionArtifacts));
        }
    }

    @Test
    public void shouldReadIgnoreImageContentFromYamlAndJsonConfigAndExcludeTheDefault() {
        JobConfig fromYaml = JacksonWrapper.deserializeConfig(new StringReader("""
                urls:
                  https://www.example.com:
                    ignore-image-content: true
                """), JacksonWrapper.ConfigFormat.YAML);
        JobConfig fromJson = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": { "ignore-image-content": true } } }
                """), JacksonWrapper.ConfigFormat.JSON);
        JobConfig withDefault = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": {} } }
                """), JacksonWrapper.ConfigFormat.JSON);

        assertThat(fromYaml.urls.get("https://www.example.com").ignoreImageContent, is(true));
        assertThat(fromJson.urls.get("https://www.example.com").ignoreImageContent, is(true));
        assertThat(JobConfig.prettyPrint(fromJson), containsString("ignore-image-content"));
        assertThat(withDefault.urls.get("https://www.example.com").ignoreImageContent, is(false));
        assertThat(JobConfig.prettyPrint(withDefault), not(containsString("ignore-image-content")));
    }

    @Test
    public void shouldUseCompressionArtifactDefaultsAndExcludeThemFromSerializedConfig() {
        JobConfig jobConfig = JacksonWrapper.deserializeConfig(new StringReader("""
                { "urls": { "https://www.example.com": {} } }
                """), JacksonWrapper.ConfigFormat.JSON);
        IgnoreCompressionArtifactsConfig config = jobConfig.urls.get("https://www.example.com").ignoreCompressionArtifacts;

        assertThat(config.enabled, is(false));
        assertThat(config.radius, is(DEFAULT_COMPRESSION_ARTIFACT_RADIUS));
        assertThat(config.maxPixelColorDistance, is(DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE));
        assertThat(config.textureTolerance, is(DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE));
        assertThat(JobConfig.prettyPrint(jobConfig), not(containsString("compression-artifact")));
    }

    @Test
    public void shouldKeepReportLabelsWhenInsertingDefaultsAndSanitizing() {
        JobConfig jobConfig = JobConfig.copyOfBuilder(JobConfig.defaultConfig()).withBeforeLabel("Reference").withAfterLabel("Current").build();

        JobConfig processed = jobConfig.insertDefaults().sanitize();

        assertThat(processed.beforeLabel, is("Reference"));
        assertThat(processed.afterLabel, is("Current"));
    }

    @Test
    public void shouldSerializeReportLabelsOnlyIfConfigured() {
        String withoutLabels = JobConfig.prettyPrint(JobConfig.defaultConfig());
        String withLabels = JobConfig.prettyPrint(JobConfig.copyOfBuilder(JobConfig.defaultConfig()).withBeforeLabel("Reference").withAfterLabel("Current").build());

        assertThat(withoutLabels, not(containsString("before-label")));
        assertThat(withoutLabels, not(containsString("after-label")));
        assertThat(withLabels, containsString("\"before-label\" : \"Reference\""));
        assertThat(withLabels, containsString("\"after-label\" : \"Current\""));
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
        assertThat(jobConfig.browser.isHeadless(), is(true));
        assertThat(jobConfig.windowHeight, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").windowWidths, is(nullValue()));
        assertThat(jobConfig.urls.get("https://www.otto.de").devices, is(ImmutableList.of(deviceConfig(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT))));
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of(PathConfig.of(DEFAULT_PATH))));
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
        assertThat(jobConfig.urls.get("https://www.otto.de").paths, is(ImmutableList.of(PathConfig.of("/"), PathConfig.of("multimedia"))));
        assertThat(jobConfig.urls.get("https://www.otto.de"), is(BrowserUtilsTest.getExpectedUrlConfigForOttoDe()));
        assertThat(jobConfig.urls.get("http://www.google.de"), is(BrowserUtilsTest.getExpectedUrlConfigForGoogleDe()));
        assertThat(jobConfig.pageLoadTimeout, is(60));
        assertThat(jobConfig.screenshotRetries, is(2));
    }

    @Test
    public void shouldNotSerializeDefaultCompareThreads() {
        String json = JobConfig.prettyPrint(JobConfig.defaultConfig());
        String yaml = JobConfig.prettyPrint(JobConfig.defaultConfig(), JacksonWrapper.ConfigFormat.YAML);

        assertThat(JobConfig.defaultConfig().compareThreads, is(0));
        assertThat(json, not(containsString("compare-threads")));
        assertThat(yaml, not(containsString("compare-threads")));
    }

    @Test
    public void shouldReadAndWriteCompareThreads() throws IOException {
        JobConfig original = JobConfig.copyOfBuilder(JobConfig.defaultConfig()).withCompareThreads(3).build();

        String json = JobConfig.prettyPrint(original);
        String yaml = JobConfig.prettyPrint(original, JacksonWrapper.ConfigFormat.YAML);

        assertThat(json, containsString("\"compare-threads\" : 3"));
        assertThat(yaml, containsString("compare-threads: 3"));
        assertThat(JacksonWrapper.deserializeConfig(new StringReader(json), JacksonWrapper.ConfigFormat.JSON).compareThreads, is(3));
        assertThat(JacksonWrapper.deserializeConfig(new StringReader(yaml), JacksonWrapper.ConfigFormat.YAML).compareThreads, is(3));
        assertThat(JobConfig.copyOfBuilder(original).build(), is(original));
        assertThat(JobConfig.copyOfBuilder(original).withCompareThreads(0).build(), not(original));
    }

    @Test
    public void shouldIgnoreCompareThreadsInOlderReaders() throws IOException {
        //configs without the option (and unknown options, which is what older versions see) must still be readable
        JobConfig jobConfig = JacksonWrapper.deserializeConfig(new StringReader("{ \"urls\": { \"https://www.example.com\": {} } }"), JacksonWrapper.ConfigFormat.JSON);

        assertThat(jobConfig.compareThreads, is(0));
    }

}
