package de.otto.jlineup.file;

import de.otto.jlineup.config.Parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String DIVIDER = "_";
    public static final String PNG_EXTENSION = ".png";

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

    public static Path getWorkingDirectory(Parameters parameters) {
        return Paths.get(parameters.getWorkingDirectory());
    }

    public static Path getScreenshotDirectory(Parameters parameters) {
        return Paths.get(parameters.getWorkingDirectory() + "/" + parameters.getScreenshotDirectory());
    }

    public static Path getReportDirectory(Parameters parameters) {
        return Paths.get(parameters.getWorkingDirectory() + "/" + parameters.getReportDirectory());
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

    public static String generateScreenshotFileName(String url, String path, int width, int yPosition, String type) {

        String fileName = generateScreenshotFileNamePrefix(url, path)
                + String.format("%d", width)
                + DIVIDER
                + String.format("%d", yPosition)
                + DIVIDER
                + type;

        fileName = fileName + PNG_EXTENSION;

        return fileName;
    }

    public static String generateScreenshotFileNamePrefix(String url, String path) {

        if (path.equals("/") || path.equals("")) {
            path = "root";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String fileName = url + DIVIDER + path + DIVIDER;

        fileName = fileName
                .replace("http://", "")
                .replace("https://", "")
                .replace("/", DIVIDER)
                .replace("..", "")
                .replace(".", DIVIDER);

        return fileName;
    }

    public static String getFullScreenshotFileNameWithPath(Parameters parameters, String url, String path, int width, int yPosition, String step) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + generateScreenshotFileName(url, path, width, yPosition, step);
    }

    public static String getFullScreenshotFileNameWithPath(Parameters parameters, String fileName) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + fileName;
    }
}
