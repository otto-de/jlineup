package de.otto.jlineup.cli;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.exceptions.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static de.otto.jlineup.cli.Main.NO_EXIT;
import static de.otto.jlineup.cli.Utils.convertCommandLineParametersToRunConfiguration;
import static de.otto.jlineup.cli.Utils.readConfig;
import static java.lang.invoke.MethodHandles.lookup;

@Command(sortOptions = false)
public class JLineup implements Callable<Integer> {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @Option(names = {"-?", "--help"}, usageHelp = true, description = "Shows this help", order = 0)
    private boolean help = false;

    @Option(names = {"--url", "-u"}, description = "If you run JLineup without config file, this is the one url that is tested with the default config.", order = 1)
    private String url = null;

    @Option(names = {"-s", "--step"}, description = "JLineup step - 'before' just takes screenshots, 'after' takes screenshots and compares them with the 'before'-screenshots in the screenshots directory. 'compare' just compares existing screenshots, it's also included in 'after'.", order = 2)
    private Step step = Step.before;

    @Option(names = {"--config", "-c"}, description = "JobConfig file", order = 3)
    private String configFile = "lineup.json";

    @Option(names = {"--working-dir", "-d"}, description = "Path to the working directory", order = 4)
    private String workingDirectory = ".";

    @Option(names = {"--screenshot-dir", "-sd"}, description = "Screenshots directory name - relative to working directory", order = 5)
    private String screenshotDirectory = "report/screenshots";

    @Option(names = {"--report-dir", "-rd"}, description = "HTML report directory name - relative to working directory", order = 6)
    private String reportDirectory = "report";

    @Option(names = {"--print-config"}, description = "Prints the current (if found) or a default config file to standard out.", order = 7)
    private boolean printConfig = false;

    @Option(names = {"--print-example"}, description = "Prints an example default config file to standard out. Useful as quick start.", order = 8)
    private boolean printExample = false;

    @Option(names = {"--debug"}, description = "Sets the log level to DEBUG, produces verbose information about the current task.", order = 9)
    private boolean debug = false;

    @Option(names = {"--log"}, description = "Sets the log level to DEBUG and logs to a file in the current working directory.", order = 10)
    private boolean logToFile = false;

    @Option(names = {"--version", "-v"}, description = "Prints version information.", order = 11)
    private boolean version = false;

    @Option(names = {"--chrome-parameter"}, description = "Additional command line parameters for spawned chrome processes. Example: --chrome-parameter \"--use-shm=false\"", order = 12)
    private List<String> chromeParameters;

    @Option(names = {"--firefox-parameter"}, description = "Additional command line parameters for spawned firefox processes.", order = 13)
    private List<String> firefoxParameters;

    @Option(names = {"--replace-in-url", "-R"}, description = "The given keys are replaced with the corresponding values in all urls that are tested.", order = 14)
    private Map<String, String> urlReplacements = new HashMap<>();

    public JLineup() {

    };

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getScreenshotDirectory() {
        return screenshotDirectory;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    String getConfigFile() {
        return configFile;
    }

    public boolean isAfter() {
        return step == Step.after;
    }

    public boolean isBefore() {
        return step != Step.after && step != Step.compare;
    }

    public boolean isJustCompare() {
        return step == Step.compare;
    }

    public boolean isHelp() {
        return help;
    }

    public Step getStep() {
        return step;
    }

    public Map<String, String> getUrlReplacements() {
        return urlReplacements;
    }

    public String getUrl() {
        return url;
    }

    public boolean isPrintConfig() {
        return printConfig;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public boolean isVersion() {
        return version;
    }

    public List<String> getChromeParameters() {
        return chromeParameters;
    }

    public List<String> getFirefoxParameters() {
        return firefoxParameters;
    }

    public boolean isPrintExample() {
        return printExample;
    }


    @Override
    public String toString() {
        return "CommandLineParameters{" +
                "help=" + help +
                ", step=" + step +
                ", configFile='" + configFile + '\'' +
                ", workingDirectory='" + workingDirectory + '\'' +
                ", screenshotDirectory='" + screenshotDirectory + '\'' +
                ", reportDirectory='" + reportDirectory + '\'' +
                ", url='" + url + '\'' +
                ", printConfig=" + printConfig +
                ", printExample=" + printExample +
                ", debug=" + debug +
                ", logToFile=" + logToFile +
                ", version=" + version +
                ", chromeParameters=" + chromeParameters +
                ", firefoxParameters=" + firefoxParameters +
                ", urlReplacements=" + urlReplacements +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineup that = (JLineup) o;
        return help == that.help &&
                printConfig == that.printConfig &&
                printExample == that.printExample &&
                debug == that.debug &&
                logToFile == that.logToFile &&
                version == that.version &&
                step == that.step &&
                Objects.equals(configFile, that.configFile) &&
                Objects.equals(workingDirectory, that.workingDirectory) &&
                Objects.equals(screenshotDirectory, that.screenshotDirectory) &&
                Objects.equals(reportDirectory, that.reportDirectory) &&
                Objects.equals(url, that.url) &&
                Objects.equals(chromeParameters, that.chromeParameters) &&
                Objects.equals(firefoxParameters, that.firefoxParameters) &&
                Objects.equals(urlReplacements, that.urlReplacements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(help, step, configFile, workingDirectory, screenshotDirectory, reportDirectory, url, printConfig, printExample, debug, logToFile, version, chromeParameters, firefoxParameters, urlReplacements);
    }

    @Override
    public Integer call() throws Exception {

        if (help) {
            CommandLine.usage(new JLineup(), System.out);
            LOG.info("Version: {}\n", de.otto.jlineup.Utils.getVersion());
            return NO_EXIT;
        }

        if (version) {
            LOG.info("JLineup version {}", de.otto.jlineup.Utils.getVersion());
            return NO_EXIT;
        }

        if (debug) {
            de.otto.jlineup.Utils.setLogLevelToDebug();
        }

        if (printExample) {
            System.out.println(JobConfig.prettyPrintWithAllFields(JobConfig.exampleConfig()));
            return 0;
        }

        JobConfig jobConfig = null;
        try {
            jobConfig = buildConfig(this);
        } catch(IOException e) {
            LOG.error("Error building config.", e);
            return 1;
        }

        if (printConfig) {
            System.out.println(JobConfig.prettyPrint(jobConfig));
            return 0;
        }

        if (jobConfig.debug) {
            de.otto.jlineup.Utils.setLogLevelToDebug();
        }

        if (jobConfig.logToFile || logToFile) {
            de.otto.jlineup.Utils.logToFile(workingDirectory);
        }

        LOG.info("Running JLineup [{}] with step '{}'.\n\n", de.otto.jlineup.Utils.getVersion(), step);

        RunStepConfig runStepConfig = convertCommandLineParametersToRunConfiguration(this);

        JLineupRunner jLineupRunner = null;
        try {
            jLineupRunner = new JLineupRunner(jobConfig, runStepConfig);
        } catch (ValidationError e) {
            LOG.error(e.getMessage());
            return 1;
        }

        try {
            boolean runSucceeded = jLineupRunner.run();
            if (!runSucceeded) {
                return 1;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(),e);
            de.otto.jlineup.Utils.writeInfosForCommonErrors(e.getMessage());
            return 1;
        }
        return NO_EXIT;
    }

    private static JobConfig buildConfig(JLineup parameters) throws IOException {
        JobConfig jobConfig;
        if (parameters.getUrl() != null) {
            String url = BrowserUtils.prependHTTPIfNotThereAndToLowerCase(parameters.getUrl());
            jobConfig = JobConfig.defaultConfig(url);
            if (!parameters.isPrintConfig()) {
                LOG.info("You specified an explicit URL parameter ({}), any given jobConfig file is ignored! This should only be done for testing purposes.", url);
                LOG.info("Using generated jobConfig:\n\n{}", JobConfig.prettyPrint(jobConfig));
                LOG.info("\nYou can take this generated jobConfig as base and save it as a text file named 'lineup.json'.");
                LOG.info("Just add --print-config parameter to let JLineup print a more detailed example jobConfig");
            }
        } else {
            try {
                jobConfig = readConfig(parameters);
            } catch (Exception e) {
                if (!parameters.isPrintConfig()) {
                    LOG.error(e.getMessage());
                    LOG.error("Use --help to see the JLineup quick help.");
                    throw e;
                } else {
                    return JobConfig.exampleConfig();
                }
            }
        }
        return jobConfig;
    }
}
