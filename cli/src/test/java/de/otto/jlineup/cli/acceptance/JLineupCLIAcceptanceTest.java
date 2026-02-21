package de.otto.jlineup.cli.acceptance;

import com.google.gson.Gson;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.cli.Main;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.report.Report;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class JLineupCLIAcceptanceTest {

    private ByteArrayOutputStream systemOutCaptor = new ByteArrayOutputStream();
    private ByteArrayOutputStream systemErrCaptor = new ByteArrayOutputStream();

    private static final String CWD = Paths.get(".").toAbsolutePath().normalize().toString();

    private final PrintStream stdout = System.out;
    private final PrintStream stderr = System.err;

    private Path tempDirectory;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUpStreams() {

        systemOutCaptor = new ByteArrayOutputStream();
        systemErrCaptor = new ByteArrayOutputStream();

        MirrorOutputStream teeOut = new MirrorOutputStream(stdout, systemOutCaptor);
        MirrorOutputStream teeErr = new MirrorOutputStream(stderr, systemErrCaptor);

        System.setOut(new PrintStream(teeOut));
        System.setErr(new PrintStream(teeErr));
    }

    @BeforeEach
    void createTempDir() throws IOException {
        tempDirectory = Files.createTempDirectory("jlineup-acceptance-test-");
    }

    @AfterEach
    void cleanUpStreams() throws IOException {
        System.setOut(stdout);
        System.setErr(stderr);

        systemOutCaptor.close();
        systemErrCaptor.close();
    }

    @AfterEach
    void deleteTempDir() throws Exception {
        deleteDir(tempDirectory);
    }

    @Test
    void shouldExitWithExitStatus1IfConfigHasNoUrls() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_no_urls.lineup.json"}));
        assertEquals(1, status);
        assertThat(systemErrCaptor.toString(), containsString("No URLs configured."));
    }

    @Test
    void shouldExitWithExitStatus1IfThereIsAJSException() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_js.lineup.json", "--replace-in-url=###CWD###=" + CWD}));
        assertEquals(1, status);
        assertThat(combinedOutput(), containsString("doesnotexist"));
    }

    @Test
    void shouldExitWithExitStatus1IfThereIsAMalformedUrlInFirefox() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_url_firefox.lineup.json"}));
        assertEquals(1, status);
        assertThat(combinedOutput(), containsString("Reached error page"));
    }

    @Test
    void shouldExitWithExitStatus1IfThereIsAMalformedUrlInChrome() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_url_chrome.lineup.json"}));
        assertEquals(1, status);
        assertThat(combinedOutput(), anyOf(containsString("ERR_NAME_RESOLUTION_FAILED"), containsString("ERR_NAME_NOT_RESOLVED")));
    }

    @Test
    void shouldExitWithExitStatus1IfThereIsAGlobalTimeout() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_timeout.lineup.json"}));
        assertEquals(1, status);
        assertThat(combinedOutput(), containsString("Timeout"));
    }

    @Test
    void shouldOpenTestPage() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--url", "file://" + CWD + "/src/test/resources/acceptance/webpage/test.html"});
    }


    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_WithChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});

        final Path reportBeforeHtml = Paths.get(tempDirectory.toString(), "report", "report_before.html");
        assertThat("Report Reference HTML exists", Files.exists(reportBeforeHtml));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithSomeDevices_WithChrome() throws Exception {

        System.err.println("Using " + tempDirectory.toString());

        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--chrome-parameter", "--hide-scrollbars", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_devices.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--chrome-parameter", "--hide-scrollbars", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_devices.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path filesJson = Paths.get(tempDirectory.toString(), "report", "files.json");
        assertThat("Filetracker file exists", Files.exists(filesJson));

        FileTracker fileTracker = JacksonWrapper.readFileTrackerFile(filesJson.toFile());
        assertThat(fileTracker.contexts.size(), is(4));
        fileTracker.contexts.forEach((k, v) -> {
            DeviceConfig deviceConfig = v.screenshotContext.deviceConfig;
            String filename = v.screenshots.get(0).get(BrowserStep.before);
            if (deviceConfig.deviceName.equals("iPhone 14 Pro Max")) {
                checkScreenshotSize(filename, 1290, 2796); //iPhone 14 Pro screen size
            } else if ("mobile1".equals(deviceConfig.userAgent)) {
                checkScreenshotSize(filename, 1500, 3000);
            } else if ("mobile2".equals(deviceConfig.userAgent)) {
                checkScreenshotSize(filename, 320, 480);
            } else if (deviceConfig.deviceName.equals("DESKTOP")) {
                //This checks if the magic chrome resize function works
                checkScreenshotSize(filename, 1000, 1000);
            } else {
                fail("Context should not be here");
            }
        });
    }

    private void checkScreenshotSize(String filename, int width, int height) {
        try {
            String filepath = tempDirectory.toString() + "/report/screenshots/" + filename;
            System.out.println("Trying to read: " + filepath);
            BufferedImage image = ImageIO.read(new File(filepath));
            assertThat(image.getWidth(), is(width));
            assertThat(image.getHeight(), is(height));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void shouldPassCommandLineParametersToChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before", "--chrome-parameter", "--user-agent=\"fakeuseragent\""});
        assertThat(systemOutCaptor.toString(), containsString("User agent: \"fakeuseragent\""));
    }

    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_WithChromeHeadless() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-headless.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});

        final Path reportBeforeHtml = Paths.get(tempDirectory.toString(), "report", "report_before.html");
        assertThat("Report Reference HTML exists", Files.exists(reportBeforeHtml));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-headless.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithWaitForSelectors() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));
    }

    @Test
    void shouldRemoveNodesAndShouldCalculateStableContextHashAlthoughEffectiveUrlChangesThroughReplaceInUrlFeature() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-remove_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=1", "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-remove_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=2", "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));
    }

    @Test
    void shouldFailBecauseSelectorNotFound() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors_fails_with_error.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"}));
        assertEquals(1, status);
        assertThat(combinedOutput(), containsString("Didn't find element with selector '#willNeverBeHere'."));
    }

    @Test
    void shouldNotFailBecauseSelectorNotFound() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors_fails_but_no_error.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        //No exception
    }

    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_WithFirefox() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});

        final Path reportBeforeHtml = Paths.get(tempDirectory.toString(), "report", "report_before.html");
        assertThat("Report Reference HTML exists", Files.exists(reportBeforeHtml));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_WithDifferentConfigsButSamePages_FixesContextHashBug() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox_variant1.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox_variant2.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_ReportFormat2() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_reportv2.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_reportv2.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_LegacyReportFormat() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_legacy.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_legacy.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        @SuppressWarnings("unchecked") final ArrayList<Map> report = gson.fromJson(jsonReportText, ArrayList.class);
        assertThat(report.get(0).get("difference"), is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithTestPageThatDoesntChange_DefaultReportFormat() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));

        assertThat(systemOutCaptor.toString(), containsString("Sum of screenshot differences for file://###CWD###/src/test/resources/acceptance/webpage/: 0.0 (0 %)"));
        assertThat(systemOutCaptor.toString(), containsString("Sum of overall screenshot differences: 0.0 (0 %)"));
    }

    @Test
    void shouldRunJLineupWithMergedConfig() throws Exception {

        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--merge-config", "src/test/resources/acceptance/acceptance-merge.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--merge-config", "src/test/resources/acceptance/acceptance-merge.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        assertThat(systemOutCaptor.toString(), containsString("test.html"));
        assertThat(systemOutCaptor.toString(), containsString("test_remove1.html"));
        assertThat(systemOutCaptor.toString(), containsString("1234x999"));
        assertThat(systemOutCaptor.toString(), not(containsString("800x800")));
        assertThat(systemOutCaptor.toString(), containsString("Sum of overall screenshot differences: 0.0 (0 %)"));
    }

    @Test
    void shouldNotMakeScreenshotsTwiceBecauseKeepExistingIsUsed() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});

        FileTracker fileTracker = JacksonWrapper.readFileTrackerFile(new File(tempDirectory.toString() + "/report/files.json"));
        Set<Map.Entry<Integer, ScreenshotContextFileTracker>> entries = fileTracker.getContexts().entrySet();
        String screenshotFileName = entries.stream().findFirst().get().getValue().screenshots.values().stream().findFirst().get().get(BrowserStep.before);

        FileTime lastModifiedTimeBeforeSecondRun = Files.getLastModifiedTime(Paths.get(tempDirectory.toString(), "report", "screenshots", screenshotFileName));
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before", "--keep-existing"});

        FileTime lastModifiedTimeAfterSecondRun = Files.getLastModifiedTime(Paths.get(tempDirectory.toString(), "report", "screenshots", screenshotFileName));

        assertThat(lastModifiedTimeAfterSecondRun, is(lastModifiedTimeBeforeSecondRun));
    }

    @Test
    void shouldRunJLineupWithTestPageThatHasAVeryLongPath() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_long_url.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_long_url.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});
    }

    @Test
    void shouldPrintConfig() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--print-config"}));
        assertEquals(0, status);
        assertThat(systemOutCaptor.toString(), containsString("https://www.example.com"));
    }

    @Test
    void shouldPrintExampleConfig() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--print-example"}));
        assertEquals(0, status);
        assertThat(systemOutCaptor.toString(), containsString(JobConfig.prettyPrintWithAllFields(JobConfig.exampleConfig())));
    }

    private String getTextFileContentAsString(Path reportJson) throws IOException {
        final List<String> reportJsonLines = Files.readAllLines(reportJson);
        return reportJsonLines.stream().collect(Collectors.joining());
    }

    private void deleteDir(Path path) throws Exception {

        LinkedList<File> files = new LinkedList<>();

        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(files::add);
        }

        files.forEach(file -> {
            if (!file.delete()) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Couldn't delete file " + file.getAbsolutePath());
            }
        });
    }

    private String combinedOutput() {
        return systemOutCaptor.toString() + systemErrCaptor.toString();
    }

    private File getFile(String name) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        return new File(url.getPath());
    }

    private String getPath(String name) {
        File file = getFile(name);
        return file.getAbsolutePath();
    }

}
