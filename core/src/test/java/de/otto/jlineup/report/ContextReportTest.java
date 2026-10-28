package de.otto.jlineup.report;

import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.UrlConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.otto.jlineup.browser.BrowserStep.before;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class ContextReportTest {

    private final Summary summary = new Summary(false, 0d, 0d, 0);

    @Test
    void shouldCalculateDisplayWidthFromMeasuredScreenshotWidthAndDevicePixelRatio() {
        DeviceConfig deviceConfig = DeviceConfig.deviceConfigBuilder().withDeviceName("MOBILE").withWidth(320).withHeight(800).withPixelRatio(3f).build();

        ContextReport contextReport = contextReport(deviceConfig, 3d, 960);

        assertThat(contextReport.getScreenshotDisplayWidth(), is(320));
        assertThat(contextReport.getScreenshotMaxWidthStyle(), is("--screenshot-max-width: 320px"));
    }

    @Test
    void shouldCalculateDisplayWidthForSpecificMobileDeviceWithMeasuredValues() {
        DeviceConfig deviceConfig = DeviceConfig.deviceConfigBuilder().withDeviceName("iPhone X").build();

        ContextReport contextReport = contextReport(deviceConfig, 3d, 1125);

        assertThat(contextReport.getScreenshotDisplayWidth(), is(375));
    }

    @Test
    void shouldFallBackToConfiguredWidthWithoutMeasuredValues() {
        DeviceConfig deviceConfig = DeviceConfig.deviceConfigBuilder().withDeviceName("MOBILE").withWidth(320).withHeight(800).withPixelRatio(2f).build();

        ContextReport contextReport = contextReport(deviceConfig, null, null);

        assertThat(contextReport.getScreenshotDisplayWidth(), is(320));
        assertThat(contextReport.getScreenshotMaxWidthStyle(), is("--screenshot-max-width: 320px"));
    }

    @Test
    void shouldHaveNoDisplayWidthForSpecificMobileDeviceWithoutMeasuredValues() {
        DeviceConfig deviceConfig = DeviceConfig.deviceConfigBuilder().withDeviceName("iPhone X").build();

        ContextReport contextReport = contextReport(deviceConfig, null, null);

        assertThat(contextReport.getScreenshotDisplayWidth(), is(nullValue()));
        assertThat(contextReport.getScreenshotMaxWidthStyle(), is(nullValue()));
    }

    private ContextReport contextReport(DeviceConfig deviceConfig, Double devicePixelRatio, Integer screenshotWidth) {
        ScreenshotContext screenshotContext = ScreenshotContext.of("someurl", "somepath", deviceConfig, before, UrlConfig.urlConfigBuilder().build());
        return new ContextReport(screenshotContext.contextHash(), screenshotContext, summary, List.of(), false, devicePixelRatio, screenshotWidth);
    }
}
