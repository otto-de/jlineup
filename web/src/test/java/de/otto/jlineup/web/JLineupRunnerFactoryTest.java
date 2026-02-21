package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.browser.Browser.Type.*;
import static de.otto.jlineup.config.JobConfig.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JLineupRunnerFactoryTest {

    private JLineupWebProperties jLineupWebProperties;
    private JLineupRunnerFactory jLineupRunnerFactory;

    @BeforeEach
    void setup() {
        jLineupWebProperties = new JLineupWebProperties();
        jLineupWebProperties.setInstalledBrowsers(Arrays.asList(CHROME, CHROME_HEADLESS));
        jLineupWebProperties.setMaxThreadsPerJob(10);
        jLineupWebProperties.setAllowedUrlPrefixes(List.of("https://www.example.com"));
        jLineupRunnerFactory = new JLineupRunnerFactory(jLineupWebProperties);
    }

    @Test
    void shouldSanitizeJobConfigForInstalledBrowsers() throws BrowserNotInstalledException {

        //Given
        JobConfig evilJobConfig = copyOfBuilder(exampleConfig())
                .withBrowser(CHROME_HEADLESS)
                .withThreads(Integer.MAX_VALUE)
                .withDebug(true)
                .withLogToFile(true)
                .withReportFormat(1)
                .build();

        //When
        JobConfig sanitizedJobConfig = jLineupRunnerFactory.sanitizeJobConfig(evilJobConfig);

        //Then
        assertThat(sanitizedJobConfig.threads, is(jLineupWebProperties.getMaxThreadsPerJob()));
        assertThat(sanitizedJobConfig.debug, is(false));
        assertThat(sanitizedJobConfig.logToFile, is(false));
        assertThat(sanitizedJobConfig.reportFormat, is(DEFAULT_REPORT_FORMAT));
    }

    @Test
    void shouldThrowBrowserNotInstalledException() {
        JobConfig jobConfig = copyOfBuilder(exampleConfig()).withBrowser(FIREFOX).build();
        assertThrows(BrowserNotInstalledException.class, () -> jLineupRunnerFactory.sanitizeJobConfig(jobConfig));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionBecauseURLIsNotAllowed() {
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withUrls(Map.of("Some Url", UrlConfig.copyOfBuilder(exampleConfig().urls.values().stream().findFirst().get()).withUrl("https://www.notallowed.org").build()))
                .build();
        assertThrows(IllegalArgumentException.class, () -> jLineupRunnerFactory.sanitizeJobConfig(jobConfig));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionBecauseJavaScriptContainsHttpsURLThatIsNotInTheAllowedPrefixes() {
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withUrls(Map.of("Some Url", UrlConfig.copyOfBuilder(exampleConfig().urls.values().stream().findFirst().get()).withJavaScript("fetch('https%25253A%25252F%25252Fwww.example.com');fetch('https%25253A%25252F%25252Fwww.notallowed.com');").build()))
                .build();
        assertThrows(IllegalArgumentException.class, () -> jLineupRunnerFactory.sanitizeJobConfig(jobConfig));
    }

    @Test
    void shouldNotThrowIllegalArgumentExceptionBecauseJavaScriptContainsAllowedURLAfterHttps() throws IllegalArgumentException, BrowserNotInstalledException {
        // Given
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withUrls(Map.of("Some Url", UrlConfig.copyOfBuilder(exampleConfig().urls.values().stream().findFirst().get()).withJavaScript("fetch('https%25253A%25252F%25252Fwww.example.com')").build()))
                .build();

        //When
        jLineupRunnerFactory.sanitizeJobConfig(jobConfig);
    }


    @Test
    void shouldNotThrowIllegalArgumentExceptionWhenURLPrefixesAreNotSet() throws BrowserNotInstalledException {
        // Given
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withUrls(Map.of("Some Url", UrlConfig.copyOfBuilder(exampleConfig().urls.values().stream().findFirst().get()).withUrl("https://www.notallowed.org").build()))
                .build();

        jLineupWebProperties.setAllowedUrlPrefixes(List.of());

        //When
        jLineupRunnerFactory.sanitizeJobConfig(jobConfig);
    }

    @Test
    void shouldUseMaxThreadsPerJobIfNoThreadsAreConfigured() throws BrowserNotInstalledException {
        //Given
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withThreads(0)
                .build();
        //When
        JobConfig sanitizedConfig = jLineupRunnerFactory.sanitizeJobConfig(jobConfig);

        //Then
        assertThat(sanitizedConfig.threads, is(jLineupWebProperties.getMaxThreadsPerJob()));
    }

    @Test
    void shouldUseDefinedNumberOfThreadsIfConfiguredAndBelowMax() throws BrowserNotInstalledException {
        //Given
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withThreads(2)
                .build();
        //When
        JobConfig sanitizedConfig = jLineupRunnerFactory.sanitizeJobConfig(jobConfig);

        //Then
        assertThat(sanitizedConfig.threads, is(2));
    }

    @Test
    void shouldReduceNumberOfThreadsToMax() throws BrowserNotInstalledException {
        //Given
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withThreads(200)
                .build();
        //When
        JobConfig sanitizedConfig = jLineupRunnerFactory.sanitizeJobConfig(jobConfig);

        //Then
        assertThat(sanitizedConfig.threads, is(jLineupWebProperties.getMaxThreadsPerJob()));
    }
}