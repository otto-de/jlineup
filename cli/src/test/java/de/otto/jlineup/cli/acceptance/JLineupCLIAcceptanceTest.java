package de.otto.jlineup.cli.acceptance;

import com.google.gson.Gson;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.cli.Main;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.report.Report;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class JLineupCLIAcceptanceTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private ByteArrayOutputStream systemOutCaptor = new ByteArrayOutputStream();
    private ByteArrayOutputStream systemErrCaptor = new ByteArrayOutputStream();

    private static final String CWD = Paths.get(".").toAbsolutePath().normalize().toString();

    private PrintStream stdout = System.out;
    private PrintStream stderr = System.err;

    private Path tempDirectory;

    private Gson gson = new Gson();

    @Before
    public void setUpStreams() {

        systemOutCaptor = new ByteArrayOutputStream();
        systemErrCaptor = new ByteArrayOutputStream();

        MirrorOutputStream teeOut = new MirrorOutputStream(stdout, systemOutCaptor);
        MirrorOutputStream teeErr = new MirrorOutputStream(stderr, systemErrCaptor);

        System.setOut(new PrintStream(teeOut));
        System.setErr(new PrintStream(teeErr));
    }

    @Before
    public void createTempDir() throws IOException {
        tempDirectory = Files.createTempDirectory("jlineup-acceptance-test-");
    }

    @After
    public void cleanUpStreams() throws IOException {
        System.setOut(stdout);
        System.setErr(stderr);

        systemOutCaptor.close();
        systemErrCaptor.close();
    }

    @After
    public void deleteTempDir() throws Exception {
        deleteDir(tempDirectory);
    }

    @Test
    public void shouldExitWithExitStatus1IfConfigHasNoUrls() throws Exception {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(systemErrCaptor.toString(), containsString("No URLs configured.")));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_no_urls.lineup.json"});
    }

    @Test
    public void shouldExitWithExitStatus1IfThereIsAJSException() throws Exception {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(combinedOutput(), containsString("doesnotexist")));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_js.lineup.json", "--replace-in-url=###CWD###=" + CWD});
    }

    @Test
    public void shouldExitWithExitStatus1IfThereIsAMalformedUrlInFirefox() throws Exception {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(combinedOutput(), containsString("Reached error page")));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_url_firefox.lineup.json"});
    }

    @Test
    public void shouldExitWithExitStatus1IfThereIsAMalformedUrlInChrome() throws Exception {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(combinedOutput(), anyOf(containsString("ERR_NAME_RESOLUTION_FAILED"), containsString("ERR_NAME_NOT_RESOLVED"))));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_url_chrome.lineup.json"});
    }

    @Test
    public void shouldExitWithExitStatus1IfThereIsAGlobalTimeout() throws Exception {
        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(combinedOutput(), containsString("Timeout")));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_timeout.lineup.json"});
    }

    @Test
    public void shouldOpenTestPage() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--url", "file://" + CWD + "/src/test/resources/acceptance/webpage/test.html"});
    }


    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_WithChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
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
    public void shouldRunJLineupWithSomeDevices_WithChrome() throws Exception {

        System.err.println("Using " + tempDirectory.toString());

        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--chrome-parameter", "--hide-scrollbars", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_devices.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--chrome-parameter", "--hide-scrollbars", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_devices.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path filesJson = Paths.get(tempDirectory.toString(), "report", "files.json");
        assertThat("Filetracker file exists", Files.exists(filesJson));

        FileTracker fileTracker = JacksonWrapper.readFileTrackerFile(filesJson.toFile());
        assertThat(fileTracker.contexts.size(), is(3));
        fileTracker.contexts.forEach((k, v) -> {
            DeviceConfig deviceConfig = v.screenshotContext.deviceConfig;
            String filename = v.screenshots.get(0).get(BrowserStep.before);
            if (deviceConfig.deviceName.equals("iPhone X")) {
                checkScreenshotSize(filename, 1125, 2436); //iPhone X screen size
            } else if (deviceConfig.deviceName.equals("MOBILE")) {
                checkScreenshotSize(filename, 1500, 3000);
            } else if (deviceConfig.deviceName.equals("DESKTOP")) {
                checkScreenshotSize(filename, 2000, 2000);
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
    public void shouldPassCommandLineParametersToChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before", "--chrome-parameter", "--user-agent=\"fakeuseragent\""});
        assertThat(systemOutCaptor.toString(), containsString("User agent: \"fakeuseragent\""));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_WithChromeHeadless() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-headless.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
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
    public void shouldRunJLineupWithWaitForSelectors() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));
    }

    @Test
    public void shouldRemoveNodesAndShouldCalculateStableContextHashAlthoughEffectiveUrlChangesThroughReplaceInUrlFeature() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-remove_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=1", "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-remove_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=2", "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, is(0.0d));
    }

    @Test
    public void shouldFailBecauseSelectorNotFound() throws Exception {

        exit.expectSystemExitWithStatus(1);
        exit.checkAssertionAfterwards(() -> assertThat(combinedOutput(), containsString("Didn't find element with selector '#willNeverBeHere'.")));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors_fails_with_error.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
    }

    @Test
    public void shouldNotFailBecauseSelectorNotFound() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors_fails_but_no_error.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        //No exception
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_WithFirefox() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
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
    public void shouldRunJLineupWithTestPageThatDoesntChange_WithDifferentConfigsButSamePages_FixesContextHashBug() throws Exception {
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
    public void shouldRunJLineupWithTestPageThatDoesntChange_ReportFormat2() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_reportv2.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_reportv2.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

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
    public void shouldRunJLineupWithTestPageThatDoesntChange_LegacyReportFormat() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_legacy.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_legacy.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

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
    public void shouldRunJLineupWithTestPageThatDoesntChange_DefaultReportFormat() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

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
    public void shouldRunJLineupWithMergedConfig() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--merge-config", "src/test/resources/acceptance/acceptance-merge.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--merge-config", "src/test/resources/acceptance/acceptance-merge.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        assertThat(systemOutCaptor.toString(), containsString("test.html"));
        assertThat(systemOutCaptor.toString(), containsString("logo.html"));
        assertThat(systemOutCaptor.toString(), containsString("1234"));
        assertThat(systemOutCaptor.toString(), containsString("5678"));
        assertThat(systemOutCaptor.toString(), not(containsString("800")));
        assertThat(systemOutCaptor.toString(), containsString("Sum of overall screenshot differences: 0.0 (0 %)"));
    }

    @Test
    public void shouldNotMakeScreenshotsTwiceBecauseKeepExistingIsUsed() throws Exception {
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
    public void shouldRunJLineupWithTestPageThatHasAVeryLongPath() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_long_url.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_long_url.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});
    }

    @Test
    public void shouldPrintConfig() {
        exit.checkAssertionAfterwards(() -> assertThat(systemOutCaptor.toString(), containsString("https://www.example.com")));
        exit.expectSystemExitWithStatus(0);
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--print-config"});
    }

    @Test
    public void shouldPrintExampleConfig() {
        exit.checkAssertionAfterwards(() -> assertThat(systemOutCaptor.toString(), containsString(JobConfig.prettyPrintWithAllFields(JobConfig.exampleConfig()))));
        exit.expectSystemExitWithStatus(0);
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--print-example"});
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
