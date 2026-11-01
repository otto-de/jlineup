package de.otto.jlineup.report;

import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Rules of 'ignore-image-content' in the ScreenshotsComparator, with the real ImageService and synthetic screenshots:
 * a white 40x30 page with an image element at x 5-25, y 5-20.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageContentComparisonTest {

    private static final int WIDTH = 40, HEIGHT = 30;
    private static final int[] IMAGE_AREA = {5, 5, 25, 20};
    private static final Color IMAGE_COLOR_BEFORE = new Color(100, 150, 200);
    private static final Color IMAGE_COLOR_AFTER = new Color(140, 110, 60);
    private static final Set<String> IMAGE_URLS = Set.of("https://i.example.com/teaser.webp");

    @Mock
    private FileService fileService;
    @Mock
    private FileTracker fileTracker;

    private ScreenshotContext screenshotContext;
    private ScreenshotsComparator testee;

    private BufferedImage beforeScreenshot, afterScreenshot, beforeImageless, afterImageless;
    private Set<String> imageUrlsBefore, imageUrlsAfter;
    private List<int[]> imageAreasBefore, imageAreasAfter;
    private boolean withImagelessScreenshots;

    private void setup(boolean ignoreImageContent) throws Exception {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withPath("/")
                .withWindowWidths(singletonList(WIDTH))
                .withIgnoreImageContent(ignoreImageContent)
                .build();
        RunStepConfig runStepConfig = RunStepConfig.runStepConfigBuilder().withWorkingDirectory("src/test/resources").withStep(RunStep.compare).build();
        JobConfig jobConfig = jobConfigBuilder().withUrls(ImmutableMap.of("http://url", urlConfig)).withWindowHeight(HEIGHT).build().insertDefaults();
        screenshotContext = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig).get(0);
        testee = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, new ImageService());

        when(fileService.getFileTracker()).thenReturn(fileTracker);
        when(fileService.getRelativePathFromReportDirToScreenshotsDir()).thenReturn("");
        when(fileTracker.getScreenshotsForContext(screenshotContext.contextHash())).thenReturn(
                ImmutableMap.of(0, ImmutableMap.of(before, "before.png", after, "after.png")));
        when(fileService.readScreenshot("before.png")).thenAnswer(i -> beforeScreenshot);
        when(fileService.readScreenshot("after.png")).thenAnswer(i -> afterScreenshot);
        when(fileService.readScreenshot("before_noimages.png")).thenAnswer(i -> beforeImageless);
        when(fileService.readScreenshot("after_noimages.png")).thenAnswer(i -> afterImageless);
        when(fileService.writeScreenshot(any(), any(), anyInt())).thenReturn("compare.png");
        when(fileService.writeImagelessScreenshot(any(), any(), anyInt())).thenReturn("compare_noimages.png");
        when(fileService.getRecordedContextFileTracker(screenshotContext.contextHash())).thenAnswer(i -> contextFileTracker());

        beforeScreenshot = page(IMAGE_COLOR_BEFORE);
        afterScreenshot = page(IMAGE_COLOR_AFTER);
        beforeImageless = page(null);
        afterImageless = page(null);
        imageUrlsBefore = IMAGE_URLS;
        imageUrlsAfter = IMAGE_URLS;
        imageAreasBefore = List.<int[]>of(IMAGE_AREA);
        imageAreasAfter = List.<int[]>of(IMAGE_AREA);
        withImagelessScreenshots = true;
    }

    private ScreenshotContextFileTracker contextFileTracker() {
        ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> imagelessScreenshots = new ConcurrentSkipListMap<>();
        if (withImagelessScreenshots) {
            imagelessScreenshots.put(0, Map.of(before, "before_noimages.png", after, "after_noimages.png"));
        }
        ConcurrentHashMap<BrowserStep, Set<String>> imageUrls = new ConcurrentHashMap<>();
        if (imageUrlsBefore != null) imageUrls.put(before, imageUrlsBefore);
        if (imageUrlsAfter != null) imageUrls.put(after, imageUrlsAfter);
        ConcurrentHashMap<BrowserStep, List<int[]>> areas = new ConcurrentHashMap<>();
        if (imageAreasBefore != null) areas.put(before, imageAreasBefore);
        if (imageAreasAfter != null) areas.put(after, imageAreasAfter);
        ConcurrentSkipListMap<Integer, Map<BrowserStep, List<int[]>>> imageAreas = new ConcurrentSkipListMap<>();
        imageAreas.put(0, areas);
        return ScreenshotContextFileTracker.screenshotContextFileTrackerBuilder()
                .withScreenshotContext(screenshotContext)
                .withImagelessScreenshots(imagelessScreenshots)
                .withImageUrls(imageUrls)
                .withImageAreas(imageAreas)
                .build();
    }

    //White page, the image area filled with the image color, or with a checkerboard (screenshot without images)
    private static BufferedImage page(Color imageColor) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.dispose();
        for (int y = IMAGE_AREA[1]; y < IMAGE_AREA[3]; y++) {
            for (int x = IMAGE_AREA[0]; x < IMAGE_AREA[2]; x++) {
                int checkerboard = (x + y) % 2 == 0 ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                image.setRGB(x, y, imageColor != null ? imageColor.getRGB() : checkerboard);
            }
        }
        return image;
    }

    private ScreenshotComparisonResult compare() throws Exception {
        return testee.compare().get("http://url").get(0);
    }

    @Test
    void shouldAcceptDifferencesInsideUnchangedImages() throws Exception {
        setup(true);

        ScreenshotComparisonResult result = compare();

        assertThat(result.difference(), is(0.0));
        assertThat(result.acceptedDifferentPixels(), is(20 * 15));
        assertThat(result.imageContentHint(), is(ImageContentHint.IMAGE_CONTENT_ACCEPTED));
        assertThat(result.imagelessScreenshotBeforeFileName(), is("before_noimages.png"));
        assertThat(result.imagelessScreenshotAfterFileName(), is("after_noimages.png"));
        assertThat(result.imagelessDifferenceImageFileName(), is(nullValue()));
    }

    @Test
    void shouldIgnoreScreenshotsWithoutImagesIfOptionIsDisabled() throws Exception {
        setup(false);

        ScreenshotComparisonResult result = compare();

        assertThat(result.difference(), greaterThan(0d));
        assertThat(result.imageContentHint(), is(nullValue()));
        assertThat(result.imagelessScreenshotBeforeFileName(), is(nullValue()));
        verify(fileService, never()).readScreenshot("before_noimages.png");
    }

    @Test
    void shouldStillDetectDifferencesOutsideOfImages() throws Exception {
        setup(true);
        afterScreenshot.setRGB(35, 25, Color.RED.getRGB());
        afterImageless.setRGB(35, 25, Color.RED.getRGB());

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.LAYOUT_CHANGED));
        assertThat(result.difference(), greaterThan(0d));
        assertThat(result.imagelessDifferenceImageFileName(), is("compare_noimages.png"));
    }

    @Test
    void shouldNotAcceptAnythingIfAnOverlayOnTopOfAnImageChanged() throws Exception {
        setup(true);
        //one pixel of i.e. text on top of the image, in the checkerboard of the screenshot without images
        afterImageless.setRGB(10, 10, afterImageless.getRGB(10, 10) == Color.BLACK.getRGB() ? Color.WHITE.getRGB() : Color.BLACK.getRGB());

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.LAYOUT_CHANGED));
        assertThat(result.difference(), greaterThan(0d));
    }

    @Test
    void shouldNotAcceptAnythingIfImageUrlsChanged() throws Exception {
        setup(true);
        imageUrlsAfter = Set.of("https://i.example.com/other.webp");

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.IMAGE_URLS_CHANGED));
        assertThat(result.difference(), greaterThan(0d));
    }

    @Test
    void shouldWriteDifferenceImageOfScreenshotsWithoutImagesEvenIfImageUrlsChanged() throws Exception {
        setup(true);
        imageUrlsAfter = Set.of("https://i.example.com/other.webp");
        //something outside of the images moved
        afterImageless.setRGB(35, 25, Color.RED.getRGB());

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.IMAGE_URLS_CHANGED));
        assertThat(result.imagelessDifferenceImageFileName(), is("compare_noimages.png"));
    }

    @Test
    void shouldWriteNoDifferenceImageOfScreenshotsWithoutImagesIfTheyAreEqual() throws Exception {
        setup(true);
        imageUrlsAfter = Set.of("https://i.example.com/other.webp");

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.IMAGE_URLS_CHANGED));
        assertThat(result.imagelessDifferenceImageFileName(), is(nullValue()));
    }

    @Test
    void shouldNotAcceptAnythingWithoutScreenshotsWithoutImages() throws Exception {
        setup(true);
        withImagelessScreenshots = false;

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.NO_IMAGE_DATA));
        assertThat(result.difference(), greaterThan(0d));
    }

    @Test
    void shouldNotAcceptAnythingWithoutImageAreas() throws Exception {
        setup(true);
        imageAreasAfter = null;

        ScreenshotComparisonResult result = compare();

        assertThat(result.imageContentHint(), is(ImageContentHint.NO_IMAGE_DATA));
        assertThat(result.difference(), greaterThan(0d));
    }

    @Test
    void shouldNotAcceptImagePixelsOutsideOfTheImageAreasOfBothRuns() throws Exception {
        setup(true);
        //i.e. an image that is only there in the before step
        imageAreasAfter = List.of();

        ScreenshotComparisonResult result = compare();

        assertThat(result.difference(), greaterThan(0d));
        assertThat(result.imageContentHint(), is(nullValue()));
    }

    @Test
    void shouldNeverReportMoreDifferenceThanWithoutTheOption() throws Exception {
        setup(true);
        afterScreenshot.setRGB(35, 25, Color.RED.getRGB());
        double withOption = compare().difference();

        setup(false);
        afterScreenshot.setRGB(35, 25, Color.RED.getRGB());
        double withoutOption = compare().difference();

        assertThat(withOption, lessThanOrEqualTo(withoutOption));
    }

    @Test
    void shouldPassIdenticalScreenshotsEvenIfImageUrlsChanged() throws Exception {
        setup(true);
        afterScreenshot = page(IMAGE_COLOR_BEFORE);
        imageUrlsAfter = Set.of("https://i.example.com/renamed.webp");

        ScreenshotComparisonResult result = compare();

        assertThat(result.difference(), is(0.0));
        assertThat(result.imageContentHint(), is(nullValue()));
        assertThat(result.imagelessScreenshotAfterFileName(), is("after_noimages.png"));
    }
}
