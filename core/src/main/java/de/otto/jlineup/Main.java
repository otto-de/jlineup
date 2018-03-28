package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.CommandLineParameters;
import de.otto.jlineup.config.Config;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws Exception {

        final CommandLineParameters parameters = new CommandLineParameters();
        final JCommander jCommander = new JCommander(parameters);
        jCommander.parse(args);
        jCommander.setProgramName("JLineup");
        if (parameters.isHelp()) {
            jCommander.usage();
            System.out.printf("Version: %s%n", Util.getVersion());
            return;
        }

        if (parameters.isVersion()) {
            System.out.printf("JLineup version %s", Util.getVersion());
            return;
        }

        if (parameters.isDebug()) {
            Util.setLogLevelToDebug();
        }

        Config config = null;
        try {
            config = buildConfig(parameters);
        } catch(FileNotFoundException e) {
            System.exit(1);
        }

        if (parameters.isPrintConfig()) {
            System.out.println(Util.createPrettyConfigJson(config));
            System.exit(0);
        }

        if (config.debug) {
            Util.setLogLevelToDebug();
        }

        if (config.logToFile || parameters.isLogToFile()) {
            Util.logToFile(parameters.getWorkingDirectory());
        }

        System.out.printf("Running JLineup [%s] with step '%s'.%n%n", Util.getVersion(), parameters.getStep());

        JLineupRunConfiguration jLineupRunConfiguration = JLineupRunConfiguration.fromCommandlineParameters(parameters);
        JLineup jLineup = new JLineup(config, jLineupRunConfiguration);

        int errorLevel = jLineup.run();
        if (errorLevel != 0) {
            System.exit(errorLevel);
        }
    }

    private static Config buildConfig(CommandLineParameters parameters) throws FileNotFoundException {
        Config config;
        if (parameters.getUrl() != null) {
            String url = BrowserUtils.prependHTTPIfNotThereAndToLowerCase(parameters.getUrl());
            config = Config.defaultConfig(url);
            if (!parameters.isPrintConfig()) {
                System.out.printf("You specified an explicit URL parameter (%s), any given config file is ignored! This should only be done for testing purpose.%n", url);
                System.out.printf("Using generated config:%n%s%n", Util.createPrettyConfigJson(config));
                System.out.println("You can take this generated config as base and save it as a text file named 'lineup.json'.");
                System.out.println("Just add --print-config parameter to let JLineup print an example config");
            }
        } else {
            try {
                config = Config.readConfig(parameters);
            } catch (FileNotFoundException e) {
                if (!parameters.isPrintConfig()) {
                    System.err.println(e.getMessage());
                    System.err.println("Use --help to see the JLineup quick help.");
                    throw e;
                } else {
                    return Config.exampleConfig();
                }
            }
        }
        return config;


    }

}
