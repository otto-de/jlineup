package de.otto.jlineup.service;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.State;
import de.otto.jlineup.web.configuration.JLineupWebLambdaProperties;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JLineupServiceTest {

    private JLineupService testee;
    private JLineupRunnerFactory jLineupRunnerFactory;
    private JLineupRunner jLineupRunnerBefore;
    private JLineupRunner jLineupRunnerAfter;
    private JLineupWebProperties jLineupWebProperties;
    private RunPersistenceService runPersistenceService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        jLineupRunnerFactory = mock(JLineupRunnerFactory.class);
        jLineupRunnerBefore = mock(JLineupRunner.class);
        jLineupRunnerAfter = mock(JLineupRunner.class);
        jLineupWebProperties = mock(JLineupWebProperties.class);
        JLineupWebLambdaProperties jLineupWebLambdaProperties = mock(JLineupWebLambdaProperties.class);
        runPersistenceService = mock(RunPersistenceService.class);

        when(jLineupRunnerFactory.createBeforeRun(any(), any())).thenReturn(jLineupRunnerBefore);
        when(jLineupRunnerFactory.createAfterRun(any(), any())).thenReturn(jLineupRunnerAfter);
        when(jLineupWebProperties.getMaxParallelJobs()).thenReturn(1);
        when(jLineupWebProperties.getLambda()).thenReturn(jLineupWebLambdaProperties);
        when(jLineupWebProperties.getLambda().getFunctionName()).thenReturn("jlineup-lambda-test");
        when(jLineupWebProperties.getWorkingDirectory()).thenReturn(tempDir.toString() + "/");
        when(jLineupWebProperties.getScreenshotsDirectory()).thenReturn("report-{id}");
        when(jLineupWebProperties.getReportDirectory()).thenReturn("report-{id}");
        testee = new JLineupService(jLineupRunnerFactory, jLineupWebProperties, runPersistenceService);
    }

    @Test
    void shouldStartBeforeRun() throws Exception {

        //given
        JobConfig jobConfig = JobConfig.exampleConfig();

        //when
        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();


        //then
        verify(jLineupRunnerFactory).createBeforeRun(beforeStatus.getId(), jobConfig);
        verify(jLineupRunnerBefore).run();
    }

    @Test
    void shouldStartAfterRun() throws Exception {

        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);

        //when
        beforeStatus.getCurrentJobStepFuture().get().get();
        JLineupRunStatus afterStatus = testee.startAfterRun(beforeStatus.getId());
        afterStatus.getCurrentJobStepFuture().get().get();

        //then
        verify(jLineupRunnerFactory).createAfterRun(beforeStatus.getId(), jobConfig);
        verify(jLineupRunnerAfter).run();
    }

    @Test
    void shouldContainReportPathInResult() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);

        //when
        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);

        when(jLineupRunnerAfter.run()).thenReturn(true);
        beforeStatus.getCurrentJobStepFuture().get().get();

        //then

        Optional<JLineupRunStatus> currentStatus = testee.getRun(beforeStatus.getId());
        assertThat(currentStatus.get().getState(), is(State.BEFORE_DONE));
        assertThat(currentStatus.get().getReports().getHtmlUrl(), is("/reports/report-" + beforeStatus.getId() + "/report_before.html"));

        //when
        JLineupRunStatus afterStatus = testee.startAfterRun(beforeStatus.getId());
        afterStatus.getCurrentJobStepFuture().get().get();

        currentStatus = testee.getRun(beforeStatus.getId());

        //then
        assertTrue(currentStatus.isPresent());
        assertThat(currentStatus.get().getState(), is(State.FINISHED_WITHOUT_DIFFERENCES));
        assertThat(currentStatus.get().getReports().getHtmlUrl(), is("/reports/report-" + beforeStatus.getId() + "/report.html"));
        assertThat(currentStatus.get().getReports().getJsonUrl(), is("/reports/report-" + beforeStatus.getId() + "/report.json"));

    }

    @Test
    void shouldRetryAfterRunWhenInErrorState() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        when(jLineupRunnerAfter.run())
                .thenThrow(new RuntimeException("Simulated failure"))
                .thenReturn(true);

        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();

        JLineupRunStatus afterStatus = testee.startAfterRun(beforeStatus.getId());
        afterStatus.getCurrentJobStepFuture().get().get();

        // Verify the run is now in ERROR state
        assertThat(testee.getRun(beforeStatus.getId()).get().getState(), is(State.ERROR));

        //when — retry the after run
        JLineupRunStatus retryStatus = testee.retryAfterRun(beforeStatus.getId());
        retryStatus.getCurrentJobStepFuture().get().get();

        //then
        assertThat(testee.getRun(beforeStatus.getId()).get().getState(), is(State.FINISHED_WITHOUT_DIFFERENCES));
        verify(jLineupRunnerFactory, times(2)).createAfterRun(beforeStatus.getId(), jobConfig);
        verify(jLineupRunnerAfter, times(2)).run();
    }

    @Test
    void shouldRetryAfterRunWhenFinishedWithDifferences() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        when(jLineupRunnerAfter.run())
                .thenReturn(false)  // first run: differences detected
                .thenReturn(true);  // retry: no differences

        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();

        JLineupRunStatus afterStatus = testee.startAfterRun(beforeStatus.getId());
        afterStatus.getCurrentJobStepFuture().get().get();

        assertThat(testee.getRun(beforeStatus.getId()).get().getState(), is(State.FINISHED_WITH_DIFFERENCES));

        //when
        JLineupRunStatus retryStatus = testee.retryAfterRun(beforeStatus.getId());
        retryStatus.getCurrentJobStepFuture().get().get();

        //then
        assertThat(testee.getRun(beforeStatus.getId()).get().getState(), is(State.FINISHED_WITHOUT_DIFFERENCES));
    }

    @Test
    void shouldRejectRetryWhenRunIsNotRetryable() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();

        // Run is in BEFORE_DONE — not retryable
        assertThat(testee.getRun(beforeStatus.getId()).get().getState(), is(State.BEFORE_DONE));

        //when/then
        assertThrows(InvalidRunStateException.class, () -> testee.retryAfterRun(beforeStatus.getId()));
    }

    @Test
    void shouldRejectRetryWhenRunNotFound() {
        assertThrows(RunNotFoundException.class, () -> testee.retryAfterRun("nonexistent-id"));
    }

    @Test
    void shouldRerunAfterFromExistingRun() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        when(jLineupRunnerAfter.run()).thenReturn(true);

        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();

        // Simulate the before screenshots directory (with context hash subdirectory)
        Path sourceDir = tempDir.resolve("report-" + beforeStatus.getId());
        Files.createDirectories(sourceDir.resolve("2d6834e2"));
        Files.writeString(sourceDir.resolve("files.json"), "{}");
        Files.writeString(sourceDir.resolve("2d6834e2").resolve("screenshot_before.png"), "fake-png-data");
        Files.writeString(sourceDir.resolve("2d6834e2").resolve("screenshot_after.png"), "stale-after-data");

        //when
        JLineupRunStatus rerunStatus = testee.rerunAfterFromRun(beforeStatus.getId());
        rerunStatus.getCurrentJobStepFuture().get().get();

        //then — new run ID, different from source
        assertThat(rerunStatus.getId(), is(not(beforeStatus.getId())));

        // Verify the new run's directory was created with copied files
        Path newDir = tempDir.resolve("report-" + rerunStatus.getId());
        assertTrue(Files.exists(newDir.resolve("files.json")));
        assertTrue(Files.exists(newDir.resolve("2d6834e2").resolve("screenshot_before.png")));
        // Stale after screenshot should have been cleaned
        assertFalse(Files.exists(newDir.resolve("2d6834e2").resolve("screenshot_after.png")));

        // Verify after runner was created for the new run ID
        verify(jLineupRunnerFactory).createAfterRun(rerunStatus.getId(), jobConfig);
    }

    @Test
    void shouldRerunAfterFromFinishedRun() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        when(jLineupRunnerAfter.run()).thenReturn(false).thenReturn(true);

        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();

        // Create source dir
        Path sourceDir = tempDir.resolve("report-" + beforeStatus.getId());
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("files.json"), "{}");

        // Complete the first after run (FINISHED_WITH_DIFFERENCES)
        JLineupRunStatus afterStatus = testee.startAfterRun(beforeStatus.getId());
        afterStatus.getCurrentJobStepFuture().get().get();
        assertThat(testee.getRun(beforeStatus.getId()).get().getState(), is(State.FINISHED_WITH_DIFFERENCES));

        //when — rerun from finished run
        JLineupRunStatus rerunStatus = testee.rerunAfterFromRun(beforeStatus.getId());
        rerunStatus.getCurrentJobStepFuture().get().get();

        //then
        assertThat(rerunStatus.getId(), is(not(beforeStatus.getId())));
        assertThat(testee.getRun(rerunStatus.getId()).get().getState(), is(State.FINISHED_WITHOUT_DIFFERENCES));
    }

    @Test
    void shouldRejectRerunWhenRunIsNotRerunnable() throws Exception {
        //given — run in BEFORE_RUNNING state is not rerunnable
        JobConfig jobConfig = JobConfig.exampleConfig();
        // Don't let before run finish so it stays in BEFORE_RUNNING
        when(jLineupRunnerBefore.run()).thenAnswer(inv -> {
            Thread.sleep(5000);
            return true;
        });

        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        // Wait briefly for it to enter BEFORE_RUNNING
        Thread.sleep(200);

        //when/then
        assertThrows(InvalidRunStateException.class, () -> testee.rerunAfterFromRun(beforeStatus.getId()));
    }

    @Test
    void shouldRejectRerunWhenRunNotFound() {
        assertThrows(RunNotFoundException.class, () -> testee.rerunAfterFromRun("nonexistent-id"));
    }

    @Test
    void shouldCleanAfterArtifactsOnRerun() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        when(jLineupRunnerAfter.run()).thenReturn(true);

        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);
        beforeStatus.getCurrentJobStepFuture().get().get();

        // Set up a realistic source directory with before AND after artifacts
        Path sourceDir = tempDir.resolve("report-" + beforeStatus.getId());
        Files.createDirectories(sourceDir.resolve("abc123"));
        Files.createDirectories(sourceDir.resolve("def456"));

        // files.json with after and compare entries
        String filesJson = """
                {
                  "job-config": null,
                  "contexts": {
                    "abc123": {
                      "screenshotContext": null,
                      "screenshots": {
                        "0": {"before": "abc123/url_0_before.png", "after": "abc123/url_0_after.png", "compare": "abc123/url_0_compare.png"},
                        "500": {"before": "abc123/url_500_before.png", "after": "abc123/url_500_after.png"}
                      }
                    },
                    "def456": {
                      "screenshotContext": null,
                      "screenshots": {
                        "0": {"before": "def456/page_0_before.png", "after": "def456/page_0_after.png"}
                      }
                    }
                  },
                  "browsers": {"before": ["chrome"], "after": ["chrome"]}
                }
                """;
        Files.writeString(sourceDir.resolve("files.json"), filesJson);

        // Before screenshots (should be preserved)
        Files.writeString(sourceDir.resolve("abc123").resolve("url_0_before.png"), "before-data");
        Files.writeString(sourceDir.resolve("abc123").resolve("url_500_before.png"), "before-data");
        Files.writeString(sourceDir.resolve("def456").resolve("page_0_before.png"), "before-data");

        // After screenshots (should be deleted)
        Files.writeString(sourceDir.resolve("abc123").resolve("url_0_after.png"), "stale-after");
        Files.writeString(sourceDir.resolve("abc123").resolve("url_500_after.png"), "stale-after");
        Files.writeString(sourceDir.resolve("abc123").resolve("url_0_compare.png"), "stale-compare");
        Files.writeString(sourceDir.resolve("def456").resolve("page_0_after.png"), "stale-after");

        // Metadata files
        Files.writeString(sourceDir.resolve("abc123").resolve("metadata_after.json"), "{}");
        Files.writeString(sourceDir.resolve("abc123").resolve("metadata_before.json"), "{}");

        //when
        JLineupRunStatus rerunStatus = testee.rerunAfterFromRun(beforeStatus.getId());
        rerunStatus.getCurrentJobStepFuture().get().get();

        //then
        Path newDir = tempDir.resolve("report-" + rerunStatus.getId());

        // Before screenshots preserved
        assertTrue(Files.exists(newDir.resolve("abc123").resolve("url_0_before.png")));
        assertTrue(Files.exists(newDir.resolve("abc123").resolve("url_500_before.png")));
        assertTrue(Files.exists(newDir.resolve("def456").resolve("page_0_before.png")));
        assertTrue(Files.exists(newDir.resolve("abc123").resolve("metadata_before.json")));

        // After/compare screenshots deleted
        assertFalse(Files.exists(newDir.resolve("abc123").resolve("url_0_after.png")));
        assertFalse(Files.exists(newDir.resolve("abc123").resolve("url_500_after.png")));
        assertFalse(Files.exists(newDir.resolve("abc123").resolve("url_0_compare.png")));
        assertFalse(Files.exists(newDir.resolve("def456").resolve("page_0_after.png")));
        assertFalse(Files.exists(newDir.resolve("abc123").resolve("metadata_after.json")));

        // files.json should not contain after/compare entries
        String newFilesJson = Files.readString(newDir.resolve("files.json"));
        assertFalse(newFilesJson.contains("\"after\""));
        assertFalse(newFilesJson.contains("\"compare\""));
        assertTrue(newFilesJson.contains("\"before\""));
    }

}