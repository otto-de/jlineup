package de.otto.jlineup.web;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static de.otto.jlineup.browser.Browser.Type.CHROME;
import static de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS;
import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static de.otto.jlineup.config.JobConfig.DEFAULT_REPORT_FORMAT;
import static de.otto.jlineup.config.JobConfig.copyOfBuilder;
import static de.otto.jlineup.config.JobConfig.exampleConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JLineupRunnerFactoryTest {

    private JLineupWebProperties jLineupWebProperties;
    private JLineupRunnerFactory jLineupRunnerFactory;

    @Before
    public void setup() {
        jLineupWebProperties = new JLineupWebProperties();
        jLineupWebProperties.setInstalledBrowsers(Arrays.asList(CHROME, CHROME_HEADLESS));
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
}