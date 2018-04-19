package de.otto.jlineup.service;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.State;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JLineupServiceTest {

    private JLineupService testee;
    private JLineupWebProperties jLineupWebProperties;
    private JLineupRunnerFactory jLineupRunnerFactory;
    private JLineupRunner jLineupRunnerBefore;
    private JLineupRunner jLineupRunnerAfter;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        jLineupRunnerFactory = mock(JLineupRunnerFactory.class);
        jLineupRunnerBefore = mock(JLineupRunner.class);
        jLineupRunnerAfter = mock(JLineupRunner.class);
        jLineupWebProperties = mock(JLineupWebProperties.class);
        when(jLineupRunnerFactory.createBeforeRun(any(), any())).thenReturn(jLineupRunnerBefore);
        when(jLineupRunnerFactory.createAfterRun(any(), any())).thenReturn(jLineupRunnerAfter);
        when(jLineupWebProperties.getMaxParallelJobs()).thenReturn(1);
        testee = new JLineupService(jLineupRunnerFactory, jLineupWebProperties);
    }

    @Test
    public void shouldStartBeforeRun() throws Exception {

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
    public void shouldStartAfterRun() throws Exception {

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
    public void shouldContainReportPathInResult() throws Exception {
        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        JLineupRunStatus beforeStatus = testee.startBeforeRun(jobConfig);

        when(jLineupRunnerAfter.run()).thenReturn(true);
        beforeStatus.getCurrentJobStepFuture().get().get();
        JLineupRunStatus afterStatus = testee.startAfterRun(beforeStatus.getId());
        afterStatus.getCurrentJobStepFuture().get().get();


        //when
        Optional<JLineupRunStatus> status = testee.getRun(beforeStatus.getId());

        //then
        assertTrue(status.isPresent());
        assertThat(status.get().getState(), is(State.FINISHED_WITHOUT_DIFFERENCES));
        assertThat(status.get().getReports().getHtmlUrl(), is("/reports/report-" + beforeStatus.getId() + "/report.html"));
        assertThat(status.get().getReports().getJsonUrl(), is("/reports/report-" + beforeStatus.getId() + "/report.json"));

    }

}