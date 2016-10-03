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
}
