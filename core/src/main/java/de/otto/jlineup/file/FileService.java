package de.otto.jlineup.file;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

import static de.otto.jlineup.file.FileUtils.clearDirectory;
import static java.lang.invoke.MethodHandles.lookup;

public class FileService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public final static String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    public static final String DIVIDER = "_";
    public static final String PNG_EXTENSION = ".png";
    private static final int MAX_URL_TO_FILENAME_LENGTH = 180;

    public static final String FILETRACKER_FILENAME = "files.json";
    public static final String REPORT_HTML_FILENAME = "report.html";
    public static final String LEGACY_REPORT_HTML_FILENAME = "report_legacy.html";
    public static final String REPORT_JSON_FILENAME = "report.json";

    private final RunStepConfig runStepConfig;

    public FileTracker getFileTracker() {
        return fileTracker;
    }

    public ScreenshotContext getRecordedContext(int hash) {
        return fileTracker.getScreenshotContextFileTracker(hash).screenshotContext;
    }

    private final FileTracker fileTracker;

    public FileService(RunStepConfig runStepConfig, JobConfig jobConfig) {
        this.runStepConfig = runStepConfig;
        if (runStepConfig.getStep() == Step.before) {
            this.fileTracker = FileTracker.create(jobConfig);
        } else {
            Path path = Paths.get(runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory(), FILETRACKER_FILENAME);
            this.fileTracker = JacksonWrapper.readFileTrackerFile(path.toFile());
        }
    }

    @VisibleForTesting
    Path createDirIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        Files.createDirectories(path);
        return path;
    }


    public void createOrClearReportDirectory() throws IOException {
        createOrClearDirectoryBelowWorkingDir(runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory());
    }

    @VisibleForTesting
    Path getScreenshotDirectory() {
        return Paths.get(String.format("%s/%s", runStepConfig.getWorkingDirectory(), runStepConfig.getScreenshotsDirectory()));
    }

    private Path getReportDirectory() {
        return Paths.get(String.format("%s/%s", runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory()));
    }

    public void createWorkingDirectoryIfNotExists() throws IOException {
        try {
            createDirIfNotExists(runStepConfig.getWorkingDirectory());
        } catch (IOException e) {
            throw new IOException("Could not create or open working directory.", e);
        }
    }

    public void createOrClearScreenshotsDirectory() throws IOException {
        createOrClearDirectoryBelowWorkingDir(runStepConfig.getWorkingDirectory(), runStepConfig.getScreenshotsDirectory());
    }

    private void createOrClearDirectoryBelowWorkingDir(String workingDirectory, String subDirectory) throws IOException {
        try {
            final String subDirectoryPath = workingDirectory + FILE_SEPARATOR + subDirectory;
            createDirIfNotExists(subDirectoryPath);
            clearDirectory(subDirectoryPath);
        } catch (IOException e) {
            throw new IOException("Could not create or open " + subDirectory + " directory.", e);
        }
    }

    @VisibleForTesting
    String generateScreenshotFileName(String url, String urlSubPath, int width, int yPosition, String type) {

        String fileName = generateScreenshotFileNamePrefix(url, urlSubPath)
                + String.format("%04d", width)
                + DIVIDER
                + String.format("%05d", yPosition)
                + DIVIDER
                + type;

        fileName = fileName + PNG_EXTENSION;

        return fileName;
    }

    private String generateScreenshotFileNamePrefix(String url, String urlSubPath) {

        @SuppressWarnings("deprecation") String hash = Hashing.sha1().hashString(url + urlSubPath, Charsets.UTF_8).toString().substring(0, 7);

        if (urlSubPath.equals("/") || urlSubPath.equals("")) {
            urlSubPath = "root";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String fileNamePrefix = url + DIVIDER + urlSubPath + DIVIDER;
        fileNamePrefix = fileNamePrefix.replace("://", DIVIDER);
        fileNamePrefix = fileNamePrefix.replaceAll("[^A-Za-z0-9\\-_]", DIVIDER);

        if (fileNamePrefix.length() > MAX_URL_TO_FILENAME_LENGTH) {
            fileNamePrefix = fileNamePrefix.substring(0, MAX_URL_TO_FILENAME_LENGTH) + DIVIDER;
        }

        fileNamePrefix = fileNamePrefix + hash + DIVIDER;

        return fileNamePrefix;
    }

    @VisibleForTesting
    String getScreenshotPath(String url, String urlSubPath, int width, int yPosition, String step) {
        return runStepConfig.getWorkingDirectory() + (runStepConfig.getWorkingDirectory().endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + runStepConfig.getScreenshotsDirectory() + (runStepConfig.getScreenshotsDirectory().endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + generateScreenshotFileName(url, urlSubPath, width, yPosition, step);
    }

    private String getScreenshotPath(String fileName) {
        return runStepConfig.getWorkingDirectory() + (runStepConfig.getWorkingDirectory().endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + runStepConfig.getScreenshotsDirectory() + (runStepConfig.getScreenshotsDirectory().endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR)
                + fileName;
    }

    public BufferedImage readScreenshot(String fileName) throws IOException {
        return ImageIO.read(new File(getScreenshotPath(fileName)));
    }

    private static void writeScreenshot(String fileName, BufferedImage image) throws IOException {
        LOG.debug("Writing screenshot to {}", fileName);
        File screenshotFile = new File(fileName);
        ImageIO.write(image, "png", screenshotFile);
    }

    public String writeScreenshot(ScreenshotContext screenshotContext, BufferedImage image,
                                  int yPosition) throws IOException {

        createDirIfNotExists(getScreenshotDirectory().toString() + FILE_SEPARATOR + screenshotContext.contextHash());
        String fileName = getScreenshotFilenameBelowScreenshotsDir(screenshotContext, yPosition);
        writeScreenshot(Paths.get(getScreenshotDirectory().toString(), fileName).toString(), image);
        fileTracker.addScreenshot(screenshotContext, fileName, yPosition);
        return fileName;
    }

    private String getScreenshotFilenameBelowScreenshotsDir(ScreenshotContext screenshotContext, int yPosition) {
        return screenshotContext.contextHash() + FILE_SEPARATOR + generateScreenshotFileName(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.deviceConfig.width, yPosition, screenshotContext.step.name());
    }

    public String getRelativePathFromReportDirToScreenshotsDir() {
        Path screenshotDirectory = getScreenshotDirectory().toAbsolutePath();
        Path reportDirectory = getReportDirectory().toAbsolutePath();
        Path relative = reportDirectory.relativize(screenshotDirectory);
        return relative.toString().equals("") ? "" : relative.toString() + FILE_SEPARATOR;
    }

    public void writeJsonReport(String reportJson) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory().toString() + FILE_SEPARATOR + REPORT_JSON_FILENAME))) {
            out.print(reportJson);
        }
    }

    public void writeHtmlReport(String htmlReport) throws FileNotFoundException {
        writeHtmlReport(htmlReport, LEGACY_REPORT_HTML_FILENAME);
    }

    public void writeHtmlReport(String htmlReport, String filename) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory().toString() + FILE_SEPARATOR + filename))) {
            out.print(htmlReport);
        }
    }

    public void writeHtml(String html, Step step) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory().toString() + FILE_SEPARATOR + step + ".html"))) {
            out.print(html);
        }
    }

    public void writeFileTrackerData() throws IOException {
        Path path = Paths.get(getReportDirectory().toString(), FILETRACKER_FILENAME);
        Files.write(path, JacksonWrapper.serializeObject(fileTracker).getBytes());
    }

    public void setBrowserAndVersion(ScreenshotContext screenshotContext, String browserAndVersion) {
        fileTracker.setBrowserAndVersion(screenshotContext, browserAndVersion);
    }

    public Map<Step, String> getBrowsers() {
        return fileTracker.browsers;
    }
}

