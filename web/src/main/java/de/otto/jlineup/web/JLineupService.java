package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.JLineupRunConfiguration;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JLineupService {

    private final static ConcurrentHashMap<String, Config> runs = new ConcurrentHashMap<>();

    private final JLineupSpawner jLineupSpawner;

    @Autowired
    public JLineupService(JLineupSpawner jLineupSpawner) {
        this.jLineupSpawner = jLineupSpawner;
    }

    String startBeforeRun(String config) throws IOException {
        Config parsedConfig = Config.parse(config);
        String id = String.valueOf(UUID.randomUUID());
        runs.put(id, parsedConfig);
        JLineup jLineup = jLineupSpawner.createBeforeRun(id, parsedConfig);
        jLineup.run();
        return id;
    }

    void startAfterRun(String id) throws IOException {
        JLineup jLineup = jLineupSpawner.createAfterRun(id, runs.get(id));
        jLineup.run();
    }

}
