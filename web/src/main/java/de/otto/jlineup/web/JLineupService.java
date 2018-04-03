package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.*;

@Component
public class JLineupService {

    private final static ConcurrentHashMap<String, JLineupRun> runs = new ConcurrentHashMap<>();

    private final JLineupSpawner jLineupSpawner;

    @Autowired
    public JLineupService(JLineupSpawner jLineupSpawner) {
        this.jLineupSpawner = jLineupSpawner;
    }

    JLineupRun startBeforeRun(String config) throws IOException {
        Config parsedConfig = Config.parse(config);
        String id = String.valueOf(UUID.randomUUID());
        JLineupRun jLineupRun = JLineupRun.jLineupRunBuilder().withId(id).withConfig(parsedConfig).withState(State.BEFORE_RUNNING).build();
        runs.put(id, jLineupRun);
        JLineup jLineup = jLineupSpawner.createBeforeRun(id, parsedConfig);
        jLineup.run();
        return jLineupRun;
    }

    JLineupRun startAfterRun(String id) throws IOException {
        Optional<JLineupRun> run = getRun(id);
        if (!run.isPresent()) {
            throw new JLineupWebException(HttpServletResponse.SC_NOT_FOUND, "Run not found, cannot start after step");
        }
        JLineupRun runInAfterState = JLineupRun.copyOfBuilder(run.get()).withState(State.AFTER_RUNNING).build();
        runs.put(id, runInAfterState);
        JLineup jLineup = jLineupSpawner.createAfterRun(id, run.get().getConfig());
        jLineup.run();
        return runInAfterState;
    }

    public Optional<JLineupRun> getRun(String id) {
        return Optional.ofNullable(runs.get(id));
    }
}
