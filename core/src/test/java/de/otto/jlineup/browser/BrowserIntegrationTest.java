package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.config.*;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.file.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FakeWebServerController.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class BrowserIntegrationTest {

    @LocalServerPort
    private int port;

    private Path tempDirectory;

    @Before
    public void setup() throws IOException {
        tempDirectory = Files.createTempDirectory("jlineup-browser-integration-test");
        Utils.setLogLevelToDebug();
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempDirectory);
    }


    @Test
    public void shouldNotThrowAnExceptionInChromeIfItIsConfiguredToNotCheckForErrorsOnA403() throws ValidationError {
        //given
        JobConfig jobConfig = localTestConfig("403", Browser.Type.CHROME_HEADLESS, false);
        //when
        runJLineup(jobConfig, Step.before);
        runJLineup(jobConfig, Step.after);
        //then
    }

    @Test
    public void shouldThrowAnExceptionInChromeIfItIsConfiguredToCheckForErrorsOnA403() throws ValidationError {
        //given
        JobConfig jobConfig = localTestConfig("403", Browser.Type.CHROME_HEADLESS, true);
        Exception thrown = null;
        //when
        try {
            runJLineup(jobConfig, Step.before);
        } catch (Exception e) {
            thrown = e;
        }
        //then
        assertThat(thrown, notNullValue());
        assertThat(thrown.getMessage(), containsString("Exception in Browser thread"));
    }

    @Test
    public void shouldNotThrowAnExceptionInChromeIfItIsConfiguredToNotCheckForErrorsOnA500() throws ValidationError {
        //given
        JobConfig jobConfig = localTestConfig("500", Browser.Type.CHROME_HEADLESS, false);
        //when
        runJLineup(jobConfig, Step.before);
        runJLineup(jobConfig, Step.after);
        //then
    }

    @Test
    public void shouldThrowAnExceptionInChromeIfItIsConfiguredToCheckForErrorsOnA500() throws ValidationError {
        //given
        JobConfig jobConfig = localTestConfig("500", Browser.Type.CHROME_HEADLESS, true);
        Exception thrown = null;
        //when
        try {
            runJLineup(jobConfig, Step.before);
        } catch (Exception e) {
            thrown = e;
        }
        //then
        assertThat(thrown, notNullValue());
        assertThat(thrown.getMessage(), containsString("Exception in Browser thread"));
    }

    @Test
    public void shouldNotThrowAnExceptionInFirefoxIfItIsConfiguredToNotCheckForErrorsOnA500() throws ValidationError {
        //given
        JobConfig jobConfig = localTestConfig("500", Browser.Type.FIREFOX_HEADLESS, false);
        //when
        runJLineup(jobConfig, Step.before);
        runJLineup(jobConfig, Step.after);
        //then
    }

    @Test
    public void willNotThrowAnExceptionInFirefoxBecauseFirefoxWithSeleniumCantHandleResponseCodes() throws ValidationError {
        //given
        JobConfig jobConfig = localTestConfig("500", Browser.Type.FIREFOX_HEADLESS, true);
        //when
        runJLineup(jobConfig, Step.before);
        runJLineup(jobConfig, Step.after);
        //then
        //no Exception
    }

    @Test
    public void shouldNotAppendSlashToDomain() throws ValidationError {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder().withCookie(new Cookie("CookieName", "CookieValue")).build();
        JobConfig jobConfig = localTestConfig("params?param1=1&param2=2", Browser.Type.CHROME_HEADLESS, true, urlConfig);
        runJLineup(jobConfig, Step.before);
    }

    @Test
    public void shouldSetCookieOnCorrectPath() throws ValidationError {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withCookie(new Cookie("CookieName", "CookieValue"))
                .withPaths(ImmutableList.of("/")).build();
        JobConfig jobConfig = localTestConfig("somerootpath/somevalidsubpath", Browser.Type.CHROME_HEADLESS, true, urlConfig);
        runJLineup(jobConfig, Step.before);
    }

    @Test
    public void shouldCheckHttpStatusCodeError() {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withHttpCheck(new HttpCheckConfig(true))
                .withPaths(ImmutableList.of("/")).build();
        JobConfig jobConfig = localTestConfig("500", Browser.Type.CHROME_HEADLESS, false, urlConfig);
        try {
            runJLineup(jobConfig, Step.before);
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getCause().getCause().getMessage(), containsString("Accessibility check"));
            assertThat(e.getCause().getCause().getCause().getMessage(), containsString("returned status code 500"));
        }
    }

    @Test
    public void shouldCheckCustomHttpStatusCodes() throws ValidationError {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withHttpCheck(new HttpCheckConfig(true, ImmutableList.of(304)))
                .withPaths(ImmutableList.of("/")).build();
        JobConfig jobConfig = localTestConfig("200", Browser.Type.CHROME_HEADLESS, false, urlConfig);
        try {
            runJLineup(jobConfig, Step.before);
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getCause().getCause().getMessage(), containsString("Accessibility check"));
            assertThat(e.getCause().getCause().getCause().getMessage(), containsString("returned status code 200"));
        }
    }

    @Test
    public void shouldDetectErrorSignal() throws ValidationError {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withHttpCheck(HttpCheckConfig.httpCheckConfigBuilder().withEnabled(true).withErrorSignals(singletonList("Hallo!")).build())
                .withPaths(ImmutableList.of("/")).build();
        JobConfig jobConfig = localTestConfig("somerootpath/somevalidsubpath", Browser.Type.CHROME_HEADLESS, true, urlConfig);
        try {
            runJLineup(jobConfig, Step.before);
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getCause().getCause().getMessage(), containsString("Accessibility check"));
            assertThat(e.getCause().getCause().getCause().getMessage(), containsString("returned error signal"));
        }
    }


    @Test
    public void shouldNotCheckHttpStatusCodeErrorIfNotConfigured() throws ValidationError {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withHttpCheck(new HttpCheckConfig(false))
                .withPaths(ImmutableList.of("/")).build();
        JobConfig jobConfig = localTestConfig("500", Browser.Type.CHROME_HEADLESS, false, urlConfig);
        runJLineup(jobConfig, Step.before);
        //no exception
    }

    @Test
    public void shouldSetAllCookies() throws ValidationError, IOException {
        UrlConfig urlConfig = UrlConfig.urlConfigBuilder()
                .withHttpCheck(new HttpCheckConfig(true, ImmutableList.of(HttpStatus.SERVICE_UNAVAILABLE.value(), HttpStatus.UNAUTHORIZED.value())))
                .withAlternatingCookies(ImmutableList.of(
                        ImmutableList.of(new Cookie("alternating", "SERVICE_UNAVAILABLE")),
                        ImmutableList.of(new Cookie("alternating", "UNAUTHORIZED"))
                ))
                .withPaths(ImmutableList.of("/")).build();
        JobConfig jobConfig = localTestConfig("cookies", Browser.Type.CHROME_HEADLESS, false, urlConfig);
        runJLineup(jobConfig, Step.before);
        RunStepConfig runStepConfig = runJLineup(jobConfig, Step.after);
        //no exception
    }

    private RunStepConfig runJLineup(JobConfig jobConfig, Step step) throws ValidationError {
        RunStepConfig runStepConfig = RunStepConfig.jLineupRunConfigurationBuilder()
                .withWorkingDirectory(tempDirectory.toAbsolutePath().toString())
                .withScreenshotsDirectory("screenshots")
                .withReportDirectory("report")
                .withStep(step).build();
        new JLineupRunner(jobConfig, runStepConfig).run();
        return runStepConfig;
    }

    private JobConfig localTestConfig(String endpoint, Browser.Type browser, boolean checkForErrors) {
        return localTestConfig(endpoint, browser, checkForErrors, UrlConfig.urlConfigBuilder().build());
    }

    private JobConfig localTestConfig(String endpoint, Browser.Type browser, boolean checkForErrors, UrlConfig urlConfig) {
        return JobConfig.jobConfigBuilder().withCheckForErrorsInLog(checkForErrors).withUrls(ImmutableMap.of("http://localhost:" + port + "/" + endpoint, urlConfig)).withBrowser(browser).build();
    }

}
