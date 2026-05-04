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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JLineupServiceTest {

    private JLineupService testee;
    private JLineupRunnerFactory jLineupRunnerFactory;
    private JLineupRunner jLineupRunnerBefore;
    private JLineupRunner jLineupRunnerAfter;

    @BeforeEach
    void setUp() throws Exception {
        jLineupRunnerFactory = mock(JLineupRunnerFactory.class);
        jLineupRunnerBefore = mock(JLineupRunner.class);
        jLineupRunnerAfter = mock(JLineupRunner.class);
        JLineupWebProperties jLineupWebProperties = mock(JLineupWebProperties.class);
        JLineupWebLambdaProperties jLineupWebLambdaProperties = mock(JLineupWebLambdaProperties.class);
        RunPersistenceService runPersistenceService = mock(RunPersistenceService.class);

        when(jLineupRunnerFactory.createBeforeRun(any(), any())).thenReturn(jLineupRunnerBefore);
        when(jLineupRunnerFactory.createAfterRun(any(), any())).thenReturn(jLineupRunnerAfter);
        when(jLineupWebProperties.getMaxParallelJobs()).thenReturn(1);
        when(jLineupWebProperties.getLambda()).thenReturn(jLineupWebLambdaProperties);
        when(jLineupWebProperties.getLambda().getFunctionName()).thenReturn("jlineup-lambda-test");
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

}