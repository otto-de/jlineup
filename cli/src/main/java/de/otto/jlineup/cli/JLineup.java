package de.otto.jlineup.cli;

import de.otto.jlineup.GlobalOption;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.CloudBrowserFactory;
import de.otto.jlineup.config.ConfigMerger;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.exceptions.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import static de.otto.jlineup.cli.Main.NO_EXIT;
import static de.otto.jlineup.cli.Utils.convertCommandLineParametersToRunConfiguration;
import static de.otto.jlineup.cli.Utils.readConfig;
import static de.otto.jlineup.file.FileService.REPORT_HTML_FILENAME;
import static java.lang.invoke.MethodHandles.lookup;

@Command(name = "jlineup", sortOptions = false, modelTransformer = JLineup.OptionsFilter.class)
public class JLineup implements Callable<Integer> {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @Option(names = {"-?", "--help"}, usageHelp = true, description = "Shows this help. See https://github.com/otto-de/jlineup/blob/main/README.md for full docs.", order = 0)
    private boolean help = false;

    @Option(names = {"-u", "--url"}, description = "If you run JLineup without config file, this is the one url that is tested with the default config.", order = 10)
    private String url = null;

    @Option(names = {"-s", "--step"}, description = "JLineup step - 'before' just takes screenshots, 'after' takes screenshots and compares them with the 'before'-screenshots in the screenshots directory. 'compare' just compares existing screenshots, it's also included in 'after'.", order = 20)
    private RunStep step = RunStep.before;

    @Option(names = {"-c", "--config"}, description = "The job config file which contains the url(s) and the settings for the JLineup run. See https://github.com/otto-de/jlineup/blob/main/docs/CONFIGURATION.md for all configuration options.", order = 30)
    private String configFile = "lineup.json";

    @Option(names = {"-m", "--merge-config"}, description = "(Preview Feature) Additional config that will be merged with the given config file. Identical local values have precedence. URL keys are interpreted as regex matchers.", order = 40)
    private String mergeConfigFile = null;

    @Option(names = {"-d", "--working-dir"}, description = "Path to the working directory", order = 50)
    private String workingDirectory = ".";

    @Option(names = {"-sd", "--screenshot-dir"}, description = "Screenshots directory name - relative to working directory", order = 60)
    private String screenshotDirectory = "report/screenshots";

    @Option(names = {"-rd", "--report-dir"}, description = "HTML report directory name - relative to working directory", order = 70)
    private String reportDirectory = "report";

    @Option(names = {"--print-config"}, description = "Prints the current (if found) or a default config file to standard out.", order = 80)
    private boolean printConfig = false;

    @Option(names = {"--print-example"}, description = "Prints an example default config file to standard out. Useful as quick start.", order = 90)
    private boolean printExample = false;

    @Option(names = {"--debug"}, description = "Sets the log level to DEBUG, produces verbose information about the current task.", order = 100)
    private boolean debug = false;

    @Option(names = {"--log"}, description = "Sets the log level to DEBUG and logs to a file in the current working directory.", order = 110)
    private boolean logToFile = false;

    @Option(names = {"-v", "--version"}, description = "Prints version information.", order = 120)
    private boolean version = false;

    @Option(names = {"--chrome-parameter"}, description = "Additional command line parameters for spawned chrome processes. Example: --chrome-parameter \"--use-shm=false\"", order = 130)
    private List<String> chromeParameters;

    @Option(names = {"--firefox-parameter"}, description = "Additional command line parameters for spawned firefox processes.", order = 140)
    private List<String> firefoxParameters;

    @Option(names = {"-R", "--replace-in-url"}, description = "The given keys are replaced with the corresponding values in all urls that are tested.", order = 150)
    private Map<String, String> urlReplacements = new HashMap<>();

    @Option(names = {"-o", "--open-report"}, description = "Opens html report after the run.", order = 160)
    private boolean openReport = false;

    @Option(names = {"-k", "--keep-existing"}, description = "(Preview feature) Keep existing 'before' screenshots after having added new urls or paths to the config.", order = 170)
    private boolean keepExisting = false;

    @Option(names = {"--refresh-url"}, description = "(Preview feature) Refresh 'before' screenshots for the given url only. Implicitly sets '--keep-existing' also.", order = 180)
    private String refreshUrl = null;

    @Option(names = {"-b", "--override-browser"}, description = "(Preview feature) Override browser setting in run config.", order = 190)
    private String browserOverride = null;

    @Option(names = {"--cleanup-profile"}, description = "Cleanup browser profile directory after the run has finished and a profile dir was specified with the browser parameters.", order = 200)
    private boolean cleanupProfile = false;

    /**
     * This options filter adds the fitting global options to the command spec.
     */
    static class OptionsFilter implements CommandLine.IModelTransformer {
        @Override
        public CommandLine.Model.CommandSpec transform(CommandLine.Model.CommandSpec commandSpec) {

            commandSpec.addOption(CommandLine.Model.OptionSpec.builder("--" + GlobalOption.JLINEUP_CROP_LAST_SCREENSHOT.kebabCaseNameWithoutJLineupPrefix())
                    .order(201).description("If this is set, JLineup will crop the last screenshot when scrolling to match the previous one.")
                    .parameterConsumer((stack, argSpec, commandSpec1) -> {
                        GlobalOptions.setOption(GlobalOption.JLINEUP_CROP_LAST_SCREENSHOT, "true");
                    })
                    .build());

            commandSpec.addOption(CommandLine.Model.OptionSpec.builder("--" + GlobalOption.JLINEUP_CHROME_VERSION.kebabCaseNameWithoutJLineupPrefix())
                    .order(202).description("The version of the Chrome browser to use. If not set, the installed or default version is used.")
                    .parameterConsumer((stack, argSpec, commandSpec1) -> {
                        String value = stack.pop();
                        GlobalOptions.setOption(GlobalOption.JLINEUP_CHROME_VERSION, value);
                    })
                    .build());

            commandSpec.addOption(CommandLine.Model.OptionSpec.builder("--" + GlobalOption.JLINEUP_FIREFOX_VERSION.kebabCaseNameWithoutJLineupPrefix())
                    .order(203).description("The version of the Firefox browser to use. If not set, the installed or default version is used.")
                    .parameterConsumer((stack, argSpec, commandSpec1) -> {
                        String value = stack.pop();
                        GlobalOptions.setOption(GlobalOption.JLINEUP_FIREFOX_VERSION, value);
                    })
                    .build());

            try {
                Class<?> lambdaBrowserClass = Class.forName("de.otto.jlineup.lambda.LambdaBrowser", false, CloudBrowserFactory.class.getClassLoader());
                LOG.debug("LambdaBrowser '{}' reachable, adding lambda options to command spec.", lambdaBrowserClass.getCanonicalName());
                commandSpec.addOption(CommandLine.Model.OptionSpec.builder("-F", "--" + GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME.kebabCaseNameWithoutJLineupPrefix())
                        .order(210).description("This specifies the name or the arn of the AWS lambda function")
                        .parameterConsumer((stack, argSpec, commandSpec1) -> {
                            String value = stack.pop();
                            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME, value);
                        })
                        .build());
                commandSpec.addOption(CommandLine.Model.OptionSpec.builder("-P", "--" + GlobalOption.JLINEUP_LAMBDA_AWS_PROFILE.kebabCaseNameWithoutJLineupPrefix())
                        .order(220).description("The AWS profile for calling the lambda function and for accessing S3")
                        .parameterConsumer((stack, argSpec, commandSpec1) -> {
                            String value = stack.pop();
                            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_AWS_PROFILE, value);
                        })
                        .build());
            } catch (ClassNotFoundException e) {
                LOG.debug("No LambdaBrowser reachable.", e);
            }
            return commandSpec;
        }
    }


    public JLineup() {
    }

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

    public String getMergeConfigFile() {
        return mergeConfigFile;
    }

    public boolean isAfter() {
        return step == RunStep.after;
    }

    public boolean isBefore() {
        return step != RunStep.after && step != RunStep.compare && step != RunStep.after_only;
    }

    public boolean isJustCompare() {
        return step == RunStep.compare;
    }

    public boolean isHelp() {
        return help;
    }

    public RunStep getStep() {
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

    public boolean isOpenReport() {
        return openReport;
    }

    public boolean isKeepExisting() {
        return keepExisting;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public String getBrowserOverride() {
        return browserOverride;
    }

    public boolean isCleanupProfile() {
        return cleanupProfile;
    }

    public void setCleanupProfile(boolean cleanupProfile) {
        this.cleanupProfile = cleanupProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineup jLineup = (JLineup) o;
        return help == jLineup.help && printConfig == jLineup.printConfig && printExample == jLineup.printExample && debug == jLineup.debug && logToFile == jLineup.logToFile && version == jLineup.version && openReport == jLineup.openReport && keepExisting == jLineup.keepExisting && cleanupProfile == jLineup.cleanupProfile && Objects.equals(url, jLineup.url) && step == jLineup.step && Objects.equals(configFile, jLineup.configFile) && Objects.equals(mergeConfigFile, jLineup.mergeConfigFile) && Objects.equals(workingDirectory, jLineup.workingDirectory) && Objects.equals(screenshotDirectory, jLineup.screenshotDirectory) && Objects.equals(reportDirectory, jLineup.reportDirectory) && Objects.equals(chromeParameters, jLineup.chromeParameters) && Objects.equals(firefoxParameters, jLineup.firefoxParameters) && Objects.equals(urlReplacements, jLineup.urlReplacements) && Objects.equals(refreshUrl, jLineup.refreshUrl) && Objects.equals(browserOverride, jLineup.browserOverride);
    }

    @Override
    public int hashCode() {
        return Objects.hash(help, url, step, configFile, mergeConfigFile, workingDirectory, screenshotDirectory, reportDirectory, printConfig, printExample, debug, logToFile, version, chromeParameters, firefoxParameters, urlReplacements, openReport, keepExisting, refreshUrl, browserOverride, cleanupProfile);
    }

    @Override
    public String toString() {
        return "JLineup{" +
                "help=" + help +
                ", url='" + url + '\'' +
                ", step=" + step +
                ", configFile='" + configFile + '\'' +
                ", mergeConfigFile='" + mergeConfigFile + '\'' +
                ", workingDirectory='" + workingDirectory + '\'' +
                ", screenshotDirectory='" + screenshotDirectory + '\'' +
                ", reportDirectory='" + reportDirectory + '\'' +
                ", printConfig=" + printConfig +
                ", printExample=" + printExample +
                ", debug=" + debug +
                ", logToFile=" + logToFile +
                ", version=" + version +
                ", chromeParameters=" + chromeParameters +
                ", firefoxParameters=" + firefoxParameters +
                ", urlReplacements=" + urlReplacements +
                ", openReport=" + openReport +
                ", keepExisting=" + keepExisting +
                ", refreshUrl='" + refreshUrl + '\'' +
                ", browserOverride='" + browserOverride + '\'' +
                ", cleanupProfile=" + cleanupProfile +
                '}';
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

        //If refresh url is set, keepExisting is mandatory!
        if (getRefreshUrl() != null) {
            keepExisting = true;
        }

        if (step != RunStep.before && isKeepExisting()) {
            LOG.warn("The --keep-existing option is only usable in combination with the 'before' step. It is ignored for step '{}'.", step);
        }

        JobConfig jobConfig = null;
        try {
            jobConfig = buildConfig(this);
        } catch (IOException e) {
            LOG.error("Error building config.", e);
            return 1;
        }

        if (getMergeConfigFile() != null) {
            JobConfig mergeConfig = JobConfig.readConfig(workingDirectory, getMergeConfigFile());
            jobConfig = ConfigMerger.mergeJobConfigWithMergeConfig(jobConfig, mergeConfig);
        }

        if (jobConfig.mergeConfig != null) {
            JobConfig mainGlobalConfig = JobConfig.copyOfBuilder(jobConfig).withMergeConfig(null).build();
            JobConfig mergeGlobalConfig = jobConfig.mergeConfig;
            jobConfig = ConfigMerger.mergeJobConfigWithMergeConfig(mainGlobalConfig, mergeGlobalConfig);
        }

        jobConfig = jobConfig.insertDefaults();

        if (browserOverride != null) {
            jobConfig = JobConfig.copyOfBuilder(jobConfig).withBrowser(Browser.Type.forValue(browserOverride)).build();
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
            if (openReport) {
                Desktop.getDesktop().browse(new URI("file://" + Paths.get(String.format("%s/%s/%s", runStepConfig.getWorkingDirectory(), runStepConfig.getReportDirectory(), REPORT_HTML_FILENAME)).toAbsolutePath()));
            }
            if (!runSucceeded) {
                return 1;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            de.otto.jlineup.Utils.writeInfosForCommonErrors(e.getMessage());
            return 1;
        }
        return NO_EXIT;
    }

    private JobConfig buildConfig(JLineup parameters) throws IOException {
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
