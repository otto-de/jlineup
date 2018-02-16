package de.otto.jlineup.report;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.config.Config.configBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScreenshotsComparatorTest {

    private static final int WINDOW_HEIGHT = 1000;

    private ScreenshotsComparator testee;

    private Parameters parameters;
    private Config config;

    @Mock
    private FileService fileService;

    @Mock
    private ImageService imageService;

    @Before
    public void setup() {
        initMocks(this);
        parameters = new Parameters();
        JCommander jCommander = new JCommander(parameters);
        jCommander.parse( "-d", "src/test/resources/");

        config = configBuilder()
                .withUrls(ImmutableMap.of(
                        "http://url",
                        new UrlConfig(ImmutableList.of("/"), 0.05f, null, null, null, null, ImmutableList.of(1001), 10000, 2, 0, 0, 0, null, 5)))
                .withWindowHeight(WINDOW_HEIGHT)
                .build();

        testee = new ScreenshotsComparator(parameters, config, fileService, imageService);
    }

    @Test
    public void shouldFindVerticalScrollPositionInImageFileName() throws Exception {
        String fileName = "url_root_1001_2002_after.png";
        int yPos = ScreenshotsComparator.extractVerticalScrollPositionFromFileName(fileName);
        assertThat(yPos, is(2002));
    }

    @Test
    public void shouldFindWindowWidthInImageFileName() throws Exception {
        String fileName = "url_root_1001_2002_after.png";
        int yPos = ScreenshotsComparator.extractWindowWidthFromFileName(fileName);
        assertThat(yPos, is(1001));
    }

    @Test
    public void shouldReplaceAfterWithBeforeInFilename() throws Exception {
        String filename = "url_root_1001_02002_after.png";
        String switchedFileName = ScreenshotsComparator.switchAfterWithBeforeInFileName(filename);
        assertThat(switchedFileName, is("url_root_1001_02002_before.png"));
    }

    @Test
    public void shouldBuildComparisonResults() throws Exception {
        //given
        final ImmutableMap<String, ImmutableList<ScreenshotComparisonResult>> expectedResults = ImmutableMap.of("http://url", ImmutableList.of(
                new ScreenshotComparisonResult(
                        "http://url/",
                        1001,
                        2002,
                        0.1337,
                        "screenshots/http_url_root_ff3c40c_1001_02002_before.png",
                        "screenshots/http_url_root_ff3c40c_1001_02002_after.png",
                        "screenshots/http_url_root_ff3c40c_1001_02002_DIFFERENCE.png"),
                ScreenshotComparisonResult.noBeforeImageComparisonResult(
                        "http://url/",
                        1001,
                        3003,
                        "screenshots/http_url_root_ff3c40c_1001_03003_after.png")
        ));

        when(fileService.getRelativePathFromReportDirToScreenshotsDir()).thenReturn("screenshots/");
        when(fileService.getFilenamesForStep("/", "http://url", "before")).thenReturn(ImmutableList.of("http_url_root_ff3c40c_1001_02002_before.png"));
        when(fileService.getFilenamesForStep("/", "http://url", "after")).thenReturn(ImmutableList.of("http_url_root_ff3c40c_1001_02002_after.png", "http_url_root_ff3c40c_1001_03003_after.png"));
        BufferedImage beforeBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        when(fileService.readScreenshot("http_url_root_ff3c40c_1001_02002_before.png")).thenReturn(
                beforeBuffer);
        BufferedImage afterBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_after.png"));
        when(fileService.readScreenshot("http_url_root_ff3c40c_1001_02002_after.png")).thenReturn(
                afterBuffer);


        BufferedImage differenceBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_DIFFERENCE_reference.png"));
        when(imageService.compareImages(beforeBuffer, afterBuffer, WINDOW_HEIGHT)).thenReturn(new ImageService.ImageComparisonResult(differenceBuffer, 0.1337d));

        when(fileService.writeScreenshot(differenceBuffer, "http://url", "/", 1001, 2002, "DIFFERENCE")).thenReturn("http_url_root_ff3c40c_1001_02002_DIFFERENCE.png");

        //when
        Map<String, List<ScreenshotComparisonResult>> comparisonResults = testee.compare();

        //then
        assertThat(comparisonResults, is(expectedResults));
        verify(fileService).
                writeScreenshot(
                        differenceBuffer,
                        "http://url",
                        "/",
                        1001,
                        2002,
                        "DIFFERENCE");
    }
}
