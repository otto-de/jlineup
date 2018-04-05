package de.otto.jlineup.web;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JLineupRunnerFactory {

    private final JLineupWebProperties properties;

    @Autowired
    public JLineupRunnerFactory(JLineupWebProperties properties) {
        this.properties = properties;
    }

    public JLineupRunner createBeforeRun(String id, JobConfig jobConfig) {
        return createRun(id, jobConfig, Step.before);
    }

    public JLineupRunner createAfterRun(String id, JobConfig jobConfig) {
        return createRun(id, jobConfig, Step.after);
    }

    private JLineupRunner createRun(String id, JobConfig jobConfig, Step step) {
        return new JLineupRunner(jobConfig, RunStepConfig.jLineupRunConfigurationBuilder()
                .withWorkingDirectory(properties.getWorkingDirectory().replace("{id}", id))
                .withScreenshotsDirectory(properties.getScreenshotsDirectory().replace("{id}", id))
                .withReportDirectory(properties.getReportDirectory().replace("{id}", id))
                .withStep(step)
                .build());
    }

}
