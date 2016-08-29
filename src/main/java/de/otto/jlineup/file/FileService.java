package de.otto.jlineup.file;

import com.google.common.annotations.VisibleForTesting;
import de.otto.jlineup.config.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileService {

    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String DIVIDER = "_";
    public static final String PNG_EXTENSION = ".png";

    public Path createDirIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        Files.createDirectories(path);
        return path;
    }

    public void clearDirectory(String path) throws IOException {
        Path directory = Paths.get(path);
        Files.newDirectoryStream(directory).forEach(file -> {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void createOrClearReportDirectory(Parameters parameters) {
        createOrClearDirectoryBelowWorkingDir(parameters.getWorkingDirectory(), parameters.getReportDirectory());
    }

    public Path getWorkingDirectory(Parameters parameters) {
        return Paths.get(parameters.getWorkingDirectory());
    }

    public Path getScreenshotDirectory(Parameters parameters) {
        return Paths.get(parameters.getWorkingDirectory() + "/" + parameters.getScreenshotDirectory());
    }

    public Path getReportDirectory(Parameters parameters) {
        return Paths.get(parameters.getWorkingDirectory() + "/" + parameters.getReportDirectory());
    }

    public void createWorkingDirectoryIfNotExists(Parameters parameters) {
        try {
            createDirIfNotExists(parameters.getWorkingDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open working directory.");
            System.exit(1);
        }
    }

    public void createOrClearScreenshotsDirectory(Parameters parameters) {
        createOrClearDirectoryBelowWorkingDir(parameters.getWorkingDirectory(), parameters.getScreenshotDirectory());
    }

    private void createOrClearDirectoryBelowWorkingDir(String workingDirectory, String subDirectory) {
        try {
            final String subDirectoryPath = workingDirectory + "/" + subDirectory;
            createDirIfNotExists(subDirectoryPath);
            clearDirectory(subDirectoryPath);
        } catch (IOException e) {
            System.err.println("Could not create or open " + subDirectory + " directory.");
            System.exit(1);
        }
    }

    String generateScreenshotFileName(String url, String path, int width, int yPosition, String type) {

        String fileName = generateScreenshotFileNamePrefix(url, path)
                + String.format("%04d", width)
                + DIVIDER
                + String.format("%05d", yPosition)
                + DIVIDER
                + type;

        fileName = fileName + PNG_EXTENSION;

        return fileName;
    }

    String generateScreenshotFileNamePrefix(String url, String path) {

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

    String getScreenshotPath(Parameters parameters, String url, String path, int width, int yPosition, String step) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + generateScreenshotFileName(url, path, width, yPosition, step);
    }

    String getScreenshotPath(Parameters parameters, String fileName) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + fileName;
    }

    public BufferedImage readScreenshot(Parameters parameters, String fileName) throws IOException {
        return ImageIO.read(new File(getScreenshotPath(parameters, fileName)));
    }

    private void writeScreenshot(String fileName, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", new File(fileName));
    }

    @VisibleForTesting
    List<String> getFileNamesMatchingPattern(Path directory, String matcherPattern) throws IOException {
        final List<String> files = new ArrayList<>();

        final PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher(matcherPattern);

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                directory, pathMatcher::matches)) {
            dirStream.forEach(filePath -> files.add(filePath.getFileName().toString()));
        }
        Collections.sort(files, Comparator.naturalOrder());
        return files;
    }

    public String writeScreenshot(BufferedImage image, Parameters parameters, String url,
                                String urlPath, int windowWidth, int yPosition, String step) throws IOException {
        final String screenshotPath =
                getScreenshotPath(parameters, url,
                        urlPath, windowWidth,
                        yPosition, step);
        writeScreenshot(screenshotPath, image);
        return screenshotPath;
    }

    public List<String> getFilenamesForStep(Parameters parameters, String path, String url, String step) throws IOException {
        final String matcherPattern = "glob:**" + generateScreenshotFileNamePrefix(url, path) + "*_*_" + step + ".png";
        Path screenshotDirectory = getScreenshotDirectory(parameters);
        return getFileNamesMatchingPattern(screenshotDirectory, matcherPattern);
    }
}
