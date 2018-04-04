package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.otto.jlineup.web.JLineupRunStatus.copyOfRunStatusBuilder;
import static de.otto.jlineup.web.JLineupRunStatus.jLineupRunStatusBuilder;

@Service
public class JLineupService {

    private final static ConcurrentHashMap<String, JLineupRunStatus> runs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final JLineupSpawner jLineupSpawner;

    private final JLineupWebProperties properties;

    @Autowired
    public JLineupService(JLineupSpawner jLineupSpawner, JLineupWebProperties properties) {
        this.jLineupSpawner = jLineupSpawner;
        this.properties = properties;
    }

    JLineupRunStatus startBeforeRun(Config config) {
        String id = String.valueOf(UUID.randomUUID());
        final JLineupRunStatus jLineupRunStatus = jLineupRunStatusBuilder().withId(id).withConfig(config).withState(State.BEFORE_RUNNING).withStartTime(Instant.now()).build();
        runs.put(id, jLineupRunStatus);
        final JLineup jLineup = jLineupSpawner.createBeforeRun(id, config);
        executorService.submit( () -> {
            try {
                int returnCode = jLineup.run();
                if (returnCode == 0) {
                    runs.put(id, copyOfRunStatusBuilder(jLineupRunStatus).withState(State.BEFORE_DONE).build());
                } else {
                    runs.put(id, copyOfRunStatusBuilder(jLineupRunStatus).withState(State.ERROR).build());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runs.put(id, copyOfRunStatusBuilder(jLineupRunStatus).withState(State.ERROR).build());
            }
        });
        return jLineupRunStatus;
    }

    JLineupRunStatus startAfterRun(String id) {
        Optional<JLineupRunStatus> run = getRun(id);
        if (!run.isPresent()) {
            throw new JLineupWebException(HttpServletResponse.SC_NOT_FOUND, "Run not found, cannot start after step");
        }

        JLineupRunStatus beforeStatus = run.get();
        if (beforeStatus.getState() != State.BEFORE_DONE) {
            throw new JLineupWebException(HttpServletResponse.SC_PRECONDITION_FAILED, "Cannot start after run for job that is not in state BEFORE_DONE. Desired job is in state " + beforeStatus.getState());
        }

        JLineupRunStatus afterStatus = copyOfRunStatusBuilder(beforeStatus).withState(State.AFTER_RUNNING).build();
        runs.put(id, afterStatus);
        final JLineup jLineup = jLineupSpawner.createAfterRun(id, beforeStatus.getConfig());
        executorService.submit( () -> {
            try {
                int returnCode = jLineup.run();
                if (returnCode == 0) {
                    runs.put(id, copyOfRunStatusBuilder(afterStatus).withState(State.FINISHED).withEndTime(Instant.now()).build());
                } else {
                    runs.put(id, copyOfRunStatusBuilder(afterStatus).withState(State.ERROR).build());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runs.put(id, copyOfRunStatusBuilder(afterStatus).withState(State.ERROR).build());
            }
        });
        return afterStatus;
    }

    public Optional<JLineupRunStatus> getRun(String id) {
        return Optional.ofNullable(runs.get(id));
    }

}
