package de.otto.jlineup.web.configuration;

import de.otto.jlineup.browser.Browser;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

import static de.otto.jlineup.browser.Browser.Type.*;
import static java.util.Collections.emptyList;

@ConfigurationProperties(prefix = "jlineup")
public class JLineupWebProperties {

    public static final int DEFAULT_MAX_PARALLEL_JOBS = 1;
    public static final int DEFAULT_MAX_THREADS_PER_JOB = 4;

    private String workingDirectory = "/tmp/jlineup/";
    private String screenshotsDirectory = "report-{id}";
    private String reportDirectory = "report-{id}";
    private int maxParallelJobs = DEFAULT_MAX_PARALLEL_JOBS;
    private int maxThreadsPerJob = DEFAULT_MAX_THREADS_PER_JOB;

    private List<String> chromeLaunchParameters = emptyList();
    private List<String> firefoxLaunchParameters = emptyList();

    private List<Browser.Type> installedBrowsers = Arrays.asList(
            CHROME_HEADLESS,
            FIREFOX_HEADLESS,
            PHANTOMJS);

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

    public List<Browser.Type> getInstalledBrowsers() {
        return installedBrowsers;
    }

    public void setInstalledBrowsers(List<Browser.Type> installedBrowsers) {
        this.installedBrowsers = installedBrowsers;
    }

    public int getMaxThreadsPerJob() {
        return maxThreadsPerJob;
    }

    public void setMaxThreadsPerJob(int maxThreadsPerJob) {
        this.maxThreadsPerJob = maxThreadsPerJob;
    }

    public List<String> getChromeLaunchParameters() {
        return chromeLaunchParameters;
    }

    public List<String> getFirefoxLaunchParameters() {
        return firefoxLaunchParameters;
    }

    public void setFirefoxLaunchParameters(List<String> firefoxLaunchParameters) {
        this.firefoxLaunchParameters = firefoxLaunchParameters;
    }

    public void setChromeLaunchParameters(List<String> chromeLaunchParameters) {
        this.chromeLaunchParameters = chromeLaunchParameters;
    }
}
