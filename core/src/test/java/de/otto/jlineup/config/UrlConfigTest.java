package de.otto.jlineup.config;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlConfigTest {

    //TODO: Finish this test
    @Test
    void copyOfBuilderShouldCopyAllValues() {
        //Given
        UrlConfig originalConfig = UrlConfig.urlConfigBuilder()
                .withAlternatingCookies(ImmutableList.of(ImmutableList.of(new Cookie("name1", "value1")), ImmutableList.of(new Cookie("name2", "value2"))))
                .withCookies(ImmutableList.of(new Cookie("name", "value")))
                .withCleanupPaths(ImmutableList.of("path"))
                .withIgnoreAntiAliasing(true)
                .withJavaScript("javascript")
                .withPaths(ImmutableList.of("path"))
                .withDevices(ImmutableList.of(DeviceConfig.deviceConfig(1, 1)))
                .withWindowWidths(ImmutableList.of(1))
                .build();

        //When
        UrlConfig copiedConfig = UrlConfig.copyOfBuilder(originalConfig).build();

        //Then
        assertEquals(originalConfig, copiedConfig);

    }

    @Test
    void sanitizeShouldSanitize() {

    }
}