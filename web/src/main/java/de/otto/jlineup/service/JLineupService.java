package de.otto.jlineup.service;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static de.otto.jlineup.web.JLineupRunStatus.copyOfRunStatusBuilder;
import static de.otto.jlineup.web.JLineupRunStatus.jLineupRunStatusBuilder;

@Service
public class JLineupService {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupService.class);

    private final ConcurrentHashMap<String, JLineupRunStatus> runs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final AtomicInteger runningJobs = new AtomicInteger();

    private final JLineupRunnerFactory jLineupRunnerFactory;

    @Autowired
    public JLineupService(JLineupRunnerFactory jLineupRunnerFactory) {
        this.jLineupRunnerFactory = jLineupRunnerFactory;
    }

    public JLineupRunStatus startBeforeRun(JobConfig jobConfig) {
        String runId = UUID.randomUUID().toString();
        final JLineupRunStatus beforeStatus = jLineupRunStatusBuilder()
                .withId(runId)
                .withConfig(jobConfig)
                .withState(State.BEFORE_RUNNING)
                .withStartTime(Instant.now())
                .build();

        runs.put(runId, beforeStatus);
        final JLineupRunner jLineupRunner = jLineupRunnerFactory.createBeforeRun(runId, jobConfig);
        runningJobs.incrementAndGet();
        executorService.submit( () -> {
            try {
                boolean runSucceeded = jLineupRunner.run();
                if (runSucceeded) {
                    changeState(runId, State.BEFORE_DONE);
                } else {
                    changeState(runId, State.ERROR);
                }
            } catch (Exception e) {
                LOG.error("Error in before runStep.", e);
                changeState(runId, State.ERROR);
            } finally {
                runningJobs.decrementAndGet();
            }
        });
        return beforeStatus;
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

        JLineupRunStatus afterStatus = changeState(id, State.AFTER_RUNNING);

        final JLineupRunner jLineupRunner = jLineupRunnerFactory.createAfterRun(id, beforeStatus.getJobConfig());
        runningJobs.incrementAndGet();
        executorService.submit(() -> {
            try {
                boolean runSucceeded = jLineupRunner.run();
                if (runSucceeded) {
                    changeState(id, State.FINISHED);
                } else {
                    changeState(id, State.ERROR);
                }
            } catch (Exception e) {
                LOG.error("Error in after runStep.", e);
                changeState(id, State.ERROR);
            } finally {
                runningJobs.decrementAndGet();
            }
        });
        return afterStatus;
    }

    private JLineupRunStatus changeState(String runId, State state) {
        JLineupRunStatus runStatus = runs.get(runId);
        JLineupRunStatus.Builder runStatusBuilder = copyOfRunStatusBuilder(runStatus).withState(state);

        if (state == State.FINISHED) {
            runStatusBuilder.withEndTime(Instant.now());
        }

        return runs.put(runId, runStatusBuilder.build());
    }

    public Optional<JLineupRunStatus> getRun(String id) {
        return Optional.ofNullable(runs.get(id));
    }

    public int getRunningJobsCount() {
        return runningJobs.get();
    }

}
