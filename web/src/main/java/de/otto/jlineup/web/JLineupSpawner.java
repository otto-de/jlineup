package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JLineupSpawner {

    JLineupWebProperties properties;

    @Autowired
    public JLineupSpawner(JLineupWebProperties properties) {
        this.properties = properties;
    }

    public JLineup createBeforeRun(String id, Config config) {
        return createRun(id, config, Step.before);
    }

    public JLineup createAfterRun(String id, Config config) {
        return createRun(id, config, Step.after);
    }

    private JLineup createRun(String id, Config config, Step step) {
        return new JLineup(config, JLineupRunConfiguration.jLineupRunConfigurationBuilder()
                .withWorkingDirectory(properties.getWorkingDirectory().replace("{id}", id))
                .withScreenshotsDirectory(properties.getScreenshotsDirectory().replace("{id}", id))
                .withReportDirectory(properties.getReportDirectory().replace("{id}", id))
                .withStep(step)
                .build());
    }

}
