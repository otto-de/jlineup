package de.otto.jlineup.cli;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import de.otto.jlineup.config.Step;

import java.util.*;

public class CommandLineParameters {

    @Parameter(names = {"-?", "--help"}, help = true, description = "Shows this help")
    private boolean help = false;

    @Parameter(names = {"-s", "--step"}, description = "JLineup step - 'before' just takes screenshots, 'after' takes screenshots and compares them with the 'before'-screenshots in the screenshots directory. 'compare' just compares existing screenshots, it's also included in 'after'.")
    private Step step = Step.before;

    @Parameter(names = {"--config", "-c"}, description = "JobConfig file")
    private String configFile = "lineup.json";

    @Parameter(names = {"--working-dir", "-d"}, description = "Path to the working directory")
    private String workingDirectory = ".";

    @Parameter(names = {"--screenshot-dir", "-sd"}, description = "Screenshots directory name - relative to working directory")
    private String screenshotDirectory = "report/screenshots";

    @Parameter(names = {"--report-dir", "-rd"}, description = "HTML report directory name - relative to working directory")
    private String reportDirectory = "report";

    @Parameter(names = {"--url", "-u"}, description = "If you run JLineup without config file, this is the one url that is tested with the default config.")
    private String url = null;

    @Parameter(names = {"--print-config"}, description = "Prints the current (if found) or a default config file to standard out.")
    private boolean printConfig = false;

    @Parameter(names = {"--print-example"}, description = "Prints an example default config file to standard out. Useful as quick start.")
    private boolean printExample = false;

    @Parameter(names = {"--debug"}, description = "Sets the log level to DEBUG, produces verbose information about the current task.")
    private boolean debug = false;

    @Parameter(names = {"--log"}, description = "Sets the log level to DEBUG and logs to a file in the current working directory.")
    private boolean logToFile = false;

    @Parameter(names = {"--version", "-v"}, description = "Prints version information.")
    private boolean version = false;

    @Parameter(names = {"--chrome-parameter"}, description = "Additional command line parameters for spawned chrome processes. Example: --chrome-parameter \"--use-shm=false\"")
    private List<String> chromeParameters = Collections.emptyList();

    @Parameter(names = {"--firefox-parameter"}, description = "Additional command line parameters for spawned firefox processes.")
    private List<String> firefoxParameters = Collections.emptyList();

    @DynamicParameter(names = {"--replace-in-url", "-R"}, description = "The given keys are replaced with the corresponding values in all urls that are tested.")
    private Map<String, String> urlReplacements = new HashMap<>();

    public CommandLineParameters() {

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
        CommandLineParameters that = (CommandLineParameters) o;
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
}
