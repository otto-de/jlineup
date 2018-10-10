package de.otto.jlineup.service;

import de.otto.jlineup.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.throwException;

public class Housekeeper {

    private static final int DELETE_REPORTS_AFTER_DAYS = 7;
    private static final int DELETE_SCREENSHOTS_AFTER_DAYS = 1;
    private static final int ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;
    private static final String SELENIUM_SCREENSHOT_PREFIX = "screenshot";
    private static final String SELENIUM_SCREENSHOT_EXTENTION = ".png";

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final Path path;

    public Housekeeper(Path path) {
        this.path = path;
    }

    @Scheduled(fixedRate = ONE_HOUR_IN_MILLIS)
    public void deleteOldFiles() throws IOException {
        this.deleteReportsOlderThan(Duration.ofDays(DELETE_REPORTS_AFTER_DAYS));
        // in case the jlineup server is permanently running the screenshots taken by selenium within the tmp dir
        // will not be removed after the webdriver.close, that is why the screenshots must be removed using this job
        this.deleteSeleniumScreenshotsOlderThan(Duration.ofDays(DELETE_SCREENSHOTS_AFTER_DAYS));
    }

    private void deleteSeleniumScreenshotsOlderThan(Duration duration) throws IOException {

        Instant pointInTime = Instant.now().minus(duration);

        LOG.info("delete selenium screenshots older than {}", pointInTime);

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        File[] screenShotFiles = tmpDir.listFiles((dir, fileName) ->
                fileName.startsWith(SELENIUM_SCREENSHOT_PREFIX) && fileName.endsWith(SELENIUM_SCREENSHOT_EXTENTION));

        if (screenShotFiles != null) {
            Stream.of(screenShotFiles)
                    .map(File::toPath)
                    .filter(filesOlderThan(pointInTime))
                    .forEach(this::deleteFile);
        }
    }

    private void deleteReportsOlderThan(Duration duration) throws IOException {

        Instant pointInTime = Instant.now().minus(duration);

        LOG.info("delete reports older than {}", pointInTime);

        Files.list(path)
                .filter(filesOlderThan(pointInTime))
                .forEach(deletePath());
    }

    private void deleteFile(Path path) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    private Consumer<Path> deletePath() {
        return file -> {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private Predicate<Path> filesOlderThan(Instant pointInTime) {
        return file -> {
            try {
                Instant fileDate = Files.getLastModifiedTime(file).toInstant();
                return fileDate.isBefore(pointInTime);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        };
    }

}
