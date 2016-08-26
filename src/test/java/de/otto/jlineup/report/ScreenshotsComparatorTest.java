package de.otto.jlineup.report;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.ComparisonResult;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.image.ImageUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.image.ImageUtils.AFTER;
import static de.otto.jlineup.image.ImageUtils.BEFORE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ScreenshotsComparatorTest {

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
    public void shouldFindBeforeImagesInDirectory() throws IOException {
        Parameters parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources/");
        final List<String> beforeFileNames = ScreenshotsComparator.getFilenamesForStep(parameters, "/", "http://url", BEFORE);
        assertThat(beforeFileNames, is(Arrays.asList("url_root_1001_2002_before.png")));
    }

    @Test
    public void shouldFindAfterImagesInDirectory() throws IOException {
        Parameters parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources/");
        final List<String> beforeFileNames = ScreenshotsComparator.getFilenamesForStep(parameters, "/", "http://url", AFTER);
        assertThat(beforeFileNames, is(Arrays.asList("url_root_1001_2002_after.png", "url_root_1001_3003_after.png")));
    }

    @Test
    public void shouldReplaceAfterWithBeforeInFilename() throws Exception {
        String filename = "url_root_1001_2002_after.png";
        String switchedFileName = ScreenshotsComparator.switchAfterWithBeforeInFileName(filename);
        assertThat(switchedFileName, is("url_root_1001_2002_before.png"));
    }

    @Test
    public void shouldBuildComparisonResults() throws Exception {
        //given
        Parameters parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources/");
        Config config = new Config(
                ImmutableMap.of(
                        "http://url",
                        new UrlConfig(ImmutableList.of("/"), 0.05f, null, null, null, ImmutableList.of(1001), 10000, 2)),
                Browser.Type.CHROME,
                0f,
                1000);
        DifferenceFileWriter mockDifferenceFileWriter = mock(DifferenceFileWriter.class);
        ScreenshotsComparator testee = new ScreenshotsComparator(parameters, config, mockDifferenceFileWriter);

        List<ComparisonResult> expectedResults = ImmutableList.of(
                new ComparisonResult("http://url/", 1001, 2002, 0.05604, "url_root_1001_2002_before.png", "url_root_1001_2002_after.png", "url_root_1001_2002_DIFFERENCE.png"),
                ComparisonResult.noBeforeImageComparisonResult("http://url/", 1001, 3003, "url_root_1001_3003_after.png")
        );

        //when
        List<ComparisonResult> compare = testee.compare();

        //then
        assertThat(compare, is(expectedResults));
        verify(mockDifferenceFileWriter).writeDifferenceFile(eq("src/test/resources/screenshots/url_root_1001_2002_DIFFERENCE.png"), any(ImageUtils.BufferedImageComparisonResult.class));
    }
}
