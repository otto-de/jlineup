package de.otto.jlineup.report;

import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorTest {

    @Mock
    private FileService fileServiceMock;

    @Test
    void shouldAcceptFlakyContextWhenEnoughSiblingsPass() {
        // Setup: 3 contexts for the same URL+path+cookies, different devices
        // Context 1: 800x600 - FAILS (difference > 0)
        // Context 2: 1024x768 - PASSES (difference = 0)
        // Context 3: 1200x1000 - PASSES (difference = 0)
        // With flakyTolerance=2, context 1 should be flaky-accepted

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(800, 600), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1024, 768), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx3 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1200, 1000), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");

        // Verify they share the same sibling key but different context hashes
        assertThat(ctx1.flakySiblingKey(), is(ctx2.flakySiblingKey()));
        assertThat(ctx1.flakySiblingKey(), is(ctx3.flakySiblingKey()));
        assertThat(ctx1.contextHash().equals(ctx2.contextHash()), is(false));

        ContextReport cr1 = new ContextReport(ctx1.contextHash(), ctx1,
                new Summary(true, 0.05, 0.05, 0),
                List.of(new ScreenshotComparisonResult(ctx1.contextHash(), "https://example.com/page", deviceConfig(800, 600), 0, 0.05, 1.0, "before1.png", "after1.png", "diff1.png", 0)));

        ContextReport cr2 = new ContextReport(ctx2.contextHash(), ctx2,
                new Summary(false, 0, 0, 0),
                List.of(new ScreenshotComparisonResult(ctx2.contextHash(), "https://example.com/page", deviceConfig(1024, 768), 0, 0, 0, "before2.png", "after2.png", null, 0)));

        ContextReport cr3 = new ContextReport(ctx3.contextHash(), ctx3,
                new Summary(false, 0, 0, 0),
                List.of(new ScreenshotComparisonResult(ctx3.contextHash(), "https://example.com/page", deviceConfig(1200, 1000), 0, 0, 0, "before3.png", "after3.png", null, 0)));

        ArrayList<ContextReport> contextReports = new ArrayList<>(List.of(cr1, cr2, cr3));

        // Apply flaky tolerance of 2
        ArrayList<ContextReport> result = ReportGenerator.applyFlakyTolerance(contextReports, 2);

        assertThat(result.size(), is(3));
        // The failing context should be flaky-accepted
        ContextReport failingResult = result.stream().filter(cr -> cr.contextHash().equals(ctx1.contextHash())).findFirst().orElseThrow();
        assertThat(failingResult.flakyAccepted(), is(true));
        assertThat(failingResult.isSuccess(), is(true));

        // Passing contexts should remain unchanged
        ContextReport passingResult2 = result.stream().filter(cr -> cr.contextHash().equals(ctx2.contextHash())).findFirst().orElseThrow();
        assertThat(passingResult2.flakyAccepted(), is(false));

        ContextReport passingResult3 = result.stream().filter(cr -> cr.contextHash().equals(ctx3.contextHash())).findFirst().orElseThrow();
        assertThat(passingResult3.flakyAccepted(), is(false));
    }

    @Test
    void shouldNotAcceptFlakyContextWhenNotEnoughSiblingsPass() {
        // Setup: 3 contexts, 2 fail, 1 passes
        // With flakyTolerance=2, failing contexts should NOT be accepted (only 1 passing sibling)

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(800, 600), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1024, 768), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx3 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1200, 1000), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");

        ContextReport cr1 = new ContextReport(ctx1.contextHash(), ctx1,
                new Summary(true, 0.05, 0.05, 0), Collections.emptyList());

        ContextReport cr2 = new ContextReport(ctx2.contextHash(), ctx2,
                new Summary(true, 0.03, 0.03, 0), Collections.emptyList());

        ContextReport cr3 = new ContextReport(ctx3.contextHash(), ctx3,
                new Summary(false, 0, 0, 0), Collections.emptyList());

        ArrayList<ContextReport> contextReports = new ArrayList<>(List.of(cr1, cr2, cr3));

        ArrayList<ContextReport> result = ReportGenerator.applyFlakyTolerance(contextReports, 2);

        // Neither failing context should be accepted (only 1 passing sibling each)
        ContextReport failingResult1 = result.stream().filter(cr -> cr.contextHash().equals(ctx1.contextHash())).findFirst().orElseThrow();
        assertThat(failingResult1.flakyAccepted(), is(false));

        ContextReport failingResult2 = result.stream().filter(cr -> cr.contextHash().equals(ctx2.contextHash())).findFirst().orElseThrow();
        assertThat(failingResult2.flakyAccepted(), is(false));
    }

    @Test
    void shouldNotGroupContextsWithDifferentCookies() {
        // Two contexts with same path but different context-giving cookies should NOT be siblings

        Cookie cookie1 = new Cookie("app", "true", "example.com", "/", null, false, null, true);
        Cookie cookie2 = new Cookie("app", "false", "example.com", "/", null, false, null, true);

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(800, 600), BrowserStep.before, urlConfig, List.of(cookie1), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1024, 768), BrowserStep.before, urlConfig, List.of(cookie2), "https://example.com");

        // They should have different sibling keys
        assertThat(ctx1.flakySiblingKey().equals(ctx2.flakySiblingKey()), is(false));

        ContextReport cr1 = new ContextReport(ctx1.contextHash(), ctx1,
                new Summary(true, 0.05, 0.05, 0), Collections.emptyList());

        ContextReport cr2 = new ContextReport(ctx2.contextHash(), ctx2,
                new Summary(false, 0, 0, 0), Collections.emptyList());

        ArrayList<ContextReport> contextReports = new ArrayList<>(List.of(cr1, cr2));

        ArrayList<ContextReport> result = ReportGenerator.applyFlakyTolerance(contextReports, 1);

        // ctx1 should NOT be flaky-accepted (ctx2 has different cookies)
        ContextReport failingResult = result.stream().filter(cr -> cr.contextHash().equals(ctx1.contextHash())).findFirst().orElseThrow();
        assertThat(failingResult.flakyAccepted(), is(false));
    }

    @Test
    void shouldNotGroupContextsWithDifferentPaths() {
        // Two contexts with same cookies but different paths should NOT be siblings

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page1", deviceConfig(800, 600), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page2", deviceConfig(1024, 768), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");

        // They should have different sibling keys
        assertThat(ctx1.flakySiblingKey().equals(ctx2.flakySiblingKey()), is(false));
    }

    @Test
    void shouldGroupContextsWithSameContextGivingCookiesIgnoringNonContextGiving() {
        // Non-context-giving cookies should be ignored for sibling grouping

        Cookie contextGiving = new Cookie("app", "true", "example.com", "/", null, false, null, true);
        Cookie nonContextGiving = new Cookie("session", "abc123", "example.com", "/", null, false, null, false);
        Cookie differentNonContextGiving = new Cookie("session", "xyz789", "example.com", "/", null, false, null, false);

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(800, 600), BrowserStep.before, urlConfig, List.of(contextGiving, nonContextGiving), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1024, 768), BrowserStep.before, urlConfig, List.of(contextGiving, differentNonContextGiving), "https://example.com");

        // They should have the same sibling key (non-context-giving cookies are ignored)
        assertThat(ctx1.flakySiblingKey(), is(ctx2.flakySiblingKey()));
    }

    @Test
    void shouldAcceptWithFlakyToleranceOfOne() {
        // With flakyTolerance=1, even a single passing sibling should be enough

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(800, 600), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1024, 768), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");

        ContextReport cr1 = new ContextReport(ctx1.contextHash(), ctx1,
                new Summary(true, 0.05, 0.05, 0), Collections.emptyList());

        ContextReport cr2 = new ContextReport(ctx2.contextHash(), ctx2,
                new Summary(false, 0, 0, 0), Collections.emptyList());

        ArrayList<ContextReport> contextReports = new ArrayList<>(List.of(cr1, cr2));

        ArrayList<ContextReport> result = ReportGenerator.applyFlakyTolerance(contextReports, 1);

        ContextReport failingResult = result.stream().filter(cr -> cr.contextHash().equals(ctx1.contextHash())).findFirst().orElseThrow();
        assertThat(failingResult.flakyAccepted(), is(true));
    }

    @Test
    void shouldNotAffectAlreadyPassingContexts() {
        // Passing contexts should never become flaky-accepted

        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().build();

        ScreenshotContext ctx1 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(800, 600), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");
        ScreenshotContext ctx2 = ScreenshotContext.of("https://example.com", "/page", deviceConfig(1024, 768), BrowserStep.before, urlConfig, Collections.emptyList(), "https://example.com");

        ContextReport cr1 = new ContextReport(ctx1.contextHash(), ctx1,
                new Summary(false, 0, 0, 0), Collections.emptyList());

        ContextReport cr2 = new ContextReport(ctx2.contextHash(), ctx2,
                new Summary(false, 0, 0, 0), Collections.emptyList());

        ArrayList<ContextReport> contextReports = new ArrayList<>(List.of(cr1, cr2));

        ArrayList<ContextReport> result = ReportGenerator.applyFlakyTolerance(contextReports, 1);

        assertThat(result.get(0).flakyAccepted(), is(false));
        assertThat(result.get(1).flakyAccepted(), is(false));
    }
}
