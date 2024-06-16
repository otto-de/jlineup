package de.otto.jlineup.service;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.GlobalOption;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.State;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static de.otto.jlineup.web.JLineupRunStatus.copyOfRunStatusBuilder;
import static de.otto.jlineup.web.JLineupRunStatus.runStatusBuilder;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Service
public class JLineupService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final ConcurrentHashMap<String, JLineupRunStatus> runs = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final AtomicInteger runningJobs = new AtomicInteger();

    private final JLineupRunnerFactory jLineupRunnerFactory;
    private final JLineupWebProperties jLineupWebProperties;
    private final RunPersistenceService runPersistenceService;

    @Autowired
    public JLineupService(JLineupRunnerFactory jLineupRunnerFactory,
                          JLineupWebProperties jLineupWebProperties,
                          RunPersistenceService runPersistenceService) {
        this.jLineupRunnerFactory = jLineupRunnerFactory;
        this.jLineupWebProperties = jLineupWebProperties;
        this.runPersistenceService = runPersistenceService;
        this.executorService = Executors.newFixedThreadPool(jLineupWebProperties.getMaxParallelJobs());
        this.runs.putAll(runPersistenceService.readRuns());

        GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME, jLineupWebProperties.getLambda().getFunctionName());
    }

    public synchronized JLineupRunStatus startBeforeRun(JobConfig jobConfig) throws Exception {
        String runId = UUID.randomUUID().toString();
        final JLineupRunStatus beforeStatus = runStatusBuilder()
                .withId(runId)
                .withJobConfig(jobConfig)
                .withState(State.BEFORE_PENDING)
                .withStartTime(Instant.now())
                .build();

        final JLineupRunner jLineupRunner = jLineupRunnerFactory.createBeforeRun(runId, jobConfig);
        runs.put(runId, beforeStatus);
        runPersistenceService.persistRuns(runs);

        runningJobs.incrementAndGet();

        CompletableFuture<State> state = supplyAsync(
                () -> {
                    changeState(runId, State.BEFORE_RUNNING);
                    jLineupRunner.run();
                    return State.BEFORE_DONE;
                }, executorService)
                .exceptionally(ex -> {
                    LOG.error("Error in before runStep.", ex);
                    return State.ERROR;
                })
                .thenApply(st -> {
                    changeState(runId, st);
                    runningJobs.decrementAndGet();
                    return st;
                });

        return JLineupRunStatus.copyOfRunStatusBuilder(beforeStatus).withCurrentJobStepFuture(state).build();
    }

    public synchronized JLineupRunStatus startAfterRun(String runId) throws Exception {

        Optional<JLineupRunStatus> run = getRun(runId);
        if (run.isEmpty()) {
            throw new RunNotFoundException(runId);
        }

        JLineupRunStatus beforeStatus = run.get();
        if (beforeStatus.getState() != State.BEFORE_DONE) {
            throw new InvalidRunStateException(beforeStatus.getId(), beforeStatus.getState(), State.BEFORE_DONE);
        }

        final JLineupRunner jLineupRunner = jLineupRunnerFactory.createAfterRun(runId, beforeStatus.getJobConfig());
        JLineupRunStatus afterStatus = changeState(runId, State.AFTER_PENDING);
        runningJobs.incrementAndGet();

        CompletableFuture<State> state = supplyAsync(
                () -> {
                    changeState(runId, State.AFTER_RUNNING);
                    boolean runSucceeded = jLineupRunner.run();
                    if (runSucceeded) {
                        return State.FINISHED_WITHOUT_DIFFERENCES;
                    } else {
                        return State.FINISHED_WITH_DIFFERENCES;
                    }
                }, executorService)
                .exceptionally(ex -> {
                    LOG.error("Error in after runStep.", ex);
                    return State.ERROR;
                })
                .thenApply(st -> {
                    changeState(runId, st);
                    runningJobs.decrementAndGet();
                    return st;
                });

        return JLineupRunStatus.copyOfRunStatusBuilder(afterStatus).withCurrentJobStepFuture(state).build();
    }

    private JLineupRunStatus changeState(String runId, State state) {
        JLineupRunStatus runStatus = runs.get(runId);
        JLineupRunStatus.Builder runStatusBuilder = copyOfRunStatusBuilder(runStatus).withState(state);

        if (state == State.BEFORE_RUNNING) {
            runStatusBuilder.withReports(JLineupRunStatus.Reports.reportsBuilder()
                    .withLogUrl("/reports/report-" + runId + "/jlineup.log")
                    .build());
        }

        if (state == State.BEFORE_DONE) {
            runStatusBuilder.withPauseTime(Instant.now());
        }

        if (state == State.AFTER_RUNNING) {
            runStatusBuilder.withResumeTime(Instant.now());
        }

        if (state == State.ERROR || state == State.DEAD) {
            runStatusBuilder.withEndTime(Instant.now());
        }

        if (state == State.FINISHED_WITH_DIFFERENCES
                || state == State.FINISHED_WITHOUT_DIFFERENCES) {
            runStatusBuilder.withEndTime(Instant.now());
            runStatusBuilder.withReports(JLineupRunStatus.Reports.reportsBuilder()
                    .withHtmlUrl("/reports/report-" + runId + "/report.html")
                    .withJsonUrl("/reports/report-" + runId + "/report.json")
                    .withLogUrl("/reports/report-" + runId + "/jlineup.log")
                    .build());
        }

        JLineupRunStatus newStatus = runStatusBuilder.build();
        runs.put(runId, newStatus);
        runPersistenceService.persistRuns(runs);
        return newStatus;
    }



    public Optional<JLineupRunStatus> getRun(String id) {
        return Optional.ofNullable(runs.get(id));
    }

    public List<JLineupRunStatus> getRunStatus() {
        return ImmutableList.copyOf(this.runs.values());
    }

}
