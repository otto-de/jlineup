package de.otto.jlineup.cli;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;

import java.io.FileNotFoundException;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyMap;

public class Utils {

    public static JobConfig readConfig(final CommandLineParameters parameters) throws FileNotFoundException {
        return JobConfig.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
    }

    public static RunStepConfig convertCommandLineParametersToRunConfiguration(CommandLineParameters commandLineParameters) {
        return RunStepConfig.jLineupRunConfigurationBuilder()
                .withWorkingDirectory(commandLineParameters.getWorkingDirectory())
                .withScreenshotsDirectory(commandLineParameters.getScreenshotDirectory())
                .withReportDirectory(commandLineParameters.getReportDirectory())
                .withStep(commandLineParameters.getStep())
                .withUrlReplacements(firstNonNull(commandLineParameters.getUrlReplacements(), emptyMap()))
                .build();
    }

}
