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
import java.util.Map;
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
        if (jLineupWebProperties.getLambda().getFunctionNameBase() != null) {
            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_BASE, jLineupWebProperties.getLambda().getFunctionNameBase());
        }
        if (jLineupWebProperties.getLambda().getFunctionNameChromeHeadless() != null) {
            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS, jLineupWebProperties.getLambda().getFunctionNameChromeHeadless());
        }
        if (jLineupWebProperties.getLambda().getFunctionNameFirefoxHeadless() != null) {
            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_FIREFOX_HEADLESS, jLineupWebProperties.getLambda().getFunctionNameFirefoxHeadless());
        }
        if (jLineupWebProperties.getLambda().getFunctionNameWebkitHeadless() != null) {
            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_FUNCTION_NAME_WEBKIT_HEADLESS, jLineupWebProperties.getLambda().getFunctionNameWebkitHeadless());
        }
        if (jLineupWebProperties.getLambda().getAwsProfile() != null) {
            LOG.info("Setting AWS profile from Spring properties: '{}'", jLineupWebProperties.getLambda().getAwsProfile());
            GlobalOptions.setOption(GlobalOption.JLINEUP_LAMBDA_AWS_PROFILE, jLineupWebProperties.getLambda().getAwsProfile());
        } else {
            LOG.info("No AWS profile configured in jlineup.lambda.aws-profile – using GlobalOptions default: '{}'",
                    GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_AWS_PROFILE));
        }
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
            runStatusBuilder
                    .withReports(JLineupRunStatus.Reports.reportsBuilder()
                            .withLogUrl("/reports/report-" + runId + "/jlineup.log")
                            .withHtmlUrl("/reports/report-" + runId + "/report_before.html")
                            .build())
                    .withPauseTime(Instant.now())
                    .build();
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
        JLineupRunStatus status = runs.get(id);
        if (status == null) {
            // Run might have been created by another instance sharing the same filesystem.
            // Reload from disk and try again.
            reloadRunsFromDisk();
            status = runs.get(id);
        }
        return Optional.ofNullable(status);
    }

    public List<JLineupRunStatus> getRunStatus() {
        reloadRunsFromDisk();
        return ImmutableList.copyOf(this.runs.values());
    }

    /**
     * Re-reads runs.json from disk and merges any runs not yet known to this instance
     * into the in-memory map. This covers the deployment overlap case where another
     * instance wrote new runs to the shared file.
     * Existing in-memory entries are NOT overwritten, because this instance may hold
     * more recent state (e.g. a running future) that the file doesn't reflect.
     */
    private void reloadRunsFromDisk() {
        Map<String, JLineupRunStatus> diskRuns = runPersistenceService.readRuns();
        diskRuns.forEach(runs::putIfAbsent);
    }

}
