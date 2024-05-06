package de.otto.jlineup.service;

import de.otto.jlineup.file.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class HousekeeperTest {

    private Path tempJLineupDirectory;
    private Path tempDirectory;
    private Housekeeper housekeeper;


    @Before
    public void before() throws IOException {
        tempJLineupDirectory = Files.createTempDirectory("jlineup-web-test");
        tempDirectory =  new File(System.getProperty("java.io.tmpdir")).toPath();
        housekeeper = new Housekeeper(tempJLineupDirectory);
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(tempJLineupDirectory);
    }

    @Test
    public void shouldDeleteReportsOlderThanThreeWeeks() throws IOException {

        //Given:

        //Two folders with last modified 8 days ago
        Path report1dir = Files.createDirectories(tempJLineupDirectory.resolve("report-1"));
        Files.createDirectories(report1dir.resolve("subdir"));
        Files.setLastModifiedTime(report1dir, FileTime.from(Instant.now().minus(Duration.ofDays(22))));
        Path report2dir = Files.createDirectories(tempJLineupDirectory.resolve("report-2"));
        Files.createDirectories(report2dir.resolve("subdir"));
        Files.setLastModifiedTime(report2dir, FileTime.from(Instant.now().minus(Duration.ofDays(23))));

        //One folder with current time
        Path report3dir = Files.createDirectories(tempJLineupDirectory.resolve("report-3"));
        Files.createDirectories(report3dir.resolve("subdir"));

        //when
        housekeeper.deleteOldFiles();

        //then
        List<Path> dirs = Files.list(tempJLineupDirectory).collect(toImmutableList());
        assertThat(dirs, hasSize(1));
        assertThat(dirs.get(0), is(report3dir));
    }

    @Test
    public void shouldDeleteSeleniumScreenshotsOlderThanOneWeek() throws IOException {

        //Given: some screenshot files
        Path file1 = Files.createTempFile("screenshot",".png");
        Path file2 = Files.createTempFile("screenshot",".png");
        Path file3 = Files.createTempFile("notascreenshot",".png");
        Path file4 = Files.createTempFile("screenshot",".png");
        Path file5 = Files.createTempFile("screenshot",".jpg");

        //three files older than a week and file4 has a current timestamp
        Files.setLastModifiedTime(file1, FileTime.from(Instant.now().minus(Duration.ofDays(8))));
        Files.setLastModifiedTime(file2, FileTime.from(Instant.now().minus(Duration.ofDays(8))));
        Files.setLastModifiedTime(file3, FileTime.from(Instant.now().minus(Duration.ofDays(8))));

        //when
        housekeeper.deleteOldFiles();

        //then: all files but file3 and file4 should be removed
        assertThat(file1.toFile().exists(), is(false));
        assertThat(file2.toFile().exists(), is(false));
        assertThat(file3.toFile().exists(), is(true));
        assertThat(file4.toFile().exists(), is(true));
        assertThat(file5.toFile().exists(), is(true));

        //cleanup created files
        Stream.of(file1, file2, file3, file4, file5)
                .map(Path::toFile)
                .filter(File::exists)
                .forEach(File::delete);
    }

}