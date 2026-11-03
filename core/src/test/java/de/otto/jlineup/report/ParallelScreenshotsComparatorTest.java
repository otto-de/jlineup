package de.otto.jlineup.report;

import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.IgnoreCompressionArtifactsConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Compares real screenshot files, with the real ImageService, with different numbers of compare threads.
 */
class ParallelScreenshotsComparatorTest {

    private static final int WIDTH = 60, HEIGHT = 40;

    @TempDir
    Path tempDir;

    private RunStepConfig runStepConfig;
    private FileService fileService;
    private JobConfig baseJobConfig;

    @BeforeEach
    void setup() throws Exception {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withPaths(List.of(de.otto.jlineup.config.PathConfig.of("/a"), de.otto.jlineup.config.PathConfig.of("/b"), de.otto.jlineup.config.PathConfig.of("/c")))
                .withWindowWidths(List.of(WIDTH, WIDTH + 10))
                .withAlternatingCookies(List.of(List.of(new Cookie("ab", "a")), List.of(new Cookie("ab", "b")), List.of(new Cookie("ab", "c"))))
                .build();
        UrlConfig secondUrlConfig = UrlConfig.urlConfigBuilder()
                .withPath("/")
                .withWindowWidths(List.of(WIDTH))
                .withIgnoreCompressionArtifacts(IgnoreCompressionArtifactsConfig.ENABLED)
                .build();
        baseJobConfig = jobConfigBuilder()
                .withUrls(ImmutableMap.of("http://one", urlConfig, "http://two", secondUrlConfig))
                .withWindowHeight(HEIGHT)
                .build()
                .insertDefaults();

        runStepConfig = RunStepConfig.runStepConfigBuilder()
                .withWorkingDirectory(tempDir.toString())
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .withStep(RunStep.after)
                .build();
        fileService = new FileService(RunStepConfig.copyOfBuilder(runStepConfig).withStep(RunStep.before).build(), baseJobConfig);
        Files.createDirectories(tempDir.resolve("screenshots"));
        Files.createDirectories(tempDir.resolve("report"));

        writeScreenshots();
    }

    //Every third screenshot is identical, every third differs a bit and every third has no 'after' screenshot
    private void writeScreenshots() throws Exception {
        Random random = new Random(42);
        int index = 0;
        for (ScreenshotContext context : BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, baseJobConfig)) {
            for (int yPosition : new int[]{0, 40, 80}) {
                BufferedImage beforeImage = noise(random);
                fileService.writeScreenshot(withStep(context, before), beforeImage, yPosition);
                switch (index++ % 3) {
                    case 0 -> fileService.writeScreenshot(withStep(context, after), beforeImage, yPosition);
                    case 1 -> {
                        BufferedImage afterImage = copy(beforeImage);
                        afterImage.setRGB(random.nextInt(WIDTH), random.nextInt(HEIGHT), 0xFF123456);
                        afterImage.setRGB(random.nextInt(WIDTH), random.nextInt(HEIGHT), 0xFF654321);
                        fileService.writeScreenshot(withStep(context, after), afterImage, yPosition);
                    }
                    default -> {
                    }
                }
            }
        }
    }

    private static ScreenshotContext withStep(ScreenshotContext context, BrowserStep step) {
        return ScreenshotContext.copyOfBuilder(context).withStep(step).build();
    }

    private static BufferedImage noise(Random random) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        return image;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        copy.getGraphics().drawImage(image, 0, 0, null);
        return copy;
    }

    private Map<String, List<ScreenshotComparisonResult>> compare(int compareThreads) throws IOException {
        JobConfig jobConfig = JobConfig.copyOfBuilder(baseJobConfig).withCompareThreads(compareThreads).build();
        return new ScreenshotsComparator(runStepConfig, jobConfig, fileService, new ImageService()).compare();
    }

    @Test
    void shouldGiveSameResultsWithOneAndManyThreads() throws Exception {
        Map<String, List<ScreenshotComparisonResult>> sequential = compare(1);

        assertThat(sequential.get("http://one").size(), is(2 * 3 * 3 * 3));
        assertThat(sequential.get("http://two").size(), is(3));
        //identical, changed and incomplete screenshots are all in the results
        assertThat(sequential.get("http://one").stream().filter(r -> r.difference() == 0 && r.differenceImageFileName() == null && r.screenshotAfterFileName() != null).count(), greaterThan(0L));
        assertThat(sequential.get("http://one").stream().filter(r -> r.difference() > 0).count(), greaterThan(0L));
        assertThat(sequential.get("http://one").stream().filter(r -> r.screenshotAfterFileName() == null).count(), greaterThan(0L));

        for (int run = 0; run < 5; run++) {
            assertThat("run " + run, compare(8), is(sequential));
        }
        assertThat(compare(0), is(sequential));
    }

    @Test
    void shouldKeepOrderOfContextsWithSameSortKeys() throws Exception {
        List<ScreenshotComparisonResult> sequential = compare(1).get("http://one");
        List<String> contextHashes = sequential.stream().map(ScreenshotComparisonResult::contextHash).toList();

        for (int run = 0; run < 5; run++) {
            assertThat(compare(8).get("http://one").stream().map(ScreenshotComparisonResult::contextHash).toList(), is(contextHashes));
        }
    }

    @Test
    void shouldWriteDifferenceImagesFromAllThreads() throws Exception {
        List<ScreenshotComparisonResult> results = compare(8).get("http://one");

        List<ScreenshotComparisonResult> different = results.stream().filter(r -> r.differenceImageFileName() != null).toList();
        assertThat(different.size(), greaterThan(0));
        for (ScreenshotComparisonResult result : different) {
            File differenceImage = tempDir.resolve("screenshots").resolve(result.differenceImageFileName().replaceFirst("^.*screenshots/", "")).toFile();
            assertThat(differenceImage.getPath(), differenceImage.exists(), is(true));
            assertThat(ImageIO.read(differenceImage), is(notNullValue()));
        }
    }

    @Test
    void shouldPropagateExceptionAndStopWorkers() throws Exception {
        JobConfig failingJobConfig = JobConfig.copyOfBuilder(baseJobConfig).withCompareThreads(8).build();
        ImageService failingImageService = new ImageService() {
            @Override
            public ImageComparisonResult compareImages(BufferedImage image1, BufferedImage image2, int viewportHeight, boolean ignoreAntiAliased, double maxAntiAliasColorDistance, boolean strictColorComparison, double maxColorDistance, de.otto.jlineup.image.CompressionArtifactDetector compressionArtifactDetector) {
                throw new IllegalStateException("compare failed");
            }
        };
        long threadsBefore = compareThreadCount();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new ScreenshotsComparator(runStepConfig, failingJobConfig, fileService, failingImageService).compare());

        assertThat(exception.getMessage(), is("compare failed"));
        for (int i = 0; i < 50 && compareThreadCount() > threadsBefore; i++) {
            Thread.sleep(100);
        }
        assertThat(compareThreadCount(), is(threadsBefore));
    }

    private static long compareThreadCount() {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().startsWith("CompareThread-") && t.isAlive()).count();
    }

    @Test
    void shouldRunWithSharedCompareLimit() throws Exception {
        Map<String, List<ScreenshotComparisonResult>> sequential = compare(1);
        Semaphore compareSlots = new Semaphore(2);
        JobConfig jobConfig = JobConfig.copyOfBuilder(baseJobConfig).withCompareThreads(8).build();

        Map<String, List<ScreenshotComparisonResult>> limited = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, new ImageService(), compareSlots).compare();

        assertThat(limited, is(sequential));
        assertThat(compareSlots.availablePermits(), is(2));
    }

    @Test
    void shouldCalculateEffectiveThreads() {
        long gb = 1024L * 1024 * 1024;
        //992x1000 needs about 40 MB per screenshot
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(0, 10, 748, 992_000, 8 * gb), is(10));
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(4, 10, 748, 992_000, 8 * gb), is(4));
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(0, 10, 3, 992_000, 8 * gb), is(3));
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(0, 10, 748, 992_000, 200 * 1024 * 1024), is(2));
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(0, 10, 748, 992_000, 1024 * 1024), is(1));
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(0, 0, 748, 0, 8 * gb), is(1));
        assertThat(ScreenshotsComparator.calculateEffectiveThreads(0, 10, 0, 992_000, 8 * gb), is(1));
    }
}
