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
}