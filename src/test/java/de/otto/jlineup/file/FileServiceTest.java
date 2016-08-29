package de.otto.jlineup.file;

import de.otto.jlineup.config.Parameters;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.file.FileService.AFTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileServiceTest {

    private String tempDirPath;
    private FileService testee;

    @Before
    public void setup() {
        testee = new FileService();
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDirPath = tempDir.getPath();
    }

    @After
    public void cleanup() throws IOException {
        deleteIfExists(Paths.get(tempDirPath + "/testdirforlineuptest"));
        deleteIfExists(Paths.get(tempDirPath + "/testdirforcleardirectorylineuptest"));
    }

    @Test
    public void shouldCreateDirectory() throws IOException {
        //when
        testee.createDirIfNotExists(tempDirPath + "/testdirforlineuptest");
        //then
        assertThat(Files.isDirectory(Paths.get(tempDirPath + "/testdirforlineuptest")), is(true));
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
        Parameters parameters = mock(Parameters.class);
        when(parameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");
        final String fullFileNameWithPath = FileService.getFullScreenshotFileNameWithPath(parameters, "testurl", "/", 1001, 2002, "step");
        Assert.assertThat(fullFileNameWithPath, CoreMatchers.is("some/working/dir/screenshots/testurl_root_1001_02002_step.png"));
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = FileService.generateScreenshotFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        Assert.assertThat(outputString, CoreMatchers.is("www_otto_de_multimedia_1000_02000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = FileService.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        Assert.assertThat(outputString, CoreMatchers.is("www_otto_de_root_1000_02000_before.png"));
    }

    @Test
    public void shouldFindAfterImagesInDirectory() throws IOException {
        List<String> fileNamesMatchingPattern = testee.getFileNamesMatchingPattern(Paths.get("src/test/resources/screenshots"), "glob:**url_root_*_*_after.png");
        assertThat(fileNamesMatchingPattern, is(Arrays.asList("url_root_1001_02002_after.png", "url_root_1001_03003_after.png")));
    }

    private void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            testee.clearDirectory(path.toString());
            Files.delete(path);
        }
    }

}