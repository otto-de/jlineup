package de.otto.jlineup.cli;

import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;

import static de.otto.jlineup.cli.Utils.readConfig;
import static java.lang.invoke.MethodHandles.lookup;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    static final int NO_EXIT = -1;

    public static void main(String[] args) {

        int exitCode = new CommandLine(new JLineup()).execute(args);
        exitWithExitCode(exitCode);
    }

    private static void exitWithExitCode(int exitCode) {
        Utils.stopFileLoggers();
        if (exitCode != NO_EXIT) {
            System.exit(exitCode);
        }
    }



}
