package de.otto.jlineup.config;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

import java.util.HashMap;
import java.util.Map;

import static de.otto.jlineup.config.Step.*;

public class Parameters {

    @Parameter(names = {"-?", "--help"}, help = true, description = "Shows this help")
    private boolean help = false;

    @Parameter(names = {"-s", "--step"}, description = "JLineup step - 'before' just takes screenshots, 'after' takes screenshots and compares them with the 'before'-screenshots in the screenshots directory. 'compare' just compares existing screenshots, it's also included in 'after'.")
    private Step step = before;

    @Parameter(names = {"--config", "-c"}, description = "Config file")
    private String configFile = "lineup.json";

    @Parameter(names = {"--working-dir", "-d"}, description = "Path to the working directory")
    private String workingDirectory = ".";

    @Parameter(names = {"--screenshot-dir", "-sd"}, description = "Screenshots directory name - relative to working directory")
    private String screenshotDirectory = "screenshots";

    @Parameter(names = {"--report-dir", "-rd"}, description = "HTML report directory name - relative to working directory")
    private String reportDirectory = "report";

    @Parameter(names = {"--url", "-u"}, description = "If you run JLineup without config file, this is the one url that is tested with the default config.")
    private String url = null;

    @Parameter(names = {"--print-config"}, description = "Prints a default config file to standard out. Useful as quick start.")
    private boolean printConfig = false;

    @DynamicParameter(names = {"--replace-in-url", "-R"}, description = "The given keys are replaced with the corresponding values in all urls that are tested.")
    private Map<String, String> urlReplacements = new HashMap<>();

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
        return step == after;
    }

    public boolean isBefore() {
        return step != after && step != compare;
    }

    public boolean isJustCompare() {
        return step == compare;
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

    @Override
    public String toString() {
        return "Parameters{" +
                "help=" + help +
                ", step=" + step +
                ", configFile='" + configFile + '\'' +
                ", workingDirectory='" + workingDirectory + '\'' +
                ", screenshotDirectory='" + screenshotDirectory + '\'' +
                ", reportDirectory='" + reportDirectory + '\'' +
                ", url='" + url + '\'' +
                ", printConfig=" + printConfig +
                ", urlReplacements=" + urlReplacements +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameters that = (Parameters) o;

        if (help != that.help) return false;
        if (printConfig != that.printConfig) return false;
        if (step != that.step) return false;
        if (configFile != null ? !configFile.equals(that.configFile) : that.configFile != null) return false;
        if (workingDirectory != null ? !workingDirectory.equals(that.workingDirectory) : that.workingDirectory != null)
            return false;
        if (screenshotDirectory != null ? !screenshotDirectory.equals(that.screenshotDirectory) : that.screenshotDirectory != null)
            return false;
        if (reportDirectory != null ? !reportDirectory.equals(that.reportDirectory) : that.reportDirectory != null)
            return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return urlReplacements != null ? urlReplacements.equals(that.urlReplacements) : that.urlReplacements == null;

    }

    @Override
    public int hashCode() {
        int result = (help ? 1 : 0);
        result = 31 * result + (step != null ? step.hashCode() : 0);
        result = 31 * result + (configFile != null ? configFile.hashCode() : 0);
        result = 31 * result + (workingDirectory != null ? workingDirectory.hashCode() : 0);
        result = 31 * result + (screenshotDirectory != null ? screenshotDirectory.hashCode() : 0);
        result = 31 * result + (reportDirectory != null ? reportDirectory.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (printConfig ? 1 : 0);
        result = 31 * result + (urlReplacements != null ? urlReplacements.hashCode() : 0);
        return result;
    }

}
