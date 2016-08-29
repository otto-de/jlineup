package de.otto.jlineup.file;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import de.otto.jlineup.config.Parameters;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.file.FileService.AFTER;
import static de.otto.jlineup.file.FileService.BEFORE;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileServiceTest {

    private String tempDirPath;
    private String writeScreenshotTestPath;
    private FileService testee;

    private Parameters parameters;

    @Before
    public void setup() throws IOException {

        //given
        parameters = new Parameters();
        new JCommander(parameters, "-d", "src/test/resources");

        testee = new FileService();

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDirPath = tempDir.getPath();
        writeScreenshotTestPath = tempDirPath + "/testdirforlineupwritetest";
        testee.createDirIfNotExists(writeScreenshotTestPath);
        testee.createDirIfNotExists(writeScreenshotTestPath + "/screenshots");

    }

    @After
    public void cleanup() throws IOException {
        deleteIfExists(Paths.get(tempDirPath + "/testdirforlineuptest"));
        deleteIfExists(Paths.get(tempDirPath + "/testdirforcleardirectorylineuptest"));
        deleteIfExists(Paths.get(writeScreenshotTestPath + "/screenshots"));
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
        Parameters myParameters = new Parameters();
        new JCommander(myParameters, "-d", writeScreenshotTestPath);
        BufferedImage bufferedImage = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, 200);

        String fileName = testee.writeScreenshot(bufferedImage, myParameters, "http://someurl", "somePath", 999, 777, "someStep");

        assertThat(Files.exists(Paths.get(fileName)), is(true));

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

        Parameters myParameters =  mock(Parameters.class);
        when(myParameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(myParameters.getScreenshotDirectory()).thenReturn("someScreenshotDir");

        final String fullFileNameWithPath = testee.getScreenshotPath(myParameters, "testurl", "/", 1001, 2002, "step");
        Assert.assertThat(fullFileNameWithPath, CoreMatchers.is("some/working/dir/someScreenshotDir/testurl_root_1001_02002_step.png"));
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        Assert.assertThat(outputString, CoreMatchers.is("www_otto_de_multimedia_1000_02000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = testee.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        Assert.assertThat(outputString, CoreMatchers.is("www_otto_de_root_1000_02000_before.png"));
    }

    @Test
    public void shouldFindAfterImagesInDirectoryWithPattern() throws IOException {
        List<String> fileNamesMatchingPattern = testee.getFileNamesMatchingPattern(Paths.get("src/test/resources/screenshots"), "glob:**url_root_*_*_after.png");
        assertThat(fileNamesMatchingPattern, is(Arrays.asList("url_root_1001_02002_after.png", "url_root_1001_03003_after.png")));
    }

    @Test
    public void shouldFindBeforeImages() throws IOException {
        //when
        List<String> beforeFiles = testee.getFilenamesForStep(parameters, "/", "http://url", BEFORE);
        //then
        assertThat(beforeFiles, is(ImmutableList.of("url_root_1001_02002_before.png")));
    }

    @Test
    public void shouldFindAfterImages() throws IOException {
        //when
        List<String> beforeFiles = testee.getFilenamesForStep(parameters, "/", "http://url", AFTER);
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