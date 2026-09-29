package de.otto.jlineup.service;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.HTMLReportWriter;
import de.otto.jlineup.report.Report;
import de.otto.jlineup.report.ReportGenerator;
import de.otto.jlineup.report.ScreenshotComparisonResult;
import de.otto.jlineup.report.ScreenshotsComparator;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

import static de.otto.jlineup.JLineupRunner.REPORT_LOG_NAME_KEY;
import static java.lang.invoke.MethodHandles.lookup;

/**
 * Imports a completed 'before' run (e.g. produced by the JLineup CLI in a CI workspace or
 * checked in as reference into a repository) into the web server's working directory.
 * <p>
 * After a successful import, the run looks exactly like a run whose 'before' step was executed
 * by this server: it is in state BEFORE_DONE, its report directory contains files.json,
 * the before screenshots, report_before.html and jlineup.log, and the 'after' step can be
 * triggered with the regular API call.
 * <p>
 * Supported archive formats: tar.gz, tar and zip. Both directory layouts are accepted:
 * <ul>
 *     <li>web server layout: files.json next to the {contextHash}/ screenshot directories</li>
 *     <li>CLI default layout: files.json next to a screenshots/ directory containing {contextHash}/</li>
 * </ul>
 */
@Service
public class BeforeRunImportService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    static final String METADATA_BEFORE_FILENAME = "metadata_before.json";
    static final String CLI_SCREENSHOTS_SUBDIRECTORY = "screenshots";
    private static final String BEFORE_SCREENSHOT_SUFFIX = "_" + BrowserStep.before.name() + FileService.PNG_EXTENSION;
    private static final int MAX_REPORTED_PROBLEMS = 20;

    private final JLineupService jLineupService;
    private final JLineupRunnerFactory jLineupRunnerFactory;
    private final JLineupWebProperties properties;

    @Autowired
    public BeforeRunImportService(JLineupService jLineupService,
                                  JLineupRunnerFactory jLineupRunnerFactory,
                                  JLineupWebProperties properties) {
        this.jLineupService = jLineupService;
        this.jLineupRunnerFactory = jLineupRunnerFactory;
        this.properties = properties;
    }

    /**
     * @param archive        the uploaded archive (tar.gz, tar or zip) containing a completed 'before' run
     * @param providedConfig the job config to use for the run. If null, the job config stored in the
     *                       uploaded files.json is used. The config has to produce the same screenshot
     *                       contexts as the one used to create the 'before' screenshots.
     * @return the status of the newly registered run (state BEFORE_DONE)
     */
    public JLineupRunStatus importBeforeRun(InputStream archive, JobConfig providedConfig) throws Exception {

        final String runId = UUID.randomUUID().toString();
        final Path workingDirectory = Path.of(properties.getWorkingDirectory());
        final String reportDirectory = properties.getReportDirectory().replace("{id}", runId);
        final String screenshotsDirectory = properties.getScreenshotsDirectory().replace("{id}", runId);
        final Path reportPath = workingDirectory.resolve(reportDirectory);
        final Path screenshotsPath = workingDirectory.resolve(screenshotsDirectory);

        Files.createDirectories(workingDirectory);
        final Path tempDirectory = Files.createTempDirectory(workingDirectory, "import-");

        JobConfig jobConfig;
        boolean success = false;
        try {
            extractArchive(archive, tempDirectory);

            final Path uploadedFileTrackerPath = locateFileTracker(tempDirectory);
            final FileTracker uploadedFileTracker = readFileTracker(uploadedFileTrackerPath);

            if (providedConfig != null) {
                jobConfig = providedConfig;
            } else if (uploadedFileTracker.jobConfig != null) {
                jobConfig = uploadedFileTracker.jobConfig.insertDefaults();
            } else {
                throw new InvalidBeforeRunArchiveException("No config was given and the uploaded " + FileService.DEFAULT_FILETRACKER_FILENAME + " doesn't contain a job-config.");
            }

            // Same checks as for a regular run (installed browser, allowed URL prefixes, config validation).
            // The runner itself is discarded, the 'after' runner is created when the 'after' step is triggered.
            jLineupRunnerFactory.createAfterRun(runId, jobConfig);

            final RunStepConfig runStepConfig = RunStepConfig.runStepConfigBuilder()
                    .withWorkingDirectory(properties.getWorkingDirectory())
                    .withReportDirectory(reportDirectory)
                    .withScreenshotsDirectory(screenshotsDirectory)
                    .withStep(RunStep.after)
                    .build();

            final List<ScreenshotContext> expectedContexts = BrowserUtils.buildScreenshotContextListFromConfigAndState(
                    RunStepConfig.copyOfBuilder(runStepConfig).withStep(RunStep.before).build(), jobConfig);

            final Path uploadedScreenshotsRoot = detectScreenshotsRoot(uploadedFileTrackerPath.getParent(), uploadedFileTracker);
            final FileTracker importedFileTracker = buildImportedFileTracker(jobConfig, uploadedFileTracker, expectedContexts, uploadedScreenshotsRoot);

            Files.createDirectories(reportPath);
            Files.createDirectories(screenshotsPath);
            copyScreenshots(importedFileTracker, uploadedScreenshotsRoot, screenshotsPath);
            Files.writeString(reportPath.resolve(FileService.DEFAULT_FILETRACKER_FILENAME), JacksonWrapper.serializeObject(importedFileTracker));

            writeLogAndReports(runStepConfig, jobConfig, importedFileTracker);
            success = true;
        } finally {
            deleteRecursivelyQuietly(tempDirectory);
            if (!success) {
                deleteRecursivelyQuietly(reportPath);
                deleteRecursivelyQuietly(screenshotsPath);
            }
        }

        return jLineupService.registerImportedBeforeRun(runId, jobConfig);
    }

    /*
     * Archive extraction
     */

    void extractArchive(InputStream rawInputStream, Path targetDirectory) throws InvalidBeforeRunArchiveException {
        final long maxBytes = properties.getImport().getMaxUncompressedSizeBytes();
        final int maxEntries = properties.getImport().getMaxEntries();

        long totalBytes = 0;
        int entryCount = 0;
        int extractedFiles = 0;

        try (ArchiveInputStream<?> archiveInputStream = openArchive(new BufferedInputStream(rawInputStream))) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (++entryCount > maxEntries) {
                    throw new InvalidBeforeRunArchiveException("Archive contains more than the allowed " + maxEntries + " entries.");
                }
                if (entry.isDirectory() || !isRegularFile(entry)) {
                    continue;
                }
                final String name = normalizeEntryName(entry.getName());
                if (name == null || !isRelevantForBeforeRun(name)) {
                    continue;
                }
                final Path destination = resolveSafely(targetDirectory, name);
                Files.createDirectories(destination.getParent());
                totalBytes += copyLimited(archiveInputStream, destination, maxBytes - totalBytes, maxBytes);
                extractedFiles++;
            }
        } catch (InvalidBeforeRunArchiveException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidBeforeRunArchiveException("Could not read uploaded archive (supported formats: tar.gz, tar, zip): " + e.getMessage(), e);
        }

        if (extractedFiles == 0) {
            throw new InvalidBeforeRunArchiveException("Uploaded archive doesn't contain any JLineup 'before' files (" + FileService.DEFAULT_FILETRACKER_FILENAME + ", *" + BEFORE_SCREENSHOT_SUFFIX + ").");
        }
        LOG.info("Extracted {} file(s) ({} bytes) from uploaded 'before' archive.", extractedFiles, totalBytes);
    }

    private static ArchiveInputStream<?> openArchive(BufferedInputStream inputStream) throws IOException {
        inputStream.mark(4);
        byte[] magic = inputStream.readNBytes(4);
        inputStream.reset();

        if (magic.length >= 2 && (magic[0] & 0xff) == 0x1f && (magic[1] & 0xff) == 0x8b) {
            return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
        }
        if (magic.length == 4 && magic[0] == 'P' && magic[1] == 'K' && magic[2] == 3 && magic[3] == 4) {
            return new ZipArchiveInputStream(inputStream);
        }
        return new TarArchiveInputStream(inputStream);
    }

    private static boolean isRegularFile(ArchiveEntry entry) {
        if (entry instanceof TarArchiveEntry tarEntry) {
            return tarEntry.isFile() && !tarEntry.isSymbolicLink() && !tarEntry.isLink();
        }
        if (entry instanceof ZipArchiveEntry zipEntry) {
            return !zipEntry.isUnixSymlink();
        }
        return true;
    }

    static String normalizeEntryName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.replace('\\', '/');
        while (name.startsWith("./")) {
            name = name.substring(2);
        }
        if (name.isEmpty() || name.startsWith("__MACOSX/")) {
            return null;
        }
        String fileName = name.substring(name.lastIndexOf('/') + 1);
        if (fileName.startsWith("._")) {
            // macOS AppleDouble resource fork files
            return null;
        }
        return name;
    }

    private static boolean isRelevantForBeforeRun(String name) {
        String fileName = name.substring(name.lastIndexOf('/') + 1);
        return fileName.equals(FileService.DEFAULT_FILETRACKER_FILENAME)
                || fileName.equals(METADATA_BEFORE_FILENAME)
                || fileName.endsWith(BEFORE_SCREENSHOT_SUFFIX);
    }

    private static long copyLimited(InputStream in, Path destination, long remainingBytes, long maxBytes) throws IOException, InvalidBeforeRunArchiveException {
        long written = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream out = Files.newOutputStream(destination)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                written += read;
                if (written > remainingBytes) {
                    throw new InvalidBeforeRunArchiveException("Uncompressed content of uploaded archive exceeds the allowed " + maxBytes + " bytes.");
                }
                out.write(buffer, 0, read);
            }
        }
        return written;
    }

    static Path resolveSafely(Path root, String relativePath) throws InvalidBeforeRunArchiveException {
        final Path normalizedRoot = root.toAbsolutePath().normalize();
        final Path resolved = normalizedRoot.resolve(relativePath.replace('\\', '/')).normalize();
        if (!resolved.startsWith(normalizedRoot) || resolved.equals(normalizedRoot)) {
            throw new InvalidBeforeRunArchiveException("Illegal path in uploaded 'before' run: '" + relativePath + "'");
        }
        return resolved;
    }

    /*
     * Layout detection and validation
     */

    static Path locateFileTracker(Path directory) throws IOException, InvalidBeforeRunArchiveException {
        final List<Path> candidates;
        try (Stream<Path> walker = Files.walk(directory)) {
            candidates = walker
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(FileService.DEFAULT_FILETRACKER_FILENAME))
                    .sorted(Comparator.comparingInt(Path::getNameCount))
                    .toList();
        }
        if (candidates.isEmpty()) {
            throw new InvalidBeforeRunArchiveException("Uploaded archive doesn't contain a " + FileService.DEFAULT_FILETRACKER_FILENAME + ".");
        }
        if (candidates.size() > 1 && candidates.get(0).getNameCount() == candidates.get(1).getNameCount()) {
            throw new InvalidBeforeRunArchiveException("Uploaded archive contains more than one " + FileService.DEFAULT_FILETRACKER_FILENAME + " on the same level, can't decide which one to use.");
        }
        return candidates.get(0);
    }

    private static FileTracker readFileTracker(Path path) throws InvalidBeforeRunArchiveException {
        try {
            FileTracker fileTracker = JacksonWrapper.readFileTrackerFile(path.toFile());
            if (fileTracker == null || fileTracker.contexts == null) {
                throw new InvalidBeforeRunArchiveException("Uploaded " + FileService.DEFAULT_FILETRACKER_FILENAME + " doesn't contain any screenshot contexts.");
            }
            return fileTracker;
        } catch (RuntimeException e) {
            throw new InvalidBeforeRunArchiveException("Could not read uploaded " + FileService.DEFAULT_FILETRACKER_FILENAME + ": " + rootMessage(e), e);
        }
    }

    /**
     * Paths in files.json are relative to the screenshots directory. In the web server layout, that is
     * the same directory as the one containing files.json, in the CLI default layout it is 'screenshots/'.
     */
    static Path detectScreenshotsRoot(Path fileTrackerDirectory, FileTracker fileTracker) {
        List<String> beforePaths = fileTracker.contexts.values().stream()
                .filter(c -> c.screenshots != null)
                .flatMap(c -> c.screenshots.values().stream())
                .map(m -> m.get(BrowserStep.before))
                .filter(p -> p != null)
                .toList();

        Path best = fileTrackerDirectory;
        long bestHits = -1;
        for (Path candidate : List.of(fileTrackerDirectory, fileTrackerDirectory.resolve(CLI_SCREENSHOTS_SUBDIRECTORY))) {
            long hits = beforePaths.stream().filter(p -> Files.isRegularFile(candidate.resolve(p.replace('\\', '/')))).count();
            if (hits > bestHits) {
                best = candidate;
                bestHits = hits;
            }
        }
        return best;
    }

    /**
     * Builds the file tracker for the imported run: only contexts that are part of the job config and only
     * 'before' screenshots are taken over. Fails if a context of the job config has no before screenshots
     * or if a referenced screenshot file is missing.
     */
    static FileTracker buildImportedFileTracker(JobConfig jobConfig, FileTracker uploaded, List<ScreenshotContext> expectedContexts, Path screenshotsRoot) throws InvalidBeforeRunArchiveException {
        final ConcurrentHashMap<String, ScreenshotContextFileTracker> contexts = new ConcurrentHashMap<>();
        final List<String> problems = new ArrayList<>();

        for (ScreenshotContext expected : expectedContexts) {
            final String hash = expected.contextHash();
            final ScreenshotContextFileTracker uploadedContext = uploaded.contexts.get(hash);
            if (uploadedContext == null || uploadedContext.screenshots == null) {
                problems.add("No 'before' screenshots for " + describe(expected) + " (context hash " + hash + ")");
                continue;
            }
            final ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> beforeScreenshots = new ConcurrentSkipListMap<>();
            for (Map.Entry<Integer, Map<BrowserStep, String>> positionEntry : uploadedContext.screenshots.entrySet()) {
                String beforePath = positionEntry.getValue() != null ? positionEntry.getValue().get(BrowserStep.before) : null;
                if (beforePath == null) {
                    continue;
                }
                beforePath = beforePath.replace('\\', '/');
                Path file = resolveSafely(screenshotsRoot, beforePath);
                if (!Files.isRegularFile(file)) {
                    problems.add("Missing screenshot file '" + beforePath + "' for " + describe(expected));
                    continue;
                }
                Map<BrowserStep, String> stepMap = new EnumMap<>(BrowserStep.class);
                stepMap.put(BrowserStep.before, beforePath);
                beforeScreenshots.put(positionEntry.getKey(), stepMap);
            }
            if (beforeScreenshots.isEmpty()) {
                problems.add("No 'before' screenshots for " + describe(expected) + " (context hash " + hash + ")");
                continue;
            }
            contexts.put(hash, ScreenshotContextFileTracker.screenshotContextFileTrackerBuilder()
                    .withScreenshotContext(uploadedContext.screenshotContext)
                    .withScreenshots(beforeScreenshots)
                    .build());
        }

        if (!problems.isEmpty()) {
            StringBuilder message = new StringBuilder("The uploaded 'before' run doesn't match the config (")
                    .append(problems.size()).append(" problem(s)). Was it created with the same config and browser?");
            problems.stream().limit(MAX_REPORTED_PROBLEMS).forEach(p -> message.append("\n - ").append(p));
            if (problems.size() > MAX_REPORTED_PROBLEMS) {
                message.append("\n - ...");
            }
            throw new IllegalArgumentException(message.toString());
        }

        final ConcurrentHashMap<BrowserStep, Set<String>> browsers = new ConcurrentHashMap<>();
        if (uploaded.browsers != null && uploaded.browsers.get(BrowserStep.before) != null) {
            Set<String> beforeBrowsers = ConcurrentHashMap.newKeySet();
            beforeBrowsers.addAll(uploaded.browsers.get(BrowserStep.before));
            browsers.put(BrowserStep.before, beforeBrowsers);
        }

        return new FileTracker(jobConfig, contexts, browsers);
    }

    private static String describe(ScreenshotContext context) {
        return context.url + context.urlSubPath
                + " [width " + context.deviceConfig.width
                + (context.deviceConfig.deviceName != null && !context.deviceConfig.deviceName.isEmpty() ? ", device " + context.deviceConfig.deviceName : "")
                + ", browser " + context.browserType + "]";
    }

    /*
     * Writing the run
     */

    private static void copyScreenshots(FileTracker fileTracker, Path sourceRoot, Path targetRoot) throws IOException, InvalidBeforeRunArchiveException {
        for (Map.Entry<String, ScreenshotContextFileTracker> contextEntry : fileTracker.contexts.entrySet()) {
            for (Map<BrowserStep, String> stepMap : contextEntry.getValue().screenshots.values()) {
                String relativePath = stepMap.get(BrowserStep.before);
                Path target = resolveSafely(targetRoot, relativePath);
                Files.createDirectories(target.getParent());
                Files.copy(resolveSafely(sourceRoot, relativePath), target, StandardCopyOption.REPLACE_EXISTING);
            }
            Path metadata = sourceRoot.resolve(contextEntry.getKey()).resolve(METADATA_BEFORE_FILENAME);
            if (Files.isRegularFile(metadata)) {
                Path target = resolveSafely(targetRoot, contextEntry.getKey() + "/" + METADATA_BEFORE_FILENAME);
                Files.createDirectories(target.getParent());
                Files.copy(metadata, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Writes jlineup.log, the "not finished" report.html and report_before.html - the same files a
     * regular 'before' step leaves behind. report_before.html is regenerated because the uploaded one
     * may reference screenshots with a different relative path (CLI layout).
     */
    private static void writeLogAndReports(RunStepConfig runStepConfig, JobConfig jobConfig, FileTracker fileTracker) throws IOException {
        MDC.put(REPORT_LOG_NAME_KEY, BrowserUtils.getFullPathToLogFile(runStepConfig));
        try {
            int screenshotCount = fileTracker.contexts.values().stream().mapToInt(c -> c.screenshots.size()).sum();
            LOG.info("Imported 'before' step from uploaded archive: {} screenshot context(s), {} screenshot(s).", fileTracker.contexts.size(), screenshotCount);
            Set<String> beforeBrowsers = fileTracker.browsers.get(BrowserStep.before);
            if (beforeBrowsers == null || beforeBrowsers.isEmpty()) {
                LOG.warn("Uploaded 'before' run doesn't contain information about the browser version that was used.");
            } else {
                LOG.info("Browser(s) used for uploaded 'before' screenshots: {}", String.join(", ", beforeBrowsers));
            }

            final FileService fileService = new FileService(runStepConfig, jobConfig);
            final HTMLReportWriter htmlReportWriter = new HTMLReportWriter(fileService);
            htmlReportWriter.writeNotFinishedReport(runStepConfig, jobConfig);

            final ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, new ImageService());
            final Map<String, List<ScreenshotComparisonResult>> onlyBeforeResults = screenshotsComparator.compare();
            final Report report = new ReportGenerator(fileService).generateReport(onlyBeforeResults, jobConfig);
            htmlReportWriter.writeReportAfterBeforeStep(report);
            LOG.info("JLineup run finished for step 'before' (imported)");
        } finally {
            MDC.remove(REPORT_LOG_NAME_KEY);
        }
    }

    private static void deleteRecursivelyQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walker = Files.walk(path)) {
            walker.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    LOG.warn("Could not delete {}", p, e);
                }
            });
        } catch (IOException e) {
            LOG.warn("Could not delete {}", path, e);
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
