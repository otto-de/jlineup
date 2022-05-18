package de.otto.jlineup.cli;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;

import java.io.IOException;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class Utils {

    public static JobConfig readConfig(final JLineup parameters) throws IOException {
        return JobConfig.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
    }

    public static RunStepConfig convertCommandLineParametersToRunConfiguration(JLineup commandLineParameters) {
        return RunStepConfig.runStepConfigBuilder()
                .withWorkingDirectory(commandLineParameters.getWorkingDirectory())
                .withScreenshotsDirectory(commandLineParameters.getScreenshotDirectory())
                .withReportDirectory(commandLineParameters.getReportDirectory())
                .withStep(commandLineParameters.getStep())
                .withUrlReplacements(firstNonNull(commandLineParameters.getUrlReplacements(), emptyMap()))
                .withChromeParameters(firstNonNull(commandLineParameters.getChromeParameters(), emptyList()))
                .withFirefoxParameters(firstNonNull(commandLineParameters.getFirefoxParameters(), emptyList()))
                .build();
    }

}
