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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    /**
     * Retries the 'after' step for a run that previously failed (ERROR, DEAD) or
     * finished with differences. Resets the run state to BEFORE_DONE and then
     * delegates to {@link #startAfterRun(String)}.
     */
    public synchronized JLineupRunStatus retryAfterRun(String runId) throws Exception {

        Optional<JLineupRunStatus> run = getRun(runId);
        if (run.isEmpty()) {
            throw new RunNotFoundException(runId);
        }

        JLineupRunStatus currentStatus = run.get();
        if (!currentStatus.getState().isRetryableForAfter()) {
            throw new InvalidRunStateException(currentStatus.getId(), currentStatus.getState(), State.BEFORE_DONE);
        }

        LOG.info("Retrying 'after' step for run {} (previous state: {})", runId, currentStatus.getState());

        // Reset state to BEFORE_DONE so startAfterRun() accepts it
        changeState(runId, State.BEFORE_DONE);

        return startAfterRun(runId);
    }

    /**
     * Creates a new run that reuses the 'before' screenshots from an existing
     * (source) run. The source run's report directory is copied to a fresh run,
     * then the after+compare step is executed against the live website.
     *
     * @return the status of the newly created run (already in after-pending state)
     */
    public synchronized JLineupRunStatus rerunAfterFromRun(String sourceRunId) throws Exception {

        Optional<JLineupRunStatus> sourceOpt = getRun(sourceRunId);
        if (sourceOpt.isEmpty()) {
            throw new RunNotFoundException(sourceRunId);
        }

        JLineupRunStatus sourceStatus = sourceOpt.get();
        if (!sourceStatus.getState().isRerunnableForAfter()) {
            throw new InvalidRunStateException(sourceStatus.getId(), sourceStatus.getState(), State.BEFORE_DONE);
        }

        String newRunId = UUID.randomUUID().toString();

        // Copy the source run's report directory (before screenshots + files.json) to the new run's directory
        copyReportDirectory(sourceRunId, newRunId);

        LOG.info("Creating rerun of 'after' step from source run {} as new run {}", sourceRunId, newRunId);

        // Register the new run in BEFORE_DONE state so startAfterRun() accepts it
        JLineupRunStatus newStatus = runStatusBuilder()
                .withId(newRunId)
                .withJobConfig(sourceStatus.getJobConfig())
                .withState(State.BEFORE_DONE)
                .withStartTime(sourceStatus.getStartTime())
                .withPauseTime(Instant.now())
                .withReports(JLineupRunStatus.Reports.reportsBuilder()
                        .withLogUrl("/reports/report-" + newRunId + "/jlineup.log")
                        .withHtmlUrl("/reports/report-" + newRunId + "/report_before.html")
                        .build())
                .build();

        runs.put(newRunId, newStatus);
        runPersistenceService.persistRuns(runs);

        return startAfterRun(newRunId);
    }

    /**
     * Copies the source run's report directory (including subdirectories with
     * before screenshots and files.json) to a new directory for the rerun.
     * Existing after screenshots will simply be overwritten by the new after step.
     */
    private void copyReportDirectory(String sourceRunId, String newRunId) throws IOException {
        String sourceDir = jLineupWebProperties.getScreenshotsDirectory().replace("{id}", sourceRunId);
        String targetDir = jLineupWebProperties.getScreenshotsDirectory().replace("{id}", newRunId);

        Path sourcePath = Path.of(jLineupWebProperties.getWorkingDirectory(), sourceDir);
        Path targetPath = Path.of(jLineupWebProperties.getWorkingDirectory(), targetDir);

        if (!Files.isDirectory(sourcePath)) {
            throw new IOException("Source report directory does not exist: " + sourcePath);
        }

        // Walk the entire source directory tree and copy everything
        try (var walker = Files.walk(sourcePath)) {
            walker.forEach(source -> {
                Path target = targetPath.resolve(sourcePath.relativize(source));
                try {
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        LOG.info("Copied report directory from {} to {}", sourcePath, targetPath);
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
