package de.otto.jlineup.file;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

import static de.otto.jlineup.file.FileUtils.clearDirectory;
import static de.otto.jlineup.file.ScreenshotContextFileTracker.screenshotContextFileTrackerBuilder;
import static java.lang.invoke.MethodHandles.lookup;

public class FileService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public final static String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    public static final String DIVIDER = "_";
    public static final String PNG_EXTENSION = ".png";
    private static final int MAX_URL_TO_FILENAME_LENGTH = 180;

    public static final String DEFAULT_FILETRACKER_FILENAME = "files.json";
    public static final String REPORT_HTML_FILENAME = "report.html";
    public static final String LEGACY_REPORT_HTML_FILENAME = "report_legacy.html";
    public static final String REPORT_JSON_FILENAME = "report.json";

    private final RunStepConfig runStepConfig;

    private final FileTracker fileTracker;

    private final String filetrackerFilename;

    public FileTracker getFileTracker() {
        return fileTracker;
    }

    public ScreenshotContext getRecordedContext(int hash) {
        return fileTracker.getScreenshotContextFileTracker(hash).screenshotContext;
    }

    public FileService(RunStepConfig runStepConfig, JobConfig jobConfig) {
        this(runStepConfig, jobConfig, DEFAULT_FILETRACKER_FILENAME);
    }

    public FileService(RunStepConfig runStepConfig, JobConfig jobConfig, String filetrackerFilename) {
        this.runStepConfig = runStepConfig;
        this.filetrackerFilename = filetrackerFilename;
        if (!runStepConfig.isKeepExisting() && (runStepConfig.getStep() == RunStep.before || runStepConfig.getStep() == RunStep.after_only)) {
            //Only create fresh file tracker file when step is before or after_only and --keep-existing is not used.
            this.fileTracker = FileTracker.create(jobConfig);
        } else {
            //Step is after or --keep-existing is set, which leads to reading old file tracker file from former before step, but setting new jobConfig!
            Path path = Paths.get(runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory(), this.filetrackerFilename);
            FileTracker fileTrackerFromFile;
            try {
                fileTrackerFromFile = JacksonWrapper.readFileTrackerFile(path.toFile());
            } catch (Exception e) {
                if (e.getCause() instanceof FileNotFoundException && runStepConfig.isKeepExisting()) {
                    LOG.info("Nothing to keep although --keep-existing was specified. No former run found.");
                    fileTrackerFromFile = FileTracker.create(jobConfig);
                } else {
                    throw e;
                }
            }
            if (runStepConfig.isKeepExisting()) {
                this.fileTracker = new FileTracker(jobConfig, fileTrackerFromFile.contexts, fileTrackerFromFile.browsers);
            } else {
                this.fileTracker = fileTrackerFromFile;
            }
        }
    }

    @VisibleForTesting
    Path createDirIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        Files.createDirectories(path);
        return path;
    }


    public void createOrClearReportDirectory(boolean keepExisting) throws IOException {
        createOrClearDirectoryBelowWorkingDir(runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory(), keepExisting);
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

    public void createOrClearScreenshotsDirectory(boolean keepExisting) throws IOException {
        createOrClearDirectoryBelowWorkingDir(runStepConfig.getWorkingDirectory(), runStepConfig.getScreenshotsDirectory(), keepExisting);
    }

    private void createOrClearDirectoryBelowWorkingDir(String workingDirectory, String subDirectory, boolean keepExisting) throws IOException {
        try {
            final String subDirectoryPath = workingDirectory + FILE_SEPARATOR + subDirectory;
            createDirIfNotExists(subDirectoryPath);
            if (!keepExisting) {
                clearDirectory(subDirectoryPath);
            }
        } catch (IOException e) {
            throw new IOException("Could not create or open " + subDirectory + " directory.", e);
        }
    }

    @VisibleForTesting
    static String generateScreenshotFileName(String url, String urlSubPath, int width, int yPosition, String type) {

        String fileName = generateScreenshotFileNamePrefix(url, urlSubPath)
                + String.format("%04d", width)
                + DIVIDER
                + String.format("%05d", yPosition)
                + DIVIDER
                + type;

        fileName = fileName + PNG_EXTENSION;

        return fileName;
    }

    public static String generateScreenshotFileNamePrefix(String url, String urlSubPath) {

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
        fileNamePrefix = fileNamePrefix.replace("\\", "").replace("/", "").replace(".", "");

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
        return relative.toString().equals("") ? "" : relative + FILE_SEPARATOR;
    }

    public void writeJsonReport(String reportJson) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory() + FILE_SEPARATOR + REPORT_JSON_FILENAME))) {
            out.print(reportJson);
        }
    }

    public void writeHtmlReportLegacy(String htmlReport) throws FileNotFoundException {
        writeHtmlReport(htmlReport, LEGACY_REPORT_HTML_FILENAME);
    }

    public void writeHtmlReport(String htmlReport, String filename) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory() + FILE_SEPARATOR + filename))) {
            out.print(htmlReport);
        }
    }

    public void writeHtml(String html, RunStep step) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(new FileOutputStream(getReportDirectory() + FILE_SEPARATOR + step + ".html"))) {
            out.print(html);
        }
    }

    public void writeFileTrackerData() throws IOException {
        writeFileTrackerData(this.filetrackerFilename, this.fileTracker);
    }

    private void writeFileTrackerData(String filetrackerFilename, FileTracker fileTracker) throws IOException {
        Path path = Paths.get(getReportDirectory().toString(), filetrackerFilename);
        Files.write(path, JacksonWrapper.serializeObject(fileTracker).getBytes());
    }

    public void setBrowserAndVersion(ScreenshotContext screenshotContext, String browserAndVersion) {
        fileTracker.setBrowserAndVersion(screenshotContext, browserAndVersion);
    }

    public Map<BrowserStep, String> getBrowsers() {
        return fileTracker.browsers;
    }

    public void writeFileTrackerDataForScreenshotContextOnly(ScreenshotContext context) throws IOException {
        Path path = Paths.get(getScreenshotDirectory().toString(), FILE_SEPARATOR, String.valueOf(context.contextHash()), "metadata_" + context.step.name() + ".json");
        Files.write(path, JacksonWrapper.serializeObject(fileTracker.contexts.get(context.contextHash())).getBytes());
    }

    public void mergeContextFileTrackersIntoFileTracker(Path directory, FilenameFilter filenameFilter) throws IOException {
        File dir = directory.toFile();
        File[] files = dir.listFiles(filenameFilter);
        assert files != null;
        for (File file : files) {
            FileTracker part = JacksonWrapper.readFileTrackerFile(file);

            //fileTracker.contexts.putAll(part.contexts);
            part.contexts.forEach((contextHash, screenshotContextFileTracker) -> fileTracker.contexts.merge(contextHash, screenshotContextFileTracker, (existingContext, newContext) -> screenshotContextFileTrackerBuilder()
                    .withScreenshotContext(existingContext.screenshotContext)
                    .withScreenshots(existingContext.screenshots)
                    .addScreenshots(newContext.screenshots)
                    .build()));

            fileTracker.browsers.putAll(part.browsers);
        }
        writeFileTrackerData();
    }
}

