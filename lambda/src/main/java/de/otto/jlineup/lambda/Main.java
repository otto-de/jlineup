package de.otto.jlineup.lambda;

import de.otto.jlineup.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
//import de.otto.jlineup.cli.JLineup;

import static java.lang.invoke.MethodHandles.lookup;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    static final int NO_EXIT = -1;

    public static void main(String[] args) {

        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();

        //int exitCode = new CommandLine(new JLineup()).execute(args);
        //exitWithExitCode(exitCode);
    }

    private static void exitWithExitCode(int exitCode) {
        Utils.stopFileLoggers();
        if (exitCode != NO_EXIT) {
            System.exit(exitCode);
        }
    }



}
