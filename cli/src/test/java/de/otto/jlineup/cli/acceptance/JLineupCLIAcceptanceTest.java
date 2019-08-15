package de.otto.jlineup.cli.acceptance;

import com.google.gson.Gson;
import de.otto.jlineup.cli.Main;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.report.Report;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

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

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_wrong_js.lineup.json", "--replace-in-url###CWD###=" + CWD});
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
        exit.checkAssertionAfterwards(() -> assertThat(combinedOutput(), CoreMatchers.anyOf(containsString("ERR_NAME_RESOLUTION_FAILED"), containsString("ERR_NAME_NOT_RESOLVED"))));

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
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    public void shouldPassCommandLineParametersToChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before", "--chrome-parameter", "--user-agent=\"fakeuseragent\""});
        assertThat(systemOutCaptor.toString(), containsString("User agent: \"fakeuseragent\""));
    }

    @Test
    @Ignore //New IgnoreAntiAliasing option handles this, chrome does not render deterministically all the time
    public void shouldRenderLogoDeterministically_WithChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_svg.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_svg.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    @Ignore //New IgnoreAntiAliasing option handles this, chrome does not render deterministically all the time
    public void shouldRenderProgressiveJPEGsDeterministically_WithChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_progressive_jpg.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_progressive_jpg.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_WithChromeHeadless() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-headless.lineup.json", "--replace-in-url###CWD###=" + CWD, " --step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-headless.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_WithFirefox() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url###CWD###=" + CWD, " --step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_ReportFormat2() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_reportv2.lineup.json", "--replace-in-url###CWD###=" + CWD, " --step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_reportv2.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_LegacyReportFormat() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_legacy.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_legacy.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        @SuppressWarnings("unchecked") final ArrayList<Map> report = gson.fromJson(jsonReportText, ArrayList.class);
        assertThat(report.get(0).get("difference"), CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatDoesntChange_DefaultReportFormat() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final String jsonReportText = getTextFileContentAsString(reportJson);
        final Report report = gson.fromJson(jsonReportText, Report.class);
        assertThat(report.summary.differenceSum, CoreMatchers.is(0.0d));

        final String htmlReportText = getTextFileContentAsString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));

        assertThat(systemOutCaptor.toString(), containsString("Sum of screenshot differences for file://###CWD###/src/test/resources/acceptance/webpage/: 0.0 (0 %)"));
        assertThat(systemOutCaptor.toString(), containsString("Sum of overall screenshot differences: 0.0 (0 %)"));
        assertThat(systemOutCaptor.toString(), CoreMatchers.not(containsString("WARNING: 'wait-for-fonts-time' is ignored because PhantomJS doesn't support this feature.")));
    }

    @Test
    public void shouldRunJLineupWithTestPageThatHasAVeryLongPath() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_long_url.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_long_url.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});
    }

    @Test
    public void shouldNotCrashPhantomjsFontsNotLoaded() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_phantom_fonts.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_phantom_fonts.lineup.json", "--replace-in-url###CWD###=" + CWD, "--step", "after"});
        assertThat(systemOutCaptor.toString(), containsString("WARNING: 'wait-for-fonts-time' is ignored because PhantomJS doesn't support this feature."));
    }

    @Test
    public void shouldPrintConfig() {
        exit.checkAssertionAfterwards(() -> assertThat(systemOutCaptor.toString(), containsString("http://www.example.com")));
        exit.expectSystemExitWithStatus(0);
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--print-config"});
    }

    @Test
    public void shouldPrintExampleConfig() {
        exit.checkAssertionAfterwards(() -> assertThat(systemOutCaptor.toString(), containsString(JobConfig.prettyPrint(JobConfig.exampleConfig()))));
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
