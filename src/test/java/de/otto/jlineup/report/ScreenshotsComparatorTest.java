package de.otto.jlineup.report;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.otto.jlineup.file.FileService.AFTER;
import static de.otto.jlineup.file.FileService.BEFORE;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScreenshotsComparatorTest {

    private ScreenshotsComparator testee;

    private Parameters parameters;
    private Config config;

    @Mock
    private FileService fileService;

    @Before
    public void setup() {
        initMocks(this);
        parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources/");

        config = new Config(
                ImmutableMap.of(
                        "http://url",
                        new UrlConfig(ImmutableList.of("/"), 0.05f, null, null, null, ImmutableList.of(1001), 10000, 2)),
                Browser.Type.CHROME,
                0f,
                1000);

        testee = new ScreenshotsComparator(parameters, config, fileService);
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
    public void shouldBuildPatternForBeforeImages() throws IOException {
        //when
        testee.getFilenamesForStep(parameters, "/", "http://url", BEFORE);
        //then
        verify(fileService).getFileNamesMatchingPattern(
                FileService.getScreenshotDirectory(parameters),
                "glob:**url_root_*_*_before.png");
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
        List<ScreenshotComparisonResult> expectedResults = ImmutableList.of(
                new ScreenshotComparisonResult("http://url/", 1001, 2002, 0.05604, "url_root_1001_02002_before.png", "url_root_1001_02002_after.png", "url_root_1001_02002_DIFFERENCE.png"),
                ScreenshotComparisonResult.noBeforeImageComparisonResult("http://url/", 1001, 3003, "url_root_1001_03003_after.png")
        );
        when(fileService.getFileNamesMatchingPattern(Paths.get("src/test/resources/screenshots"),
                "glob:**url_root_*_*_before.png")).thenReturn(ImmutableList.of("url_root_1001_02002_before.png"));
        when(fileService.getFileNamesMatchingPattern(Paths.get("src/test/resources/screenshots"),
                "glob:**url_root_*_*_after.png")).thenReturn(ImmutableList.of("url_root_1001_02002_after.png", "url_root_1001_03003_after.png"));
        when(fileService.readScreenshot(parameters, "url_root_1001_02002_before.png")).thenReturn(
                ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_02002_before.png")));
        when(fileService.readScreenshot(parameters, "url_root_1001_02002_after.png")).thenReturn(
                ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_02002_after.png")));

        //when
        List<ScreenshotComparisonResult> compare = testee.compare();

        //then
        assertThat(compare, is(expectedResults));
        verify(fileService).
                writeScreenshot(
                        eq("src/test/resources/screenshots/url_root_1001_02002_DIFFERENCE.png"),
                        any(BufferedImage.class));
    }
}
