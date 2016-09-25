package de.otto.jlineup.file;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.config.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.file.FileService.AFTER;
import static de.otto.jlineup.file.FileService.BEFORE;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FileServiceTest {

    private FileService testee;

    @Mock
    private Parameters parameters;

    private String tempDirPath;
    private String writeScreenshotTestPath;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        when(parameters.getWorkingDirectory()).thenReturn("src/test/resources");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");
        when(parameters.getReportDirectory()).thenReturn("report");

        testee = new FileService(parameters);

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDirPath = tempDir.getPath();
        writeScreenshotTestPath = tempDirPath + "/testdirforlineupwritetest";
        testee.createDirIfNotExists(writeScreenshotTestPath);
        testee.createDirIfNotExists(writeScreenshotTestPath + "/screenshots");
        testee.createDirIfNotExists(writeScreenshotTestPath + "/report");

    }

    @After
    public void cleanup() throws IOException {
        deleteIfExists(Paths.get(tempDirPath + "/testdirforlineuptest"));
        deleteIfExists(Paths.get(tempDirPath + "/testdirforcleardirectorylineuptest"));
        deleteIfExists(Paths.get(writeScreenshotTestPath + "/screenshots"));
        deleteIfExists(Paths.get(writeScreenshotTestPath + "/report"));
        deleteIfExists(Paths.get(writeScreenshotTestPath));
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
        when(parameters.getWorkingDirectory()).thenReturn(writeScreenshotTestPath);
        BufferedImage bufferedImage = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, 200);

        String fileName = testee.writeScreenshot(bufferedImage, "http://someurl", "somePath", 999, 777, "someStep");

        assertThat(Files.exists(Paths.get(fileName)), is(true));
    }

    @Test
    public void shouldWriteJsonReport() throws Exception {
        when(parameters.getWorkingDirectory()).thenReturn(writeScreenshotTestPath);

        testee.writeJsonReport("[{\"toll\":\"mega\"}]");

        Path reportFilePath = Paths.get(writeScreenshotTestPath + "/report/report.json");
        assertThat(Files.exists(reportFilePath), is(true));
        List<String> reportFileContents = Files.readAllLines(reportFilePath);
        assertThat(reportFileContents.get(0), is("[{\"toll\":\"mega\"}]"));
    }

    @Test
    public void shouldClearDirectory() throws IOException {
        //given
        final Path dirToClear = testee.createDirIfNotExists(tempDirPath + "/testdirforcleardirectorylineuptest");
        Files.createFile(dirToClear.resolve(Paths.get("test1")));
        Files.createFile(dirToClear.resolve(Paths.get("test2")));
        Files.createFile(dirToClear.resolve(Paths.get("test3")));

        //when
        testee.clearDirectory(dirToClear.toString());

        //then
        assertThat(dirToClear.toFile().list().length, is(0));
    }

    @Test
    public void shouldGenerateFullPathToPngFile() {
        when(parameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(parameters.getScreenshotDirectory()).thenReturn("someScreenshotDir");

        final String fullFileNameWithPath = testee.getScreenshotPath("testurl", "/", 1001, 2002, "step");

        assertThat(fullFileNameWithPath, is("some/working/dir/someScreenshotDir/testurl_root_1001_02002_step.png"));
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        assertThat(outputString, is("www_otto_de_multimedia_1000_02000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("www_otto_de_root_1000_02000_before.png"));
    }

    @Test
    public void shouldFindAfterImagesInDirectoryWithPattern() throws IOException {
        List<String> fileNamesMatchingPattern = testee.getFileNamesMatchingPattern(Paths.get("src/test/resources/screenshots"), "glob:**url_root_*_*_after.png");
        assertThat(fileNamesMatchingPattern, is(Arrays.asList("url_root_1001_02002_after.png", "url_root_1001_03003_after.png")));
    }

    @Test
    public void shouldFindBeforeImages() throws IOException {
        //when
        List<String> beforeFiles = testee.getFilenamesForStep("/", "http://url", BEFORE);
        //then
        assertThat(beforeFiles, is(ImmutableList.of("url_root_1001_02002_before.png")));
    }

    @Test
    public void shouldFindAfterImages() throws IOException {
        //when
        List<String> beforeFiles = testee.getFilenamesForStep("/", "http://url", AFTER);
        //then
        assertThat(beforeFiles, is(ImmutableList.of("url_root_1001_02002_after.png", "url_root_1001_03003_after.png")));
    }

    private void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            testee.clearDirectory(path.toString());
            Files.delete(path);
        }
    }

}