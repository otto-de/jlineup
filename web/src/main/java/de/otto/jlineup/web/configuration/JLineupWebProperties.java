package de.otto.jlineup.web.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jlineup")
public class JLineupWebProperties {

    private String workingDirectory = "/tmp/jlineup/";
    private String screenshotsDirectory = "report-{id}";
    private String reportDirectory = "report-{id}";

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getScreenshotsDirectory() {
        return screenshotsDirectory;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setScreenshotsDirectory(String screenshotsDirectory) {
        this.screenshotsDirectory = screenshotsDirectory;
    }

    public void setReportDirectory(String reportDirectory) {
        this.reportDirectory = reportDirectory;
    }
}
