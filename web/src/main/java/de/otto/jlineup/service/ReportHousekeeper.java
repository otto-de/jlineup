package de.otto.jlineup.service;

import de.otto.jlineup.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.invoke.MethodHandles.lookup;

public class ReportHousekeeper {

    private static final int DELETE_REPORTS_AFTER_DAYS = 7;
    private static final int ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final Path path;

    public ReportHousekeeper(Path path) {
        this.path = path;
    }

    @Scheduled(fixedRate = ONE_HOUR_IN_MILLIS)
    public void deleteOldReports() throws IOException {
        this.deleteReportsOlderThan(Duration.ofDays(DELETE_REPORTS_AFTER_DAYS));
    }

    private void deleteReportsOlderThan(Duration duration) throws IOException {

        Instant pointInTime = Instant.now().minus(duration);

        LOG.info("delete reports older than {}", pointInTime);

        Files.list(path)
                .filter(filesOlderThan(pointInTime))
                .forEach(deletePath());
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
