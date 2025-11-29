package de.otto.jlineup.file;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
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
import java.util.List;

import static de.otto.jlineup.RunStepConfig.runStepConfigBuilder;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;
import static de.otto.jlineup.file.FileService.generateScreenshotFileName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class FileServiceTest {

    private FileService testee;

    private RunStepConfig runStepConfig;
    private JobConfig jobConfig;

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    private String tempDirPath;
    private String writeScreenshotTestPath;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        tempDirPath = tempDir.getRoot().getPath();

        writeScreenshotTestPath = tempDirPath + "/testdirforlineupwritetest";

        runStepConfig = runStepConfigBuilder()
                .withWorkingDirectory(writeScreenshotTestPath)
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .withStep(RunStep.before)
                .build();

        jobConfig = JobConfig.exampleConfig();

        testee = new FileService(runStepConfig, jobConfig);
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

        String fileName = testee.writeScreenshot(ScreenshotContext.of("someUrl", "somePath", DeviceConfig.deviceConfig(1000, 1001), before, UrlConfig.urlConfigBuilder().build()), bufferedImage, 12345);

        assertThat(Files.exists(Paths.get(testee.getScreenshotDirectory().toString(), fileName)), is(true));
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
        String outputString = generateScreenshotFileName("https://www.otto.de/", "multi-media#anchor?one=two&three=fo_ur", 1000, 2000, "after");
        assertThat(outputString, is("https_www_otto_de_multi-media_anchor_one_two_three_fo_ur_99698cd_1000_02000_after.png"));
    }

    @Test
    public void shouldGenerateFilenameWithAMaxLenghtOf255Bytes() throws Exception {
        String outputString = generateScreenshotFileName("https://www.otto.de/", "multi-media#anchor?abcdefghijklmnopqrstuvwxyz=12345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890", 1000, 2000, "after");
        assertThat(outputString.length(), Matchers.lessThan(255));
        assertThat(outputString, is("https_www_otto_de_multi-media_anchor_abcdefghijklmnopqrstuvwxyz_12345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567_559e477_1000_02000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("https_www_otto_de_root_baa15be_1000_02000_before.png"));
    }

    @Test
    public void shouldBuildRelativePathsForDifferentDirectories() {
        //given
        runStepConfig = runStepConfigBuilder()
                .withWorkingDirectory("src" + FILE_SEPARATOR + "test" + FILE_SEPARATOR + "resources")
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .withStep(RunStep.before)
                .build();
        FileService fileService = new FileService(runStepConfig, jobConfig);

        //when
        String relativePathFromReportDirToScreenshotsDir = fileService.getRelativePathFromReportDirToScreenshotsDir();

        //then
        assertThat(relativePathFromReportDirToScreenshotsDir, is(".." + FILE_SEPARATOR + "screenshots" + FILE_SEPARATOR));

    }

    @Test
    public void shouldBuildRelativePathsForSame() {
        //given
        runStepConfig = runStepConfigBuilder()
                .withWorkingDirectory("src/test/resources")
                .withScreenshotsDirectory("rreeppoorrtt")
                .withReportDirectory("rreeppoorrtt")
                .withStep(RunStep.before)
                .build();
        FileService fileService = new FileService(runStepConfig, jobConfig);

        //when
        String relativePathFromReportDirToScreenshotsDir = fileService.getRelativePathFromReportDirToScreenshotsDir();

        //then
        assertThat(relativePathFromReportDirToScreenshotsDir, is(""));

    }
}