package de.otto.jlineup.file;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import org.junit.jupiter.api.Test;

import static de.otto.jlineup.browser.BrowserStep.before;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

class FileTrackerTest {

    private final ScreenshotContext screenshotContext = ScreenshotContext.of("someurl", "somepath", DeviceConfig.deviceConfig(320, 800), before, UrlConfig.urlConfigBuilder().build());

    @Test
    void shouldSerializeAndDeserializeScreenshotDimensions() {
        FileTracker fileTracker = FileTracker.create(JobConfig.exampleConfig());
        fileTracker.addScreenshot(screenshotContext, "screenshot.png", 0);
        fileTracker.setScreenshotDimensions(screenshotContext, 3d, 960);

        String json = JacksonWrapper.serializeObject(fileTracker);
        FileTracker deserialized = JacksonWrapper.jsonMapper().readValue(json, FileTracker.class);

        ScreenshotContextFileTracker contextFileTracker = deserialized.getScreenshotContextFileTracker(screenshotContext.contextHash());
        assertThat(json, containsString("\"device-pixel-ratio\" : 3.0"));
        assertThat(json, containsString("\"screenshot-width\" : 960"));
        assertThat(contextFileTracker.getDevicePixelRatio(), is(3d));
        assertThat(contextFileTracker.getScreenshotWidth(), is(960));
    }

    @Test
    void shouldHandleFileTrackerWithoutScreenshotDimensions() {
        FileTracker fileTracker = FileTracker.create(JobConfig.exampleConfig());
        fileTracker.addScreenshot(screenshotContext, "screenshot.png", 0);

        String json = JacksonWrapper.serializeObject(fileTracker);
        FileTracker deserialized = JacksonWrapper.jsonMapper().readValue(json, FileTracker.class);

        ScreenshotContextFileTracker contextFileTracker = deserialized.getScreenshotContextFileTracker(screenshotContext.contextHash());
        assertThat(json, not(containsString("device-pixel-ratio")));
        assertThat(contextFileTracker.getDevicePixelRatio(), is(nullValue()));
        assertThat(contextFileTracker.getScreenshotWidth(), is(nullValue()));
    }

    @Test
    void shouldIgnoreScreenshotDimensionsForUnknownContext() {
        FileTracker fileTracker = FileTracker.create(JobConfig.exampleConfig());

        fileTracker.setScreenshotDimensions(screenshotContext, 3d, 960);

        assertThat(fileTracker.getScreenshotContextFileTracker(screenshotContext.contextHash()), is(nullValue()));
    }
}
