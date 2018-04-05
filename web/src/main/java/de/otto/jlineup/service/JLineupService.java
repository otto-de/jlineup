package de.otto.jlineup.service;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupSpawner;
import de.otto.jlineup.web.State;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public JLineupRunStatus startBeforeRun(JobConfig jobConfig) {
        String id = UUID.randomUUID().toString();
        final JLineupRunStatus jLineupRunStatus = jLineupRunStatusBuilder()
                .withId(id)
                .withConfig(jobConfig)
                .withState(State.BEFORE_RUNNING)
                .withStartTime(Instant.now())
                .build();

        runs.put(id, jLineupRunStatus);
        final JLineupRunner jLineupRunner = jLineupSpawner.createBeforeRun(id, jobConfig);
        executorService.submit( () -> {
            try {
                boolean runSucceeded = jLineupRunner.run();
                if (runSucceeded) {
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

    public JLineupRunStatus startAfterRun(String id) throws RunNotFoundException, InvalidRunStateException {
        Optional<JLineupRunStatus> run = getRun(id);
        if (!run.isPresent()) {
            throw new RunNotFoundException(id);
        }

        JLineupRunStatus beforeStatus = run.get();
        if (beforeStatus.getState() != State.BEFORE_DONE) {
            throw new InvalidRunStateException(beforeStatus.getId(), beforeStatus.getState(), State.BEFORE_DONE);
        }

        JLineupRunStatus afterStatus = copyOfRunStatusBuilder(beforeStatus).withState(State.AFTER_RUNNING).build();
        runs.put(id, afterStatus);
        final JLineupRunner jLineupRunner = jLineupSpawner.createAfterRun(id, beforeStatus.getJobConfig());
        executorService.submit( () -> {
            try {
                boolean runSucceeded = jLineupRunner.run();
                if (runSucceeded) {
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
