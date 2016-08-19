package de.otto.jlineup.files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FileUtilsTest {

    private String tempDirPath;

    @Before
    public void setup() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDirPath = tempDir.getPath();
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

    @After
    public void cleanup() throws IOException {
        deleteIfExists(Paths.get(tempDirPath + "/testdirforlineuptest"));
        deleteIfExists(Paths.get(tempDirPath + "/testdirforcleardirectorylineuptest"));
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            FileUtils.clearDirectory(path.toString());
            Files.delete(path);
        }
    }

}