package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static de.otto.jlineup.browser.Browser.Type.*;
import static de.otto.jlineup.config.JobConfig.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JLineupRunnerFactoryTest {

    private JLineupWebProperties jLineupWebProperties;
    private JLineupRunnerFactory jLineupRunnerFactory;

    @Before
    public void setup() {
        jLineupWebProperties = new JLineupWebProperties();
        jLineupWebProperties.setInstalledBrowsers(Arrays.asList(CHROME, CHROME_HEADLESS));
        jLineupWebProperties.setMaxThreadsPerJob(10);
        jLineupRunnerFactory = new JLineupRunnerFactory(jLineupWebProperties);
    }

    @Test
    public void shouldSanitizeJobConfig() throws BrowserNotInstalledException {

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

    @Test(expected = BrowserNotInstalledException.class)
    public void shouldThrowBrowserNotInstalledException() throws BrowserNotInstalledException {
        // Given
        JobConfig jobConfig = copyOfBuilder(exampleConfig())
                .withBrowser(FIREFOX)
                .build();

        //When
        jLineupRunnerFactory.sanitizeJobConfig(jobConfig);
    }

    @Test
    public void shouldUseMaxThreadsPerJobIfNoThreadsAreConfigured() throws BrowserNotInstalledException {
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
    public void shouldUseDefinedNumberOfThreadsIfConfiguredAndBelowMax() throws BrowserNotInstalledException {
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
    public void shouldReduceNumberOfThreadsToMax() throws BrowserNotInstalledException {
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