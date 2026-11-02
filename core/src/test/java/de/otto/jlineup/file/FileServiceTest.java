package de.otto.jlineup.file;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static de.otto.jlineup.RunStepConfig.runStepConfigBuilder;
import static de.otto.jlineup.browser.BrowserStep.after;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;
import static de.otto.jlineup.file.FileService.generateScreenshotFileName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class FileServiceTest {

    private FileService testee;

    private RunStepConfig runStepConfig;
    private JobConfig jobConfig;

    @TempDir
    Path tempDir;

    private String tempDirPath;
    private String writeScreenshotTestPath;

    @BeforeEach
    void setup() throws IOException {
        tempDirPath = tempDir.toString();
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
    void shouldCreateDirectory() throws IOException {
        //when
        testee.createDirIfNotExists(tempDirPath + "/testdirforlineuptest");
        //then
        assertThat(Files.isDirectory(Paths.get(tempDirPath + "/testdirforlineuptest")), is(true));
    }

    @Test
    void shouldWriteScreenshot() throws IOException {
        BufferedImage bufferedImage = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, 200);

        String fileName = testee.writeScreenshot(ScreenshotContext.of("someUrl", "somePath", DeviceConfig.deviceConfig(1000, 1001), before, UrlConfig.urlConfigBuilder().build()), bufferedImage, 12345);

        assertThat(Files.exists(Paths.get(testee.getScreenshotDirectory().toString(), fileName)), is(true));
    }

    @Test
    void shouldWriteJsonReport() throws Exception {

        testee.writeJsonReport("[{\"toll\":\"mega\"}]");

        Path reportFilePath = Paths.get(writeScreenshotTestPath + "/report/report.json");
        assertThat(Files.exists(reportFilePath), is(true));
        List<String> reportFileContents = Files.readAllLines(reportFilePath);
        assertThat(reportFileContents.get(0), is("[{\"toll\":\"mega\"}]"));
    }

    @Test
    void shouldGenerateFullPathToPngFile() {
        final String fullFileNameWithPath = testee.getScreenshotPath("testurl", "/", 1001, 2002, "step");

        assertThat(fullFileNameWithPath, is(writeScreenshotTestPath + FILE_SEPARATOR + "screenshots" + FILE_SEPARATOR + "testurl_root_bbf1812_1001_02002_step.png"));
    }

    @Test
    void shouldGenerateFilename() {
        String outputString = generateScreenshotFileName("https://www.otto.de/", "multi-media#anchor?one=two&three=fo_ur", 1000, 2000, "after");
        assertThat(outputString, is("https_www_otto_de_multi-media_anchor_one_two_three_fo_ur_99698cd_1000_02000_after.png"));
    }

    @Test
    void shouldGenerateFilenameWithAMaxLenghtOf255Bytes() {
        String outputString = generateScreenshotFileName("https://www.otto.de/", "multi-media#anchor?abcdefghijklmnopqrstuvwxyz=12345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890", 1000, 2000, "after");
        assertThat(outputString.length(), Matchers.lessThan(255));
        assertThat(outputString, is("https_www_otto_de_multi-media_anchor_abcdefghijklmnopqrstuvwxyz_12345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567_559e477_1000_02000_after.png"));
    }

    @Test
    void shouldConvertRoot() {
        String outputString = generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("https_www_otto_de_root_baa15be_1000_02000_before.png"));
    }

    @Test
    void shouldBuildRelativePathsForDifferentDirectories() {
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
    void shouldBuildRelativePathsForSame() {
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

    @Test
    void shouldSerializeAndDeserializeBrowserTypeInFileTracker() throws Exception {
        // Create a ScreenshotContext with browserType set
        ScreenshotContext context = ScreenshotContext.of("https://example.com", "/", DeviceConfig.deviceConfig(1000, 1001), before, UrlConfig.urlConfigBuilder().build(), Browser.Type.CHROME_HEADLESS);

        // Add a screenshot to the file tracker
        BufferedImage bufferedImage = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        testee.writeScreenshot(context, bufferedImage, 0);

        // Write the file tracker data
        testee.writeFileTrackerData();

        // Read it back
        Path filesJsonPath = Paths.get(writeScreenshotTestPath, "report", "files.json");
        String json = Files.readString(filesJsonPath);

        // Verify browser-type appears in the JSON
        assertThat("browser-type should be in serialized JSON", json.contains("browser-type"), is(true));

        // Deserialize and verify
        FileTracker deserialized = JacksonWrapper.readFileTrackerFile(filesJsonPath.toFile());
        assertThat(deserialized, is(notNullValue()));

        String contextHash = context.contextHash();
        ScreenshotContextFileTracker ctxTracker = deserialized.getScreenshotContextFileTracker(contextHash);
        assertThat("Context should be found by hash " + contextHash, ctxTracker, is(notNullValue()));
        assertThat("browserType should survive round-trip", ctxTracker.screenshotContext.browserType, is(Browser.Type.CHROME_HEADLESS));
    }

    @Test
    void shouldWriteImagelessScreenshot() throws IOException {
        ScreenshotContext context = ScreenshotContext.of("someUrl", "somePath", DeviceConfig.deviceConfig(1000, 1001), before, UrlConfig.urlConfigBuilder().build());

        String fileName = testee.writeImagelessScreenshot(context, new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), 600);

        assertThat(fileName.endsWith("_1000_00600_before_noimages.png"), is(true));
        assertThat(Files.exists(Paths.get(testee.getScreenshotDirectory().toString(), fileName)), is(true));
        assertThat(testee.getRecordedContextFileTracker(context.contextHash()).imagelessScreenshots.get(600).get(before), is(fileName));
    }

    @Test
    void shouldMergeDataOfIgnoreImageContentFromContextFileTrackers() throws Exception {
        ScreenshotContext beforeContext = ScreenshotContext.of("https://example.com", "/", DeviceConfig.deviceConfig(1000, 1001), before, UrlConfig.urlConfigBuilder().build());
        ScreenshotContext afterContext = ScreenshotContext.of("https://example.com", "/", DeviceConfig.deviceConfig(1000, 1001), after, UrlConfig.urlConfigBuilder().build());
        testee.writeScreenshot(beforeContext, new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), 0);
        testee.writeImagelessScreenshot(beforeContext, new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), 0);
        testee.setImageAreas(beforeContext, 0, List.<int[]>of(new int[]{0, 0, 5, 5}));
        testee.setImageUrls(beforeContext, Set.of("a.webp"));

        //a part as written by a lambda for the after step
        FileTracker part = FileTracker.create(jobConfig);
        part.addScreenshot(afterContext, "after.png", 0);
        part.addImagelessScreenshot(afterContext, "after_noimages.png", 0);
        part.setImageAreas(afterContext, 0, List.<int[]>of(new int[]{1, 1, 5, 5}));
        part.setImageUrls(afterContext, Set.of("b.webp"));
        Path partsDirectory = Files.createDirectories(tempDir.resolve("parts"));
        Files.writeString(partsDirectory.resolve("part_files.json"), JacksonWrapper.serializeObject(part));

        testee.mergeContextFileTrackersIntoFileTracker(partsDirectory, (dir, name) -> name.endsWith("_files.json"));

        ScreenshotContextFileTracker merged = testee.getRecordedContextFileTracker(beforeContext.contextHash());
        assertThat(merged.imagelessScreenshots.get(0).get(after), is("after_noimages.png"));
        assertThat(merged.imagelessScreenshots.get(0).get(before).endsWith("before_noimages.png"), is(true));
        assertThat(merged.imageAreas.get(0).get(before).get(0), is(new int[]{0, 0, 5, 5}));
        assertThat(merged.imageAreas.get(0).get(after).get(0), is(new int[]{1, 1, 5, 5}));
        assertThat(merged.imageUrls.get(before), is(Set.of("a.webp")));
        assertThat(merged.imageUrls.get(after), is(Set.of("b.webp")));
    }

    @Test
    void shouldCleanAfterArtifactsOfIgnoreImageContent() throws Exception {
        //the web server keeps screenshots and files.json in the same report directory
        RunStepConfig webLayout = runStepConfigBuilder().withWorkingDirectory(writeScreenshotTestPath).withScreenshotsDirectory("run").withReportDirectory("run").withStep(RunStep.before).build();
        FileService fileService = new FileService(webLayout, jobConfig);
        ScreenshotContext beforeContext = ScreenshotContext.of("https://example.com", "/", DeviceConfig.deviceConfig(1000, 1001), before, UrlConfig.urlConfigBuilder().build());
        ScreenshotContext afterContext = ScreenshotContext.of("https://example.com", "/", DeviceConfig.deviceConfig(1000, 1001), after, UrlConfig.urlConfigBuilder().build());
        Path reportDirectory = Files.createDirectories(Paths.get(writeScreenshotTestPath, "run"));
        fileService.writeScreenshot(beforeContext, new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), 0);
        String beforeImageless = fileService.writeImagelessScreenshot(beforeContext, new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), 0);
        String afterImageless = fileService.writeImagelessScreenshot(afterContext, new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), 0);
        fileService.setImageAreas(beforeContext, 0, List.<int[]>of(new int[]{0, 0, 5, 5}));
        fileService.setImageAreas(afterContext, 0, List.<int[]>of(new int[]{0, 0, 5, 5}));
        fileService.setImageUrls(beforeContext, Set.of("a.webp"));
        fileService.setImageUrls(afterContext, Set.of("a.webp"));
        fileService.writeFileTrackerData();

        FileService.cleanAfterArtifacts(reportDirectory);

        assertThat(Files.exists(reportDirectory.resolve(afterImageless)), is(false));
        assertThat(Files.exists(reportDirectory.resolve(beforeImageless)), is(true));
        FileTracker cleaned = JacksonWrapper.readFileTrackerFile(reportDirectory.resolve("files.json").toFile());
        ScreenshotContextFileTracker contextFileTracker = cleaned.getScreenshotContextFileTracker(beforeContext.contextHash());
        assertThat(contextFileTracker.imagelessScreenshots.get(0).containsKey(after), is(false));
        assertThat(contextFileTracker.imagelessScreenshots.get(0).containsKey(before), is(true));
        assertThat(contextFileTracker.imageAreas.get(0).containsKey(after), is(false));
        assertThat(contextFileTracker.imageUrls.containsKey(after), is(false));
    }

    @Test
    void shouldDetectIdenticalScreenshotFiles() throws IOException {
        Path screenshots = Paths.get(writeScreenshotTestPath, "screenshots");
        Files.write(screenshots.resolve("a.png"), new byte[]{1, 2, 3, 4});
        Files.write(screenshots.resolve("same.png"), new byte[]{1, 2, 3, 4});
        Files.write(screenshots.resolve("changed.png"), new byte[]{1, 2, 3, 5});
        Files.write(screenshots.resolve("longer.png"), new byte[]{1, 2, 3, 4, 5});
        Files.write(screenshots.resolve("empty1.png"), new byte[]{});
        Files.write(screenshots.resolve("empty2.png"), new byte[]{});

        assertThat(testee.areScreenshotFilesIdentical("a.png", "same.png"), is(true));
        assertThat(testee.areScreenshotFilesIdentical("a.png", "changed.png"), is(false));
        assertThat(testee.areScreenshotFilesIdentical("a.png", "longer.png"), is(false));
        assertThat(testee.areScreenshotFilesIdentical("a.png", "missing.png"), is(false));
        assertThat(testee.areScreenshotFilesIdentical("missing.png", "a.png"), is(false));
        assertThat(testee.areScreenshotFilesIdentical("empty1.png", "empty2.png"), is(false));
    }

}
