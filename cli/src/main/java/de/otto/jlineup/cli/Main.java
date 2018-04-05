package de.otto.jlineup.cli;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws Exception {

        final CommandLineParameters parameters = new CommandLineParameters();
        final JCommander jCommander = new JCommander(parameters);
        jCommander.parse(args);
        jCommander.setProgramName("JLineupRunner");
        if (parameters.isHelp()) {
            jCommander.usage();
            System.out.printf("Version: %s%n", Utils.getVersion());
            return;
        }

        if (parameters.isVersion()) {
            System.out.printf("JLineupRunner version %s", Utils.getVersion());
            return;
        }

        if (parameters.isDebug()) {
            Utils.setLogLevelToDebug();
        }

        JobConfig jobConfig = null;
        try {
            jobConfig = buildConfig(parameters);
        } catch(FileNotFoundException e) {
            System.exit(1);
        }

        if (parameters.isPrintConfig()) {
            System.out.println(JobConfig.prettyPrint(jobConfig));
            System.exit(0);
        }

        if (jobConfig.debug) {
            Utils.setLogLevelToDebug();
        }

        if (jobConfig.logToFile || parameters.isLogToFile()) {
            Utils.logToFile(parameters.getWorkingDirectory());
        }

        System.out.printf("Running JLineupRunner [%s] with step '%s'.%n%n", Utils.getVersion(), parameters.getStep());

        RunStepConfig runStepConfig = de.otto.jlineup.cli.Utils.convertCommandLineParametersToRunConfiguration(parameters);
        JLineupRunner jLineupRunner = new JLineupRunner(jobConfig, runStepConfig);

        int errorLevel = jLineupRunner.run();
        if (errorLevel != 0) {
            System.exit(errorLevel);
        }
    }

    private static JobConfig buildConfig(CommandLineParameters parameters) throws FileNotFoundException {
        JobConfig jobConfig;
        if (parameters.getUrl() != null) {
            String url = BrowserUtils.prependHTTPIfNotThereAndToLowerCase(parameters.getUrl());
            jobConfig = JobConfig.defaultConfig(url);
            if (!parameters.isPrintConfig()) {
                System.out.printf("You specified an explicit URL parameter (%s), any given jobConfig file is ignored! This should only be done for testing purpose.%n", url);
                System.out.printf("Using generated jobConfig:%n%s%n", JobConfig.prettyPrint(jobConfig));
                System.out.println("You can take this generated jobConfig as base and save it as a text file named 'lineup.json'.");
                System.out.println("Just add --print-jobConfig parameter to let JLineupRunner print an example jobConfig");
            }
        } else {
            try {
                jobConfig = de.otto.jlineup.cli.Utils.readConfig(parameters);
            } catch (FileNotFoundException e) {
                if (!parameters.isPrintConfig()) {
                    System.err.println(e.getMessage());
                    System.err.println("Use --help to see the JLineupRunner quick help.");
                    throw e;
                } else {
                    return JobConfig.exampleConfig();
                }
            }
        }
        return jobConfig;


    }

}
