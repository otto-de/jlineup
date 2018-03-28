package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Step;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class JLineupService {

    public JLineupService() {

    }

    String startRun(String config) throws IOException {
        JLineup jLineup = new JLineup(Config.exampleConfig(), JLineupRunConfiguration.jLineupRunConfigurationBuilder().withStep(Step.before).build());
        jLineup.run();
        return String.valueOf(UUID.randomUUID());
    }
}
