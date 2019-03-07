package de.otto.jlineup.config;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.exceptions.ValidationError;
import org.junit.Test;

import java.util.Arrays;

import static de.otto.jlineup.config.DeviceConfig.deviceConfigBuilder;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JobConfigValidatorTest {

    @Test
    public void shouldDenyMobileEmulationWhenUsingOtherBrowserThanChrome() {
        // given
        JobConfig jobConfig = jobConfigBuilder()
                .addUrlConfig("someUrl", urlConfigBuilder()
                        .withWindowWidths(null)
                        .addDeviceConfig(deviceConfigBuilder()
                                .withDeviceName("MOBILE")
                                .build())
                        .build())
                .withBrowser(Browser.Type.FIREFOX)
                .build();

        // when
        try {
            JobConfigValidator.validateJobConfig(jobConfig);
            fail("Expected validation error");

        // then
        } catch(ValidationError e) {
            assertThat(e.getMessage(), containsString("Mobile emulation is only supported by Chrome"));
        }
    }

    @Test
    public void shouldDenyMixedWindowWidthsAndDevices() {
        // given
        JobConfig jobConfig = jobConfigBuilder()
                .addUrlConfig("someUrl", urlConfigBuilder()
                        .withWindowWidths(Arrays.asList(200,300,400))
                        .addDeviceConfig(deviceConfigBuilder()
                                .withDeviceName("MOBILE")
                                .build())
                        .build())
                .withBrowser(Browser.Type.FIREFOX)
                .build();

        // when
        try {
            JobConfigValidator.validateJobConfig(jobConfig);
            fail("Expected validation error");

            // then
        } catch(ValidationError e) {
            assertThat(e.getMessage(), containsString("window-widths"));
        }
    }

    @Test
    public void shouldDenyUserAgentWhenSpecialDeviceNameIsSpecified() {
        // given
        JobConfig jobConfig = jobConfigBuilder()
                .addUrlConfig("someUrl", urlConfigBuilder()
                        .withWindowWidths(null)
                        .addDeviceConfig(deviceConfigBuilder()
                                .withDeviceName("iPhone X")
                                .withUserAgent("someUserAgent")
                                .build())
                        .build())
                .withBrowser(Browser.Type.CHROME)
                .build();

        // when
        try {
            JobConfigValidator.validateJobConfig(jobConfig);
            fail("Expected validation error");

            // then
        } catch(ValidationError e) {
            assertThat(e.getMessage(), containsString("overridden"));
        }
    }

}