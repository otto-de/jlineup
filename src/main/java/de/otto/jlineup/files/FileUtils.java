package de.otto.jlineup.files;

import de.otto.jlineup.Main;
import de.otto.jlineup.config.Parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static Path createDirIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        Files.createDirectories(path);
        return path;
    }

    public static void clearDirectory(String path) throws IOException {
        Path directory = Paths.get(path);
        Files.newDirectoryStream(directory).forEach(file -> {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static void createOrClearReportDirectory(Parameters parameters) {
        createOrClearDirectoryBelowWorkingDir(parameters.getWorkingDirectory(), parameters.getReportDirectory());
    }

    public static void createWorkingDirectoryIfNotExists(Parameters parameters) {
        try {
            FileUtils.createDirIfNotExists(parameters.getWorkingDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open working directory.");
            System.exit(1);
        }
    }

    public static void createOrClearScreenshotsDirectory(Parameters parameters) {
        createOrClearDirectoryBelowWorkingDir(parameters.getWorkingDirectory(), parameters.getScreenshotDirectory());
    }

    private static void createOrClearDirectoryBelowWorkingDir(String workingDirectory, String subDirectory) {
        try {
            final String subDirectoryPath = workingDirectory + "/" + subDirectory;
            FileUtils.createDirIfNotExists(subDirectoryPath);
            FileUtils.clearDirectory(subDirectoryPath);
        } catch (IOException e) {
            System.err.println("Could not create or open " + subDirectory + " directory.");
            System.exit(1);
        }
    }
}
