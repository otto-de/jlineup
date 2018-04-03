package de.otto.jlineup.web.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jlineup")
public class JLineupWebProperties {

    private String workingDirectoryPrefix = "/tmp/jlineup-run-";
    private String screenshotsDirectory = "screenshots";
    private String reportDirectory = "report";

    public String getWorkingDirectoryPrefix() {
        return workingDirectoryPrefix;
    }

    public String getScreenshotsDirectory() {
        return screenshotsDirectory;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    public void setWorkingDirectoryPrefix(String workingDirectoryPrefix) {
        this.workingDirectoryPrefix = workingDirectoryPrefix;
    }

    public void setScreenshotsDirectory(String screenshotsDirectory) {
        this.screenshotsDirectory = screenshotsDirectory;
    }

    public void setReportDirectory(String reportDirectory) {
        this.reportDirectory = reportDirectory;
    }
}
