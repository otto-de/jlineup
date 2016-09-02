package de.otto.jlineup.config;

import com.beust.jcommander.Parameter;

public class Parameters {

    @Parameter(names = "--help", help = true, description = "Shows this help")
    private boolean help = false;

    @Parameter(names = "--step", description = "JLineup step - 'before' just takes screenshots, 'after' takes screenshots and compares them with the 'before'-screenshots in the screenshots directory")
    private Step astep = Step.before;

    @Parameter(names = {"--config", "-c"}, description = "Config file")
    private String configFile = "lineup.json";

    @Parameter(names = {"--working-dir", "-d"}, description = "Path to the working directory")
    private String workingDirectory = ".";

    @Parameter(names = {"--screenshot-dir", "-s"}, description = "Screenshots directory name - relative to working directory")
    private String screenshotDirectory = "screenshots";

    @Parameter(names = {"--report-dir", "-r"}, description = "HTML report directory name - relative to working directory")
    private String reportDirectory = "report";

    @Parameter(names = {"--compare", "-j"}, description = "Just compare the existing screenshots.")
    private boolean justCompare = false;

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
        return astep == Step.after;
    }

    public boolean isBefore() {
        return astep != Step.after && !justCompare;
    }

    public boolean isJustCompare() {
        return justCompare;
    }

    public boolean isHelp() {
        return help;
    }
}
