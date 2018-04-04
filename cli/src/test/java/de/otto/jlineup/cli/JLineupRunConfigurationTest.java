package de.otto.jlineup.cli;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Step;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class JLineupRunConfigurationTest {

    @Test
    public void shouldConvertCommandlineParameters() {

        CommandLineParameters commandLineParameters = new CommandLineParameters();
        String[] params = {
                "--screenshot-dir", "someScreenshotDirectory",
                "--report-dir", "someReportDirectory",
                "--working-dir", "someWorkingDirectory",
                "--step", "after"
        };
        JCommander jCommander = new JCommander(commandLineParameters);
        jCommander.parse(params);

        JLineupRunConfiguration jLineupRunConfiguration = Utils.convertCommandLineParametersToRunConfiguration(commandLineParameters);

        assertThat(jLineupRunConfiguration.getReportDirectory(), is("someReportDirectory"));
        assertThat(jLineupRunConfiguration.getScreenshotsDirectory(), is("someScreenshotDirectory"));
        assertThat(jLineupRunConfiguration.getWorkingDirectory(), is("someWorkingDirectory"));
        assertThat(jLineupRunConfiguration.getStep(), is(Step.after));
    }
}