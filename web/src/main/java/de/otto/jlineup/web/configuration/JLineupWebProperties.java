package de.otto.jlineup.web.configuration;

import de.otto.jlineup.browser.Browser;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static de.otto.jlineup.browser.Browser.Type.*;
import static java.util.Collections.emptyList;

@ConfigurationProperties(prefix = "jlineup")
public class JLineupWebProperties {

    public static final int DEFAULT_MAX_PARALLEL_JOBS = 1;
    public static final int DEFAULT_MAX_THREADS_PER_JOB = 4;

    private String workingDirectory = "/tmp/jlineup/";
    private String screenshotsDirectory = "report-{id}";
    private String reportDirectory = "report-{id}";
    private boolean cleanupProfile = true;
    private int maxParallelJobs = DEFAULT_MAX_PARALLEL_JOBS;
    private int maxThreadsPerJob = DEFAULT_MAX_THREADS_PER_JOB;
    private List<String> chromeLaunchParameters = emptyList();

    private List<String> firefoxLaunchParameters = emptyList();
    private List<Browser.Type> installedBrowsers = Arrays.asList(
            CHROME_HEADLESS,
            FIREFOX_HEADLESS);

    private JLineupWebLambdaProperties lambda = new JLineupWebLambdaProperties();

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
        JLineupWebProperties that = (JLineupWebProperties) o;
        return cleanupProfile == that.cleanupProfile && maxParallelJobs == that.maxParallelJobs && maxThreadsPerJob == that.maxThreadsPerJob && Objects.equals(workingDirectory, that.workingDirectory) && Objects.equals(screenshotsDirectory, that.screenshotsDirectory) && Objects.equals(reportDirectory, that.reportDirectory) && Objects.equals(chromeLaunchParameters, that.chromeLaunchParameters) && Objects.equals(firefoxLaunchParameters, that.firefoxLaunchParameters) && Objects.equals(installedBrowsers, that.installedBrowsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workingDirectory, screenshotsDirectory, reportDirectory, cleanupProfile, maxParallelJobs, maxThreadsPerJob, chromeLaunchParameters, firefoxLaunchParameters, installedBrowsers);
    }

    @Override
    public String toString() {
        return "JLineupWebProperties{" +
                "workingDirectory='" + workingDirectory + '\'' +
                ", screenshotsDirectory='" + screenshotsDirectory + '\'' +
                ", reportDirectory='" + reportDirectory + '\'' +
                ", cleanupProfile=" + cleanupProfile +
                ", maxParallelJobs=" + maxParallelJobs +
                ", maxThreadsPerJob=" + maxThreadsPerJob +
                ", chromeLaunchParameters=" + chromeLaunchParameters +
                ", firefoxLaunchParameters=" + firefoxLaunchParameters +
                ", installedBrowsers=" + installedBrowsers +
                '}';
    }

    public JLineupWebLambdaProperties getLambda() {
        return lambda;
    }

    public void setLambda(JLineupWebLambdaProperties lambda) {
        this.lambda = lambda;
    }
}
