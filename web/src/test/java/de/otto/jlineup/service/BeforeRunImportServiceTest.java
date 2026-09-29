package de.otto.jlineup.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.PathConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.State;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BeforeRunImportServiceTest {

    private static final String BROWSER_VERSION = "Chrome 147.0.7727.138";
    // Long URL and path to force file names > 100 chars (needs PAX/GNU long name support in tar)
    private static final String URL = "https://www.example.com";
    private static final String LONG_PATH = "/some/rather/long/path/to/a/page/that/produces/a/very/long/screenshot/file/name/in/the/before/run?with=query&parameters=true";

    @TempDir
    Path workingDir;

    @TempDir
    Path sourceDir;

    private BeforeRunImportService testee;
    private JLineupService jLineupService;
    private JLineupWebProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JLineupWebProperties();
        properties.setWorkingDirectory(workingDir.toString() + "/");
        properties.setInstalledBrowsers(List.of(Browser.Type.CHROME_HEADLESS));
        properties.setAllowedUrlPrefixes(List.of("https://www.example.com"));
        JLineupRunnerFactory runnerFactory = new JLineupRunnerFactory(properties);
        jLineupService = new JLineupService(runnerFactory, properties, mock(RunPersistenceService.class));
        testee = new BeforeRunImportService(jLineupService, runnerFactory, properties);
    }

    @Test
    void shouldImportCliLayoutFromTarGzUsingConfigFromFilesJson() throws Exception {
        JobConfig jobConfig = config(List.of(600, 1200));
        FileTracker tracker = createBeforeRun(sourceDir, "screenshots", jobConfig, true);

        JLineupRunStatus status = testee.importBeforeRun(new ByteArrayInputStream(tarGz(sourceDir)), null);

        assertThat(status.getState(), is(State.BEFORE_DONE));
        assertThat(jLineupService.getRun(status.getId()).orElseThrow().getState(), is(State.BEFORE_DONE));
        assertThat(status.getReports().getHtmlUrl(), is("/reports/report-" + status.getId() + "/report_before.html"));

        Path runDir = workingDir.resolve("report-" + status.getId());
        assertTrue(Files.isRegularFile(runDir.resolve("files.json")));
        assertTrue(Files.isRegularFile(runDir.resolve("report_before.html")));
        assertTrue(Files.isRegularFile(runDir.resolve("report.html")));
        tracker.contexts.forEach((hash, ctx) -> ctx.screenshots.values().forEach(m ->
                assertTrue(Files.isRegularFile(runDir.resolve(m.get(BrowserStep.before))), "missing " + m.get(BrowserStep.before))));

        FileTracker imported = JacksonWrapper.readFileTrackerFile(runDir.resolve("files.json").toFile());
        assertThat(imported.contexts.keySet(), is(tracker.contexts.keySet()));
        assertThat(imported.browsers.get(BrowserStep.before), is(Set.of(BROWSER_VERSION)));
        assertFalse(imported.browsers.containsKey(BrowserStep.after));
        imported.contexts.values().forEach(ctx -> ctx.screenshots.values().forEach(m -> assertThat(m.keySet(), is(Set.of(BrowserStep.before)))));

        // Report shows the browser version of the uploaded before run and references screenshots in web layout
        String reportBefore = Files.readString(runDir.resolve("report_before.html"));
        assertThat(reportBefore, containsString(BROWSER_VERSION));
        assertFalse(reportBefore.contains("screenshots/"));

        // No leftovers of the temporary extraction
        try (Stream<Path> entries = Files.list(workingDir)) {
            assertFalse(entries.anyMatch(p -> p.getFileName().toString().startsWith("import-")));
        }
    }

    @Test
    void shouldImportWebLayoutFromZipWithProvidedConfig() throws Exception {
        JobConfig jobConfig = config(List.of(800));
        createBeforeRun(sourceDir, "", jobConfig, false);

        JobConfig providedConfig = JobConfig.copyOfBuilder(jobConfig).withMessage("Commit abc123").build();
        JLineupRunStatus status = testee.importBeforeRun(new ByteArrayInputStream(zip(sourceDir)), providedConfig);

        assertThat(status.getState(), is(State.BEFORE_DONE));
        assertThat(status.getJobConfig().message, is("Commit abc123"));
        FileTracker imported = JacksonWrapper.readFileTrackerFile(workingDir.resolve("report-" + status.getId()).resolve("files.json").toFile());
        assertThat(imported.jobConfig.message, is("Commit abc123"));
    }

    @Test
    void shouldRejectBeforeRunNotMatchingTheConfig() throws Exception {
        createBeforeRun(sourceDir, "screenshots", config(List.of(600)), true);
        byte[] archive = tarGz(sourceDir);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testee.importBeforeRun(new ByteArrayInputStream(archive), config(List.of(600, 1200))));

        assertThat(exception.getMessage(), containsString("width 1200"));
        assertNoRunDirectoryLeft();
    }

    @Test
    void shouldRejectMissingScreenshotFile() throws Exception {
        FileTracker tracker = createBeforeRun(sourceDir, "screenshots", config(List.of(600)), true);
        String firstScreenshot = tracker.contexts.values().iterator().next().screenshots.firstEntry().getValue().get(BrowserStep.before);
        Files.delete(sourceDir.resolve("screenshots").resolve(firstScreenshot));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testee.importBeforeRun(new ByteArrayInputStream(tarGz(sourceDir)), null));

        assertThat(exception.getMessage(), containsString("Missing screenshot file"));
        assertNoRunDirectoryLeft();
    }

    @Test
    void shouldRejectUrlsThatAreNotAllowed() throws Exception {
        createBeforeRun(sourceDir, "", config(List.of(600)), false);
        properties.setAllowedUrlPrefixes(List.of("https://www.otto.de"));

        assertThrows(IllegalArgumentException.class, () -> testee.importBeforeRun(new ByteArrayInputStream(tarGz(sourceDir)), null));
        assertNoRunDirectoryLeft();
    }

    @Test
    void shouldRejectArchiveWithoutFilesJson() throws Exception {
        createBeforeRun(sourceDir, "", config(List.of(600)), false);
        Files.delete(sourceDir.resolve("files.json"));

        InvalidBeforeRunArchiveException exception = assertThrows(InvalidBeforeRunArchiveException.class,
                () -> testee.importBeforeRun(new ByteArrayInputStream(tarGz(sourceDir)), null));
        assertThat(exception.getMessage(), containsString("files.json"));
    }

    @Test
    void shouldRejectPathTraversal() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
            byte[] content = "evil".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry("../../evil_before.png", true);
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }

        InvalidBeforeRunArchiveException exception = assertThrows(InvalidBeforeRunArchiveException.class,
                () -> testee.importBeforeRun(new ByteArrayInputStream(bytes.toByteArray()), null));
        assertThat(exception.getMessage(), containsString("Illegal path"));
        assertFalse(Files.exists(workingDir.getParent().resolve("evil_before.png")));
    }

    @Test
    void shouldRejectGarbage() {
        assertThrows(InvalidBeforeRunArchiveException.class,
                () -> testee.importBeforeRun(new ByteArrayInputStream("this is no archive".getBytes(StandardCharsets.UTF_8)), null));
    }

    @Test
    void shouldRejectArchiveExceedingSizeLimit() throws Exception {
        createBeforeRun(sourceDir, "", config(List.of(600)), false);
        properties.getImport().setMaxUncompressedSizeBytes(10);

        InvalidBeforeRunArchiveException exception = assertThrows(InvalidBeforeRunArchiveException.class,
                () -> testee.importBeforeRun(new ByteArrayInputStream(tarGz(sourceDir)), null));
        assertThat(exception.getMessage(), containsString("exceeds"));
    }

    @Test
    void shouldNormalizeEntryNames() {
        assertThat(BeforeRunImportService.normalizeEntryName("./././abc/files.json"), is("abc/files.json"));
        assertThat(BeforeRunImportService.normalizeEntryName("abc\\def_before.png"), is("abc/def_before.png"));
        assertThat(BeforeRunImportService.normalizeEntryName("__MACOSX/abc/files.json"), is((String) null));
        assertThat(BeforeRunImportService.normalizeEntryName("abc/._files.json"), is((String) null));
    }

    /*
     * Helpers
     */

    private void assertNoRunDirectoryLeft() throws IOException {
        try (Stream<Path> entries = Files.list(workingDir)) {
            List<Path> leftovers = entries.filter(p -> p.getFileName().toString().startsWith("report-") || p.getFileName().toString().startsWith("import-")).toList();
            assertTrue(leftovers.isEmpty(), "Leftovers: " + leftovers);
        }
    }

    private static JobConfig config(List<Integer> widths) {
        return jobConfigBuilder()
                .withName("import-test")
                .withBrowser(Browser.Type.CHROME_HEADLESS)
                .withUrls(ImmutableMap.of(URL, urlConfigBuilder()
                        .withPaths(ImmutableList.of(PathConfig.of("/"), PathConfig.of(LONG_PATH)))
                        .withWindowWidths(widths)
                        .build()))
                .build()
                .insertDefaults();
    }

    /**
     * Writes a 'before' run like JLineup does: files.json in the report dir, screenshots in
     * {screenshotsSubDir}/{contextHash}/..., plus a stale after screenshot and some noise.
     */
    private static FileTracker createBeforeRun(Path reportDir, String screenshotsSubDir, JobConfig jobConfig, boolean includeAfterArtifacts) throws IOException {
        Path screenshotsDir = screenshotsSubDir.isEmpty() ? reportDir : reportDir.resolve(screenshotsSubDir);
        RunStepConfig runStepConfig = RunStepConfig.runStepConfigBuilder()
                .withWorkingDirectory(reportDir.toString())
                .withReportDirectory(".")
                .withScreenshotsDirectory(screenshotsSubDir.isEmpty() ? "." : screenshotsSubDir)
                .withStep(RunStep.before)
                .build();

        FileTracker tracker = FileTracker.create(jobConfig);
        List<ScreenshotContext> contexts = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (ScreenshotContext context : contexts) {
            for (int y : List.of(0, 800)) {
                String name = context.contextHash() + "/" + FileService.generateScreenshotFileNamePrefix(context.url, context.urlSubPath)
                        + String.format("%04d_%05d_before.png", context.deviceConfig.width, y);
                Path file = screenshotsDir.resolve(name);
                Files.createDirectories(file.getParent());
                ImageIO.write(image, "png", file.toFile());
                tracker.addScreenshot(context, name, y);
            }
            tracker.setBrowserAndVersion(context, BROWSER_VERSION);
            Files.writeString(screenshotsDir.resolve(context.contextHash()).resolve("metadata_before.json"), "{}");
            if (includeAfterArtifacts) {
                Files.writeString(screenshotsDir.resolve(context.contextHash()).resolve("stale_0000_00000_after.png"), "stale");
            }
        }
        if (includeAfterArtifacts) {
            // A full CLI run also contains after data - must be ignored by the import
            Map<BrowserStep, Set<String>> browsers = tracker.browsers;
            browsers.put(BrowserStep.after, ConcurrentHashMap.newKeySet());
            browsers.get(BrowserStep.after).add("Chrome 999");
            Files.writeString(reportDir.resolve("report.json"), "{}");
        }
        Files.writeString(reportDir.resolve("files.json"), JacksonWrapper.serializeObject(tracker));
        Files.writeString(reportDir.resolve("report_before.html"), "<html>old</html>");
        return tracker;
    }

    private static Map<String, Path> filesOf(Path dir) throws IOException {
        Map<String, Path> files = new LinkedHashMap<>();
        try (Stream<Path> walker = Files.walk(dir)) {
            walker.filter(Files::isRegularFile).forEach(p -> files.put("./" + dir.relativize(p).toString().replace('\\', '/'), p));
        }
        return files;
    }

    private static byte[] tarGz(Path dir) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, Path> file : filesOf(dir).entrySet()) {
                TarArchiveEntry entry = new TarArchiveEntry(file.getValue().toFile(), file.getKey());
                tar.putArchiveEntry(entry);
                Files.copy(file.getValue(), tar);
                tar.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] zip(Path dir) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(bytes)) {
            Map<String, Path> files = new HashMap<>(filesOf(dir));
            for (Map.Entry<String, Path> file : files.entrySet()) {
                zip.putArchiveEntry(new ZipArchiveEntry(file.getKey().substring(2)));
                Files.copy(file.getValue(), zip);
                zip.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }
}
