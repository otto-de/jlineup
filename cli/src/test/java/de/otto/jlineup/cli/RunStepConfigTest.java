package de.otto.jlineup.cli;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Step;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class RunStepConfigTest {

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

        RunStepConfig runStepConfig = Utils.convertCommandLineParametersToRunConfiguration(commandLineParameters);

        assertThat(runStepConfig.getReportDirectory(), is("someReportDirectory"));
        assertThat(runStepConfig.getScreenshotsDirectory(), is("someScreenshotDirectory"));
        assertThat(runStepConfig.getWorkingDirectory(), is("someWorkingDirectory"));
        assertThat(runStepConfig.getStep(), is(Step.after));
    }
}