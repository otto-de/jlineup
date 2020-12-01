package de.otto.jlineup.cli;

import de.otto.jlineup.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static java.lang.invoke.MethodHandles.lookup;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    static final int NO_EXIT = -1;

    public static void main(String[] args) {

        //For GraalVM support in WebdriverManager
        String arch = System.getProperty("os.arch");
        if (arch.endsWith("64") && "Substrate VM".equals(System.getProperty("java.vm.name"))) {
            System.setProperty("wdm.architecture", "X64");
        }

        Utils.setDebugLogLevelsOfSelectedThirdPartyLibsToWarn();

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
