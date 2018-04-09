package de.otto.jlineup.service;

import de.otto.jlineup.file.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class ReportHousekeeperTest {

    private Path tempDirectory;
    private ReportHousekeeper reportHousekeeper;


    @Before
    public void before() throws IOException {
        tempDirectory = Files.createTempDirectory("jlineup-web-test");
        reportHousekeeper = new ReportHousekeeper(tempDirectory);
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(tempDirectory);
    }

    @Test
    public void shouldDeleteReportsOlderThanOneWeek() throws IOException {

        //Given

        //Two folders with last modified 8 days ago
        Path report1dir = Files.createDirectories(tempDirectory.resolve("report-1"));
        Files.createDirectories(report1dir.resolve("subdir"));
        Files.setLastModifiedTime(report1dir, FileTime.from(Instant.now().minus(Duration.ofDays(8))));
        Path report2dir = Files.createDirectories(tempDirectory.resolve("report-2"));
        Files.createDirectories(report2dir.resolve("subdir"));
        Files.setLastModifiedTime(report2dir, FileTime.from(Instant.now().minus(Duration.ofDays(8))));

        //One folder with current time
        Path report3dir = Files.createDirectories(tempDirectory.resolve("report-3"));
        Files.createDirectories(report3dir.resolve("subdir"));

        //when
        reportHousekeeper.deleteOldReports();

        //then
        List<Path> dirs = Files.list(tempDirectory).collect(toImmutableList());
        assertThat(dirs, hasSize(1));
        assertThat(dirs.get(0), is(report3dir));
    }

}