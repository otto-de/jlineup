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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileUtilsTest {

    private String tempDirPath;

    @Before
    public void setup() {
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
        FileUtils.createDirIfNotExists(tempDirPath + "/testdirforlineuptest");
        //then
        assertThat(Files.isDirectory(Paths.get(tempDirPath + "/testdirforlineuptest")), is(true));
    }

    @Test
    public void shouldClearDirectory() throws IOException {
        //given
        final Path dirToClear = FileUtils.createDirIfNotExists(tempDirPath + "/testdirforcleardirectorylineuptest");
        Files.createFile(dirToClear.resolve(Paths.get("test1")));
        Files.createFile(dirToClear.resolve(Paths.get("test2")));
        Files.createFile(dirToClear.resolve(Paths.get("test3")));

        //when
        FileUtils.clearDirectory(dirToClear.toString());

        //then
        assertThat(dirToClear.toFile().list().length, is(0));
    }

    @Test
    public void shouldGenerateFullPathToPngFile() {
        Parameters parameters = mock(Parameters.class);
        when(parameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");
        final String fullFileNameWithPath = FileUtils.getFullScreenshotFileNameWithPath(parameters, "testurl", "/", 1001, 2002, "step");
        Assert.assertThat(fullFileNameWithPath, CoreMatchers.is("some/working/dir/screenshots/testurl_root_1001_2002_step.png"));
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = FileUtils.generateScreenshotFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        Assert.assertThat(outputString, CoreMatchers.is("www_otto_de_multimedia_1000_2000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = FileUtils.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        Assert.assertThat(outputString, CoreMatchers.is("www_otto_de_root_1000_2000_before.png"));
    }

    public static void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            FileUtils.clearDirectory(path.toString());
            Files.delete(path);
        }
    }

}