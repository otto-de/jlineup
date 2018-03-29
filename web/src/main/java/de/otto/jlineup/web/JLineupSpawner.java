package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Step;
import org.springframework.stereotype.Component;

@Component
public class JLineupSpawner {

    public JLineup createBeforeRun(String id, Config config) {
        return createRun(id, config, Step.before);
    }

    public JLineup createAfterRun(String id, Config config) {
        return createRun(id, config, Step.after);
    }

    private JLineup createRun(String id, Config config, Step step) {
        return new JLineup(config, JLineupRunConfiguration.jLineupRunConfigurationBuilder().withWorkingDirectory("/tmp/jlineup-run-" + id).withStep(step).build());
    }

}
