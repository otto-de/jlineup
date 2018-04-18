package de.otto.jlineup.web.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "jlineup")
public class JLineupWebProperties {

    private String workingDirectory = "/tmp/jlineup/";
    private String screenshotsDirectory = "report-{id}";
    private String reportDirectory = "report-{id}";
    private int maxParallelJobs = 1;
    private int maxThreadsPerJob = 4;
    private List<String> installedBrowsers = Arrays.asList("chrome-headless", "firefox-headless", "phantomjs");

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

    public int getMaxParallelJobs() {
        return maxParallelJobs;
    }

    public void setMaxParallelJobs(int maxParallelJobs) {
        this.maxParallelJobs = maxParallelJobs;
    }

    public List<String> getInstalledBrowsers() {
        return installedBrowsers;
    }

    public void setInstalledBrowsers(List<String> installedBrowsers) {
        this.installedBrowsers = installedBrowsers;
    }

    public int getMaxThreadsPerJob() {
        return maxThreadsPerJob;
    }

    public void setMaxThreadsPerJob(int maxThreadsPerJob) {
        this.maxThreadsPerJob = maxThreadsPerJob;
    }
}
