package de.otto.jlineup.report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.*;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.image.ImageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_COLOR_DISTANCE;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScreenshotsComparatorTest {

    private static final int WINDOW_HEIGHT = 1000;

    private ScreenshotsComparator testee;

    private RunStepConfig runStepConfig;
    private JobConfig jobConfig;

    private final UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
            .withPath("/")
            .withMaxDiff(0.05d)
            .withWindowWidths(singletonList(100))
            .withMaxScrollHeight(10000)
            .withWaitAfterPageLoad(2)
            .build();

    @Mock
    private FileService fileService;

    @Mock
    private FileTracker fileTracker;

    @Mock
    private ImageService imageService;

    @Before
    public void setup() {
        initMocks(this);
        runStepConfig = RunStepConfig.runStepConfigBuilder().withWorkingDirectory("src/test/resources").withStep(RunStep.compare).build();
        jobConfig = jobConfigBuilder()
                .withUrls(ImmutableMap.of(
                        "http://url", urlConfig,
                        "http://url2", urlConfig
                ))
                .withWindowHeight(WINDOW_HEIGHT)
                .build();

        testee = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, imageService);
    }

    @Test
    public void shouldBuildComparisonResults() throws Exception {
        //given
        ScreenshotContext screenshotContext = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig).get(0);
        ScreenshotContext screenshotContext2 = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig).get(1);
        DeviceConfig givenDeviceConfig = DeviceConfig.deviceConfig(100, WINDOW_HEIGHT);

        final ImmutableMap<String, ImmutableList<ScreenshotComparisonResult>> expectedResults = ImmutableMap.of("http://url", ImmutableList.of(
                new ScreenshotComparisonResult(
                        screenshotContext.contextHash(),
                        "http://url/",
                        givenDeviceConfig,
                        2002,
                        0.1337,
                        "screenshots/http_url_root_ff3c40c_1001_02002_before.png",
                        "screenshots/http_url_root_ff3c40c_1001_02002_after.png",
                        "screenshots/http_url_root_ff3c40c_1001_02002_compare.png",
                        10),
                ScreenshotComparisonResult.noBeforeImageComparisonResult(
                        screenshotContext.contextHash(),
                        "http://url/",
                        givenDeviceConfig,
                        3003,
                        "screenshots/http_url_root_ff3c40c_1001_03003_after.png")
        ),"http://url2", ImmutableList.of(
                new ScreenshotComparisonResult(
                        screenshotContext2.contextHash(),
                        "http://url2/",
                        givenDeviceConfig,
                        2002,
                        0.1337,
                        "screenshots/http_url2_root_ff3c40c_1001_02002_before.png",
                        "screenshots/http_url2_root_ff3c40c_1001_02002_after.png",
                        "screenshots/http_url2_root_ff3c40c_1001_02002_compare.png",
                        10),
                ScreenshotComparisonResult.noBeforeImageComparisonResult(
                        screenshotContext2.contextHash(),
                        "http://url2/",
                        givenDeviceConfig,
                        3003,
                        "screenshots/http_url2_root_ff3c40c_1001_03003_after.png")
        ));

        when(fileService.getFileTracker()).thenReturn(fileTracker);
        when(fileService.getRelativePathFromReportDirToScreenshotsDir()).thenReturn("screenshots/");

        when(fileTracker.getScreenshotsForContext(screenshotContext.contextHash())).thenReturn(
                ImmutableMap.of(2002, ImmutableMap.of(before, "http_url_root_ff3c40c_1001_02002_before.png",
                        after, "http_url_root_ff3c40c_1001_02002_after.png"),
                        3003, ImmutableMap.of(after, "http_url_root_ff3c40c_1001_03003_after.png")));
        when(fileTracker.getScreenshotsForContext(screenshotContext2.contextHash())).thenReturn(
                ImmutableMap.of(2002, ImmutableMap.of(before, "http_url2_root_ff3c40c_1001_02002_before.png",
                        after, "http_url2_root_ff3c40c_1001_02002_after.png"),
                        3003, ImmutableMap.of(after, "http_url2_root_ff3c40c_1001_03003_after.png")));

        BufferedImage beforeBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        when(fileService.readScreenshot("http_url_root_ff3c40c_1001_02002_before.png")).thenReturn(
                beforeBuffer);

        BufferedImage beforeBuffer2 = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        when(fileService.readScreenshot("http_url2_root_ff3c40c_1001_02002_before.png")).thenReturn(
                beforeBuffer2);

        BufferedImage afterBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_after.png"));
        when(fileService.readScreenshot("http_url_root_ff3c40c_1001_02002_after.png")).thenReturn(
                afterBuffer);

        BufferedImage afterBuffer2 = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_after.png"));
        when(fileService.readScreenshot("http_url2_root_ff3c40c_1001_02002_after.png")).thenReturn(
                afterBuffer2);

        BufferedImage differenceBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_DIFFERENCE_reference.png"));
        when(imageService.compareImages(beforeBuffer, afterBuffer, WINDOW_HEIGHT, false, false, DEFAULT_MAX_COLOR_DISTANCE)).thenReturn(new ImageService.ImageComparisonResult(differenceBuffer, 0.1337d, 10));
        when(fileService.writeScreenshot(screenshotContext, differenceBuffer, 2002)).thenReturn("http_url_root_ff3c40c_1001_02002_compare.png");

        BufferedImage differenceBuffer2 = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_DIFFERENCE_reference.png"));
        when(imageService.compareImages(beforeBuffer2, afterBuffer2, WINDOW_HEIGHT, false, false, DEFAULT_MAX_COLOR_DISTANCE)).thenReturn(new ImageService.ImageComparisonResult(differenceBuffer2, 0.1337d, 10));
        when(fileService.writeScreenshot(screenshotContext2, differenceBuffer2, 2002)).thenReturn("http_url2_root_ff3c40c_1001_02002_compare.png");

        //when
        Map<String, List<ScreenshotComparisonResult>> comparisonResults = testee.compare();

        //then
        assertThat(comparisonResults, is(expectedResults));
    }
}
