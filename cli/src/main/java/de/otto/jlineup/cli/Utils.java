package de.otto.jlineup.cli;

import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Config;

import java.io.FileNotFoundException;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyMap;

public class Utils {

    public static Config readConfig(final CommandLineParameters parameters) throws FileNotFoundException {
        return Config.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
    }

    public static JLineupRunConfiguration convertCommandLineParametersToRunConfiguration(CommandLineParameters commandLineParameters) {
        return JLineupRunConfiguration.jLineupRunConfigurationBuilder()
                .withWorkingDirectory(commandLineParameters.getWorkingDirectory())
                .withScreenshotsDirectory(commandLineParameters.getScreenshotDirectory())
                .withReportDirectory(commandLineParameters.getReportDirectory())
                .withStep(commandLineParameters.getStep())
                .withUrlReplacements(firstNonNull(commandLineParameters.getUrlReplacements(), emptyMap()))
                .build();
    }

}
