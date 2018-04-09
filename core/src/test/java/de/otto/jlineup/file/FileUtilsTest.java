package de.otto.jlineup.file;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.otto.jlineup.file.FileUtils.clearDirectory;
import static de.otto.jlineup.file.FileUtils.deleteDirectory;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileUtilsTest {


    @Test
    public void shouldClearDirectory() throws IOException {
        //given
        final Path dirToClear = Files.createTempDirectory("jlineup-fileutils-test");
        Files.createFile(dirToClear.resolve(Paths.get("test1")));
        Files.createFile(dirToClear.resolve(Paths.get("test2")));
        Files.createFile(dirToClear.resolve(Paths.get("test3")));

        //when
        clearDirectory(dirToClear.toString());

        //then
        assertThat(dirToClear.toFile().list().length, is(0));

        Files.delete(dirToClear);
    }

    @Test
    public void shouldDeleteDirectory() throws IOException {
        //given
        final Path dirToDelete = Files.createTempDirectory("jlineup-fileutils-test");
        Files.createDirectories(dirToDelete.resolve("one/two/three"));

        //when
        deleteDirectory(dirToDelete);

        //then
        assertThat(Files.exists(dirToDelete), is(false));
    }

}