package de.otto.jlineup.browser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class ImageAreasTest {

    @Test
    void shouldConvertImageAreasToScreenshotPixels() {
        //given: CSS pixels relative to the viewport, as returned by getBoundingClientRect
        List<List<Number>> fromBrowser = List.of(List.of(10.4, 20, 30.2, 40.5));

        //when
        List<int[]> areas = Browser.toScreenshotAreas(fromBrowser, 2.0, 0, 800, 600);

        //then: rounded outwards
        assertThat(areas.size(), is(1));
        assertThat(areas.get(0), is(new int[]{20, 40, 61, 81}));
    }

    @Test
    void shouldShiftImageAreasOfTheCroppedLastScreenshotAndClipThem() {
        //given
        List<List<Number>> fromBrowser = List.of(List.of(-5, 90, 120, 150), List.of(0, 0, 10, 50));

        //when: the last screenshot was shifted upwards by 60 pixels
        List<int[]> areas = Browser.toScreenshotAreas(fromBrowser, 1.0, 60, 100, 80);

        //then: the first area is clipped to the screenshot, the second one is above it
        assertThat(areas.size(), is(1));
        assertThat(areas.get(0), is(new int[]{0, 30, 100, 80}));
    }

    @Test
    void shouldSkipInvalidImageAreas() {
        assertThat(Browser.toScreenshotAreas(null, 1.0, 0, 100, 100), is(empty()));
        assertThat(Browser.toScreenshotAreas("no list", 1.0, 0, 100, 100), is(empty()));
        assertThat(Browser.toScreenshotAreas(List.of(List.of(1, 2, 3), List.of(5, 5, 5, 5)), 1.0, 0, 100, 100), is(empty()));
    }

    @Test
    void shouldContainAllImageKindsInTheScripts() {
        for (String script : List.of(ImageContentScripts.HIDE_IMAGES, ImageContentScripts.COLLECT_IMAGE_URLS, ImageContentScripts.COLLECT_IMAGE_AREAS)) {
            assertThat(List.of(script.contains("::before"), script.contains("shadowRoot"), script.contains("backgroundImage"), script.contains("borderImageSource")),
                    contains(true, true, true, true));
        }
    }
}
