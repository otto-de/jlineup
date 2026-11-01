package de.otto.jlineup.file;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

    @Test
    void shouldSerializeAndDeserializeDataOfIgnoreImageContent() {
        FileTracker fileTracker = FileTracker.create(JobConfig.exampleConfig());
        fileTracker.addScreenshot(screenshotContext, "screenshot.png", 0);
        fileTracker.addImagelessScreenshot(screenshotContext, "screenshot_noimages.png", 0);
        fileTracker.setImageAreas(screenshotContext, 0, List.<int[]>of(new int[]{1, 2, 3, 4}));
        fileTracker.setImageUrls(screenshotContext, Set.of("https://i.example.com/b.webp", "https://i.example.com/a.webp"));

        String json = JacksonWrapper.serializeObject(fileTracker);
        FileTracker deserialized = JacksonWrapper.jsonMapper().readValue(json, FileTracker.class);

        ScreenshotContextFileTracker contextFileTracker = deserialized.getScreenshotContextFileTracker(screenshotContext.contextHash());
        assertThat(contextFileTracker.imagelessScreenshots.get(0).get(before), is("screenshot_noimages.png"));
        assertThat(contextFileTracker.imageAreas.get(0).get(before).get(0), is(new int[]{1, 2, 3, 4}));
        assertThat(List.copyOf(contextFileTracker.imageUrls.get(before)), is(List.of("https://i.example.com/a.webp", "https://i.example.com/b.webp")));
    }

    @Test
    void shouldNotSerializeDataOfIgnoreImageContentIfThereIsNone() {
        FileTracker fileTracker = FileTracker.create(JobConfig.exampleConfig());
        fileTracker.addScreenshot(screenshotContext, "screenshot.png", 0);

        String json = JacksonWrapper.serializeObject(fileTracker);
        FileTracker deserialized = JacksonWrapper.jsonMapper().readValue(json, FileTracker.class);

        assertThat(json, not(containsString("imageless")));
        assertThat(json, not(containsString("image-urls")));
        assertThat(json, not(containsString("image-areas")));
        assertThat(deserialized.getScreenshotContextFileTracker(screenshotContext.contextHash()).imagelessScreenshots.isEmpty(), is(true));
    }
}
