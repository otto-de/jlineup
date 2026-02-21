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

}