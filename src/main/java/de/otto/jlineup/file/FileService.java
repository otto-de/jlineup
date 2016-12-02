package de.otto.jlineup.file;

import com.google.common.annotations.VisibleForTesting;
import de.otto.jlineup.config.Parameters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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

    private final Parameters parameters;

    public FileService(Parameters parameters) {
        this.parameters = parameters;
    }

    @VisibleForTesting
    Path createDirIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        Files.createDirectories(path);
        return path;
    }

    @VisibleForTesting
    void clearDirectory(String path) throws IOException {
        Path directory = Paths.get(path);
        Files.newDirectoryStream(directory).forEach(file -> {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void createOrClearReportDirectory() {
        createOrClearDirectoryBelowWorkingDir(parameters.getWorkingDirectory(), parameters.getReportDirectory());
    }

    private Path getScreenshotDirectory() {
        return Paths.get(parameters.getWorkingDirectory() + "/" + parameters.getScreenshotDirectory());
    }

    private Path getReportDirectory() {
        return Paths.get(parameters.getWorkingDirectory() + "/" + parameters.getReportDirectory());
    }

    public void createWorkingDirectoryIfNotExists() {
        try {
            createDirIfNotExists(parameters.getWorkingDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open working directory.");
            System.exit(1);
        }
    }

    public void createOrClearScreenshotsDirectory() {
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

    @VisibleForTesting
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

    private String generateScreenshotFileNamePrefix(String url, String path) {

        if (path.equals("/") || path.equals("")) {
            path = "root";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String fileName = url + DIVIDER + path + DIVIDER;
        fileName = fileName.replace("://", DIVIDER);
        fileName = fileName.replaceAll("[^A-Za-z0-9]", DIVIDER);

        return fileName;
    }

    @VisibleForTesting
    String getScreenshotPath(String url, String path, int width, int yPosition, String step) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + generateScreenshotFileName(url, path, width, yPosition, step);
    }

    private String getScreenshotPath(String fileName) {
        return parameters.getWorkingDirectory() + (parameters.getWorkingDirectory().endsWith("/") ? "" : "/")
                + parameters.getScreenshotDirectory() + (parameters.getScreenshotDirectory().endsWith("/") ? "" : "/")
                + fileName;
    }

    public BufferedImage readScreenshot(String fileName) throws IOException {
        return ImageIO.read(new File(getScreenshotPath(fileName)));
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

    public String writeScreenshot(BufferedImage image, String url,
                                  String urlPath, int windowWidth, int yPosition, String step) throws IOException {
        final String screenshotPath =
                getScreenshotPath(url,
                        urlPath, windowWidth,
                        yPosition, step);
        writeScreenshot(screenshotPath, image);
        return screenshotPath;
    }

    public List<String> getFilenamesForStep(String path, String url, String step) throws IOException {
        final String matcherPattern = "glob:**" + generateScreenshotFileNamePrefix(url, path) + "*_*_" + step + ".png";
        Path screenshotDirectory = getScreenshotDirectory();
        return getFileNamesMatchingPattern(screenshotDirectory, matcherPattern);
    }

    public void writeJsonReport(String reportJson) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory().toString() + "/report.json"))) {
            out.print(reportJson);
        }
    }

    public void writeHtmlReport(String htmlReport) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getScreenshotDirectory().toString() + "/report.html"))) {
            out.print(htmlReport);
        }
    }
}

