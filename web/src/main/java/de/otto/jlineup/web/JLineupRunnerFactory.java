package de.otto.jlineup.web;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.otto.jlineup.config.JobConfig.DEFAULT_REPORT_FORMAT;

@Component
public class JLineupRunnerFactory {

    private final JLineupWebProperties properties;

    @Autowired
    public JLineupRunnerFactory(JLineupWebProperties properties) {
        this.properties = properties;
    }

    public JLineupRunner createBeforeRun(String id, JobConfig jobConfig) throws BrowserNotInstalledException {
        return createRun(id, jobConfig, Step.before);
    }

    public JLineupRunner createAfterRun(String id, JobConfig jobConfig) throws BrowserNotInstalledException {
        return createRun(id, jobConfig, Step.after);
    }

    private JLineupRunner createRun(String id, JobConfig jobConfig, Step step) throws BrowserNotInstalledException {

        JobConfig webJobConfig = sanitizeJobConfig(jobConfig);

        return new JLineupRunner(webJobConfig, RunStepConfig.jLineupRunConfigurationBuilder()
                .withWorkingDirectory(properties.getWorkingDirectory())
                .withScreenshotsDirectory(properties.getScreenshotsDirectory().replace("{id}", id))
                .withReportDirectory(properties.getReportDirectory().replace("{id}", id))
                .withStep(step)
                .build());
    }

    JobConfig sanitizeJobConfig(JobConfig jobConfig) throws BrowserNotInstalledException {

        if (!properties.getInstalledBrowsers().contains(jobConfig.browser)) {
            throw new BrowserNotInstalledException(jobConfig.browser);
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
