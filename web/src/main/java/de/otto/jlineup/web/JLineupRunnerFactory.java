package de.otto.jlineup.web;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static de.otto.jlineup.browser.BrowserUtils.RANDOM_FOLDER_PLACEHOLDER;
import static de.otto.jlineup.config.JobConfig.DEFAULT_REPORT_FORMAT;

@Component
public class JLineupRunnerFactory {

    private final JLineupWebProperties properties;

    @Autowired
    public JLineupRunnerFactory(JLineupWebProperties properties) {
        this.properties = properties;
    }

    public JLineupRunner createBeforeRun(String id, JobConfig jobConfig) throws Exception {
        return createRun(id, jobConfig, RunStep.before);
    }

    public JLineupRunner createAfterRun(String id, JobConfig jobConfig) throws Exception {
        return createRun(id, jobConfig, RunStep.after);
    }

    private JLineupRunner createRun(String id, JobConfig jobConfig, RunStep step) throws Exception {

        JobConfig webJobConfig = sanitizeJobConfig(jobConfig);

        //Replace id placeholder in approval link with runId
        if (webJobConfig.approvalLink != null && webJobConfig.approvalLink.contains("{id}")) {
            webJobConfig = JobConfig.copyOfBuilder(webJobConfig)
                    .withApprovalLink(webJobConfig.approvalLink.replace("{id}", id))
                    .build();
        }

        return new JLineupRunner(webJobConfig, RunStepConfig.runStepConfigBuilder()
                .withWorkingDirectory(properties.getWorkingDirectory())
                .withScreenshotsDirectory(properties.getScreenshotsDirectory().replace("{id}", id))
                .withReportDirectory(properties.getReportDirectory().replace("{id}", id))
                .withChromeParameters(properties.getChromeLaunchParameters().stream()
                        .map(param -> param.replace("{id}", id))
                        .map(param -> param.startsWith("--user-data-dir") ? param + "/" + RANDOM_FOLDER_PLACEHOLDER : param)
                        .collect(Collectors.toList()))
                .withFirefoxParameters(properties.getFirefoxLaunchParameters().stream()
                        .map(param -> param.replace("{id}", id))
                        .map(param -> param.startsWith("-profile ") || param.startsWith("-P ") ? param + "/" + RANDOM_FOLDER_PLACEHOLDER : param)
                        .collect(Collectors.toList()))
                .withCleanupProfile(properties.isCleanupProfile())
                .withStep(step)
                .build());
    }

    JobConfig sanitizeJobConfig(final JobConfig jobConfig) throws BrowserNotInstalledException, IllegalArgumentException {

        if (!properties.getInstalledBrowsers().contains(jobConfig.browser)) {
            throw new BrowserNotInstalledException(jobConfig.browser);
        }

        if (!properties.getAllowedUrlPrefixes().isEmpty()) {
            for (UrlConfig urlConfig : jobConfig.urls.values()) {
                if (properties.getAllowedUrlPrefixes().stream().noneMatch(urlConfig.getUrl()::startsWith)) {
                    throw new IllegalArgumentException("URL " + urlConfig.url + " is not allowed. Allowed prefixes are: " + properties.getAllowedUrlPrefixes());
                }
            }
        }

        return JobConfig.copyOfBuilder(jobConfig)
                .withThreads(calculateNumberOfThreads(jobConfig))
                .withDebug(false)
                .withLogToFile(false)
                .withReportFormat(DEFAULT_REPORT_FORMAT)
                .build();
    }

    private int calculateNumberOfThreads(JobConfig jobConfig) {
        return jobConfig.threads == 0 ? properties.getMaxThreadsPerJob() : Math.min(jobConfig.threads, properties.getMaxThreadsPerJob());
    }

}
