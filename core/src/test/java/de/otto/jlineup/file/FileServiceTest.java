package de.otto.jlineup.file;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Step;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.RunStepConfig.jLineupRunConfigurationBuilder;
import static de.otto.jlineup.file.FileService.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class FileServiceTest {

    private FileService testee;

    private RunStepConfig runStepConfig;

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private String tempDirPath;
    private String writeScreenshotTestPath;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        tempDirPath = tempDir.getRoot().getPath();

        writeScreenshotTestPath = tempDirPath + "/testdirforlineupwritetest";

        runStepConfig = jLineupRunConfigurationBuilder()
                .withWorkingDirectory(writeScreenshotTestPath)
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .build();

        testee = new FileService(runStepConfig);
        testee.createDirIfNotExists(writeScreenshotTestPath);
        testee.createDirIfNotExists(writeScreenshotTestPath + "/screenshots");
        testee.createDirIfNotExists(writeScreenshotTestPath + "/report");

    }

    @Test
    public void shouldCreateDirectory() throws IOException {
        //when
        testee.createDirIfNotExists(tempDirPath + "/testdirforlineuptest");
        //then
        assertThat(Files.isDirectory(Paths.get(tempDirPath + "/testdirforlineuptest")), is(true));
    }

    @Test
    public void shouldWriteScreenshot() throws IOException {
        BufferedImage bufferedImage = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, 200);

        String fileName = testee.writeScreenshot(bufferedImage, "http://someurl", "somePath", 999, 777, "someStep");

        assertThat(Files.exists(Paths.get(fileName)), is(true));
    }

    @Test
    public void shouldWriteJsonReport() throws Exception {

        testee.writeJsonReport("[{\"toll\":\"mega\"}]");

        Path reportFilePath = Paths.get(writeScreenshotTestPath + "/report/report.json");
        assertThat(Files.exists(reportFilePath), is(true));
        List<String> reportFileContents = Files.readAllLines(reportFilePath);
        assertThat(reportFileContents.get(0), is("[{\"toll\":\"mega\"}]"));
    }

    @Test
    public void shouldGenerateFullPathToPngFile() {
        final String fullFileNameWithPath = testee.getScreenshotPath("testurl", "/", 1001, 2002, "step");

        assertThat(fullFileNameWithPath, is(writeScreenshotTestPath + FILE_SEPARATOR + "screenshots" + FILE_SEPARATOR + "testurl_root_bbf1812_1001_02002_step.png"));
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "multi-media#anchor?one=two&three=fo_ur", 1000, 2000, "after");
        assertThat(outputString, is("https_www_otto_de_multi-media_anchor_one_two_three_fo_ur_99698cd_1000_02000_after.png"));
    }

    @Test
    public void shouldGenerateFilenameWithAMaxLenghtOf255Bytes() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "multi-media#anchor?abcdefghijklmnopqrstuvwxyz=12345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890", 1000, 2000, "after");
        assertThat(outputString.length(), Matchers.lessThan(255));
        assertThat(outputString, is("https_www_otto_de_multi-media_anchor_abcdefghijklmnopqrstuvwxyz_12345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567_559e477_1000_02000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("https_www_otto_de_root_baa15be_1000_02000_before.png"));
    }

    @Test
    public void shouldFindAfterImagesInDirectoryWithPattern() throws IOException {
        List<String> fileNamesMatchingPattern = testee.getFileNamesMatchingPattern(Paths.get("src/test/resources/screenshots"), "glob:**http_url_root_*_*_after.png");
        assertThat(fileNamesMatchingPattern, is(Arrays.asList("http_url_root_ff3c40c_1001_02002_after.png", "http_url_root_ff3c40c_1001_03003_after.png")));
    }

    @Test
    public void shouldFindBeforeImages() throws IOException {

        runStepConfig = jLineupRunConfigurationBuilder()
                .withWorkingDirectory("src/test/resources")
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .build();
        FileService fileService = new FileService(runStepConfig);

        //when
        List<String> beforeFiles = fileService.getFilenamesForStep("/", "http://url", Step.before.name());
        //then
        assertThat(beforeFiles, is(ImmutableList.of("http_url_root_ff3c40c_1001_02002_before.png")));
    }

    @Test
    public void shouldFindAfterImages() throws IOException {
        runStepConfig = jLineupRunConfigurationBuilder()
                .withWorkingDirectory("src/test/resources")
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .build();
        FileService fileService = new FileService(runStepConfig);

        //when
        List<String> afterFiles = fileService.getFilenamesForStep("/", "http://url", Step.after.name());
        //then
        assertThat(afterFiles, is(ImmutableList.of("http_url_root_ff3c40c_1001_02002_after.png", "http_url_root_ff3c40c_1001_03003_after.png")));
    }

    @Test
    public void shouldBuildRelativePathsForDifferentDirectories() {
        //given
        runStepConfig = jLineupRunConfigurationBuilder()
                .withWorkingDirectory("src" + FILE_SEPARATOR + "test" + FILE_SEPARATOR + "resources")
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .build();
        FileService fileService = new FileService(runStepConfig);

        //when
        String relativePathFromReportDirToScreenshotsDir = fileService.getRelativePathFromReportDirToScreenshotsDir();

        //then
        assertThat(relativePathFromReportDirToScreenshotsDir, is(".." + FILE_SEPARATOR + "screenshots" + FILE_SEPARATOR));

    }

    @Test
    public void shouldBuildRelativePathsForSame() {
        //given
        runStepConfig = jLineupRunConfigurationBuilder()
                .withWorkingDirectory("src/test/resources")
                .withScreenshotsDirectory("rreeppoorrtt")
                .withReportDirectory("rreeppoorrtt")
                .build();
        FileService fileService = new FileService(runStepConfig);

        //when
        String relativePathFromReportDirToScreenshotsDir = fileService.getRelativePathFromReportDirToScreenshotsDir();

        //then
        assertThat(relativePathFromReportDirToScreenshotsDir, is(""));

    }
}