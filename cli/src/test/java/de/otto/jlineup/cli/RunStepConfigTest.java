package de.otto.jlineup.cli;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Step;
import org.junit.Test;
import picocli.CommandLine;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RunStepConfigTest {

    @Test
    public void shouldConvertCommandlineParameters() {

        JLineup commandLineParameters = new JLineup();
        String[] params = {
                "--screenshot-dir", "someScreenshotDirectory",
                "--report-dir", "someReportDirectory",
                "--working-dir", "someWorkingDirectory",
                "--step", "after"
        };
        CommandLine commandLine = new CommandLine(commandLineParameters);
        commandLine.parseArgs(params);

        RunStepConfig runStepConfig = Utils.convertCommandLineParametersToRunConfiguration(commandLineParameters);

        assertThat(runStepConfig.getReportDirectory(), is("someReportDirectory"));
        assertThat(runStepConfig.getScreenshotsDirectory(), is("someScreenshotDirectory"));
        assertThat(runStepConfig.getWorkingDirectory(), is("someWorkingDirectory"));
        assertThat(runStepConfig.getStep(), is(Step.after));
    }
}