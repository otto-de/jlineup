package de.otto.jlineup.cli.acceptance;

import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.cli.Main;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileTracker;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.report.ContextReport;
import de.otto.jlineup.report.ImageContentHint;
import de.otto.jlineup.report.ImageUrlChanges;
import de.otto.jlineup.report.Report;
import de.otto.jlineup.report.ScreenshotComparisonResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
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
    @EnabledIfEnvironmentVariable(named = "JLINEUP_TEST_ALL_BROWSERS", matches = "true")
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
        assertThat(combinedOutput(), containsString("timeout"));
    }

    @Test
    void shouldOpenTestPage() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--url", "file://" + CWD + "/src/test/resources/acceptance/webpage/test.html"});
    }


    @Test
    @EnabledIfEnvironmentVariable(named = "JLINEUP_TEST_ALL_BROWSERS", matches = "true")
    void shouldRunJLineupWithTestPageThatDoesntChange_WithChrome() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});

        final Path reportBeforeHtml = Paths.get(tempDirectory.toString(), "report", "report_before.html");
        assertThat("Report Reference HTML exists", Files.exists(reportBeforeHtml));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));

        final String htmlReportText = Files.readString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithSomeDevices_WithChromeHeadless() throws Exception {

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
    void shouldPassCommandLineParametersToChromeHeadless() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-headless.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before", "--chrome-parameter", "--user-agent=\"fakeuseragent\""});
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

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));

        final String htmlReportText = Files.readString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    void shouldRunJLineupWithWaitForSelectors() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-wait_for_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));
    }

    @Test
    void shouldRemoveNodesAndShouldCalculateStableContextHashAlthoughEffectiveUrlChangesThroughReplaceInUrlFeature() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-remove_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=1", "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome-remove_selectors.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=2", "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));
    }

    @Test
    void shouldDetectCdnCompressionArtifactsAsDifferenceByDefault() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_compression_artifacts_not_ignored.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=1", "--step", "before"});
        int status = catchSystemExit(() ->
                Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_compression_artifacts_not_ignored.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=2", "--step", "after"}));
        assertEquals(1, status);

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum() > 0, is(true));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifacts() throws Exception {
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_compression_artifacts.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=1", "--step", "before"});
        Main.main(new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_chrome_compression_artifacts.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--replace-in-url=###NUM###=2", "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));
        assertThat(report.summary().acceptedDifferentPixels() > 0, is(true));
    }

    /*
     * ignore-image-content: the page lives in a temp directory, so the image files can be swapped between the
     * before and the after step while their URLs stay the same.
     */

    private static final String IMAGE_CONTENT_CONFIG = "src/test/resources/acceptance/acceptance_chrome_image_content.lineup.json";
    private static final String IMAGE_CONTENT_NOT_IGNORED_CONFIG = "src/test/resources/acceptance/acceptance_chrome_image_content_not_ignored.lineup.json";

    private void prepareImageContentPage(Path pageDirectory, String page, String promoImage, String teaserImage, String promoV2Image) throws IOException {
        Files.copy(Paths.get("src/test/resources/acceptance/image_content", page), pageDirectory.resolve("page.html"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get("src/test/resources/acceptance/webpage", promoImage), pageDirectory.resolve("promo.webp"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get("src/test/resources/acceptance/webpage", teaserImage), pageDirectory.resolve("teaser.avif"), StandardCopyOption.REPLACE_EXISTING);
        if (promoV2Image != null) {
            Files.copy(Paths.get("src/test/resources/acceptance/webpage", promoV2Image), pageDirectory.resolve("promo_v2.webp"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String[] imageContentArguments(String config, Path pageDirectory, String step) {
        return new String[]{"--chrome-parameter", "--force-device-scale-factor=1", "--working-dir", tempDirectory.toString(), "--config", config, "--replace-in-url=###DIR###=" + pageDirectory, "--step", step};
    }

    private Report readReport() throws IOException {
        return JacksonWrapper.jsonMapper().readValue(Paths.get(tempDirectory.toString(), "report", "report.json").toFile(), Report.class);
    }

    //The image content config for another browser engine
    private String imageContentConfigFor(String browser) throws IOException {
        Path config = Files.createTempFile(tempDirectory, "image_content_" + browser, ".lineup.json");
        Files.writeString(config, Files.readString(Paths.get(IMAGE_CONTENT_CONFIG)).replace("\"chrome-headless\"", "\"" + browser + "\""));
        return config.toString();
    }

    private void shouldAcceptChangedImageContentAndDetectChangedLetter(String browser) throws Exception {
        String config = imageContentConfigFor(browser);
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(config, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_2.webp", "cdn_kamp_20_2.avif", null);
            Main.main(imageContentArguments(config, pageDirectory, "after"));
            final Report report = readReport();
            assertThat(report.summary().differenceSum(), is(0.0d));
            assertThat(allResults(report).stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.IMAGE_CONTENT_ACCEPTED), is(true));

            prepareImageContentPage(pageDirectory, "page_letter_changed.html", "cdn_app_promo_2.webp", "cdn_kamp_20_2.avif", null);
            int status = catchSystemExit(() -> Main.main(imageContentArguments(config, pageDirectory, "after")));
            assertEquals(1, status);
            assertThat(allResults(readReport()).stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.LAYOUT_CHANGED), is(true));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JLINEUP_TEST_ALL_BROWSERS", matches = "true")
    void shouldIgnoreImageContentWithFirefox() throws Exception {
        shouldAcceptChangedImageContentAndDetectChangedLetter("firefox-headless");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JLINEUP_TEST_ALL_BROWSERS", matches = "true")
    @EnabledOnOs(OS.LINUX)
    void shouldIgnoreImageContentWithWebkit() throws Exception {
        shouldAcceptChangedImageContentAndDetectChangedLetter("webkit-headless");
    }

    private List<ScreenshotComparisonResult> allResults(Report report) {
        return report.urlReports().stream().flatMap(u -> u.contextReports().stream()).flatMap(c -> c.results().stream()).toList();
    }

    private List<ContextReport> allContextReports(Report report) {
        return report.urlReports().stream().flatMap(u -> u.contextReports().stream()).toList();
    }

    @Test
    void shouldAcceptChangedImageContentWithIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_2.webp", "cdn_kamp_20_2.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after"));

            final Report report = readReport();
            assertThat(report.summary().differenceSum(), is(0.0d));
            assertThat(report.summary().acceptedDifferentPixels() > 0, is(true));
            List<ScreenshotComparisonResult> results = allResults(report);
            assertThat(results.stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.IMAGE_CONTENT_ACCEPTED), is(true));
            assertThat(results.stream().allMatch(ScreenshotComparisonResult::hasImagelessScreenshots), is(true));
            assertThat(allContextReports(report).stream().allMatch(c -> c.imageUrlChanges() == null), is(true));
            for (ScreenshotComparisonResult result : results) {
                assertThat(Files.exists(Paths.get(tempDirectory.toString(), "report", result.imagelessScreenshotBeforeFileName())), is(true));
                assertThat(Files.exists(Paths.get(tempDirectory.toString(), "report", result.imagelessScreenshotAfterFileName())), is(true));
            }
            final String htmlReport = Files.readString(Paths.get(tempDirectory.toString(), "report", "report.html"));
            assertThat(htmlReport, containsString("Without images"));
            final String htmlReportBefore = Files.readString(Paths.get(tempDirectory.toString(), "report", "report_before.html"));
            assertThat(htmlReportBefore, containsString("class=\"image-mode-toggle global-image-mode\""));
            assertThat(htmlReportBefore, containsString("_before_noimages.png"));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldDetectChangedImageContentWithoutIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_NOT_IGNORED_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_2.webp", "cdn_kamp_20_2.avif", null);
            int status = catchSystemExit(() -> Main.main(imageContentArguments(IMAGE_CONTENT_NOT_IGNORED_CONFIG, pageDirectory, "after")));
            assertEquals(1, status);

            final Report report = readReport();
            assertThat(report.summary().differenceSum() > 0, is(true));
            assertThat(allResults(report).stream().noneMatch(ScreenshotComparisonResult::hasImagelessScreenshots), is(true));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldDetectChangedTextOverImageDespiteIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page_text_changed.html", "cdn_app_promo_2.webp", "cdn_kamp_20_2.avif", null);
            int status = catchSystemExit(() -> Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after")));
            assertEquals(1, status);

            final Report report = readReport();
            assertThat(report.summary().differenceSum() > 0, is(true));
            assertThat(allResults(report).stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.LAYOUT_CHANGED && r.imagelessDifferenceImageFileName() != null), is(true));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldDetectSingleChangedLetterOverImageDespiteIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page_letter_changed.html", "cdn_app_promo_2.webp", "cdn_kamp_20_2.avif", null);
            int status = catchSystemExit(() -> Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after")));
            assertEquals(1, status);

            assertThat(allResults(readReport()).stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.LAYOUT_CHANGED), is(true));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldDetectChangedGradientDespiteIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page_gradient_changed.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            int status = catchSystemExit(() -> Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after")));
            assertEquals(1, status);

            final Report report = readReport();
            assertThat(allResults(report).stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.LAYOUT_CHANGED), is(true));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldDetectMovedImageDespiteIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page_moved.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            int status = catchSystemExit(() -> Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after")));
            assertEquals(1, status);

            assertThat(readReport().summary().differenceSum() > 0, is(true));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldDetectChangedImageWithOtherUrlDespiteIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            prepareImageContentPage(pageDirectory, "page_other_url.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", "cdn_app_promo_2.webp");
            int status = catchSystemExit(() -> Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after")));
            assertEquals(1, status);

            final Report report = readReport();
            assertThat(report.summary().differenceSum() > 0, is(true));
            assertThat(allResults(report).stream().anyMatch(r -> r.imageContentHint() == ImageContentHint.IMAGE_URLS_CHANGED), is(true));
            ImageUrlChanges imageUrlChanges = allContextReports(report).get(0).imageUrlChanges();
            assertThat(imageUrlChanges.added().get(0), endsWith("/promo_v2.webp"));
            assertThat(imageUrlChanges.removed().get(0), endsWith("/promo.webp"));
        } finally {
            deleteDir(pageDirectory);
        }
    }

    @Test
    void shouldPassWithNoteIfOnlyImageUrlsChangedWithIgnoreImageContent() throws Exception {
        Path pageDirectory = Files.createTempDirectory("jlineup-image-content-");
        try {
            prepareImageContentPage(pageDirectory, "page.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", null);
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "before"));
            //same file under a new URL, i.e. after a refactoring
            prepareImageContentPage(pageDirectory, "page_other_url.html", "cdn_app_promo_1.webp", "cdn_kamp_20_1.avif", "cdn_app_promo_1.webp");
            Main.main(imageContentArguments(IMAGE_CONTENT_CONFIG, pageDirectory, "after"));

            final Report report = readReport();
            assertThat(report.summary().differenceSum(), is(0.0d));
            assertThat(allContextReports(report).get(0).imageUrlChanges().added().get(0), endsWith("/promo_v2.webp"));
            final String htmlReport = Files.readString(Paths.get(tempDirectory.toString(), "report", "report.html"));
            assertThat(htmlReport, containsString("image URLs changed"));
        } finally {
            deleteDir(pageDirectory);
        }
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
    @EnabledIfEnvironmentVariable(named = "JLINEUP_TEST_ALL_BROWSERS", matches = "true")
    void shouldRunJLineupWithTestPageThatDoesntChange_WithFirefox() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});

        final Path reportBeforeHtml = Paths.get(tempDirectory.toString(), "report", "report_before.html");
        assertThat("Report Reference HTML exists", Files.exists(reportBeforeHtml));

        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));

        final String htmlReportText = Files.readString(reportHtml);
        assertThat(htmlReportText, containsString("<a href=\"screenshots/"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JLINEUP_TEST_ALL_BROWSERS", matches = "true")
    void shouldRunJLineupWithTestPageThatDoesntChange_WithDifferentConfigsButSamePages_FixesContextHashBug() throws Exception {
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox_variant1.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "before"});
        Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_firefox_variant2.lineup.json", "--replace-in-url=###CWD###=" + CWD, "--step", "after"});

        final Path reportJson = Paths.get(tempDirectory.toString(), "report", "report.json");
        assertThat("Report JSON exists", Files.exists(reportJson));
        final Path reportHtml = Paths.get(tempDirectory.toString(), "report", "report.html");
        assertThat("Report HTML exists", Files.exists(reportHtml));

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));

        final String htmlReportText = Files.readString(reportHtml);
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

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));

        final String htmlReportText = Files.readString(reportHtml);
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

        final Report report = JacksonWrapper.jsonMapper().readValue(reportJson.toFile(), Report.class);
        assertThat(report.summary().differenceSum(), is(0.0d));

        final String htmlReportText = Files.readString(reportHtml);
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
        Set<Map.Entry<String, ScreenshotContextFileTracker>> entries = fileTracker.getContexts().entrySet();
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
    void shouldSetReportLabelsFromCommandLine() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--url", "https://www.example.com", "--print-config",
                    "--before-label", "Reference (local)", "--after-label", "Current (dev)"}));
        assertEquals(0, status);
        assertThat(systemOutCaptor.toString(), containsString("before-label: Reference (local)"));
        assertThat(systemOutCaptor.toString(), containsString("after-label: Current (dev)"));
    }

    @Test
    void shouldOverrideReportLabelsOfConfigFromCommandLine() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--config", "src/test/resources/acceptance/acceptance_labels.lineup.yaml", "--print-config",
                    "--after-label", "Current (dev)"}));
        assertEquals(0, status);
        assertThat(systemOutCaptor.toString(), containsString("before-label: Reference from config"));
        assertThat(systemOutCaptor.toString(), containsString("after-label: Current (dev)"));
        assertThat(systemOutCaptor.toString(), not(containsString("After from config")));
    }

    @Test
    void shouldPrintExampleConfig() throws Exception {
        int status = catchSystemExit(() ->
            Main.main(new String[]{"--working-dir", tempDirectory.toString(), "--print-example"}));
        assertEquals(0, status);
        assertThat(systemOutCaptor.toString(), containsString(JobConfig.prettyPrintWithAllFields(JobConfig.exampleConfig(), JacksonWrapper.ConfigFormat.YAML)));
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
