package de.otto.jlineup.config;

import org.junit.jupiter.api.Test;

import static de.otto.jlineup.config.JobConfig.EXAMPLE_URL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ConfigMergerTest {

    @Test
    void shouldMergeIdenticalExampleConfigs() {

        //Given
        JobConfig jobConfig = JobConfig.exampleConfig();
        JobConfig mergeConfig = JobConfig.exampleConfig();

        UrlConfig urlConfig = jobConfig.urls.get(EXAMPLE_URL);
        //Javascript is merged by concatenating with ";"
        UrlConfig expectedUrlConfig = UrlConfig.copyOfBuilder(urlConfig).withJavaScript(urlConfig.javaScript + ";" + urlConfig.javaScript).build();
        JobConfig expectedConfig = JobConfig.copyOfBuilder(jobConfig).withUrls(null).addUrlConfig(expectedUrlConfig.url, expectedUrlConfig).build();

        //When
        JobConfig resultingConfig = ConfigMerger.mergeJobConfigWithMergeConfig(jobConfig, mergeConfig).insertDefaults();

        //Then
        assertThat(resultingConfig, is(expectedConfig));

    }

    @Test
    void shouldMergeReportLabelsWithPrecedenceOfLocalConfig() {

        //Given
        JobConfig jobConfig = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withBeforeLabel("Local before").withAfterLabel(null).build();
        JobConfig mergeConfig = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withBeforeLabel("Merged before").withAfterLabel("Merged after").build();

        //When
        JobConfig resultingConfig = ConfigMerger.mergeJobConfigWithMergeConfig(jobConfig, mergeConfig);

        //Then
        assertThat(resultingConfig.beforeLabel, is("Local before"));
        assertThat(resultingConfig.afterLabel, is("Merged after"));
    }

    @Test
    void shouldMergeCompareThreads() {
        JobConfig local = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withCompareThreads(2).build();
        JobConfig unset = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).build();
        JobConfig merge = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withCompareThreads(6).build();

        assertThat(ConfigMerger.mergeJobConfigWithMergeConfig(local, merge).compareThreads, is(2));
        assertThat(ConfigMerger.mergeJobConfigWithMergeConfig(unset, merge).compareThreads, is(6));
    }

    @Test
    void shouldMergeIgnoreCompressionArtifacts() {
        IgnoreCompressionArtifactsConfig local = new IgnoreCompressionArtifactsConfig(true, 12, JobConfig.DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE, JobConfig.DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE);
        IgnoreCompressionArtifactsConfig merge = new IgnoreCompressionArtifactsConfig(false, 30, 15, 0.15);

        IgnoreCompressionArtifactsConfig merged = mergedIgnoreCompressionArtifacts(local, merge);

        assertThat(merged, is(new IgnoreCompressionArtifactsConfig(true, 12, 15, 0.15)));
    }

    @Test
    void shouldEnableIgnoreCompressionArtifactsIfOnlyTheMergeConfigEnablesIt() {
        assertThat(mergedIgnoreCompressionArtifacts(IgnoreCompressionArtifactsConfig.DISABLED, IgnoreCompressionArtifactsConfig.ENABLED), is(IgnoreCompressionArtifactsConfig.ENABLED));
        assertThat(mergedIgnoreCompressionArtifacts(IgnoreCompressionArtifactsConfig.ENABLED, IgnoreCompressionArtifactsConfig.DISABLED), is(IgnoreCompressionArtifactsConfig.ENABLED));
        assertThat(mergedIgnoreCompressionArtifacts(IgnoreCompressionArtifactsConfig.DISABLED, IgnoreCompressionArtifactsConfig.DISABLED), is(IgnoreCompressionArtifactsConfig.DISABLED));
    }

    @Test
    void shouldMergeHoverElementAndHoverWaitMs() {
        UrlConfig localUrlConfig = UrlConfig.copyOfBuilder(JobConfig.exampleConfig().urls.get(EXAMPLE_URL))
                .withHoverElement(".local-hover")
                .withHoverWaitMs(250)
                .build();
        UrlConfig mergeUrlConfig = UrlConfig.copyOfBuilder(JobConfig.exampleConfig().urls.get(EXAMPLE_URL))
                .withHoverElement(".merge-hover")
                .withHoverWaitMs(500)
                .build();

        JobConfig local = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withUrls(null).addUrlConfig(EXAMPLE_URL, localUrlConfig).build();
        JobConfig merge = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withUrls(null).addUrlConfig(EXAMPLE_URL, mergeUrlConfig).build();

        JobConfig merged = ConfigMerger.mergeJobConfigWithMergeConfig(local, merge);
        assertThat(merged.urls.get(EXAMPLE_URL).hoverElement, is(".local-hover"));
        assertThat(merged.urls.get(EXAMPLE_URL).hoverWaitMs, is(250));

        UrlConfig unsetLocalUrlConfig = UrlConfig.copyOfBuilder(JobConfig.exampleConfig().urls.get(EXAMPLE_URL)).build();
        JobConfig unsetLocal = JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withUrls(null).addUrlConfig(EXAMPLE_URL, unsetLocalUrlConfig).build();

        JobConfig mergedWithUnset = ConfigMerger.mergeJobConfigWithMergeConfig(unsetLocal, merge);
        assertThat(mergedWithUnset.urls.get(EXAMPLE_URL).hoverElement, is(".merge-hover"));
        assertThat(mergedWithUnset.urls.get(EXAMPLE_URL).hoverWaitMs, is(500));
    }

    private static IgnoreCompressionArtifactsConfig mergedIgnoreCompressionArtifacts(IgnoreCompressionArtifactsConfig local, IgnoreCompressionArtifactsConfig merge) {
        JobConfig localConfig = withIgnoreCompressionArtifacts(local);
        JobConfig mergeConfig = withIgnoreCompressionArtifacts(merge);
        return ConfigMerger.mergeJobConfigWithMergeConfig(localConfig, mergeConfig).urls.get(EXAMPLE_URL).ignoreCompressionArtifacts;
    }

    private static JobConfig withIgnoreCompressionArtifacts(IgnoreCompressionArtifactsConfig config) {
        UrlConfig urlConfig = UrlConfig.copyOfBuilder(JobConfig.exampleConfig().urls.get(EXAMPLE_URL)).withIgnoreCompressionArtifacts(config).build();
        return JobConfig.copyOfBuilder(JobConfig.exampleConfig()).withUrls(null).addUrlConfig(EXAMPLE_URL, urlConfig).build();
    }
}
