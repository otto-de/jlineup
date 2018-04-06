package de.otto.jlineup.service;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupRunnerFactory;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JLineupServiceTest {

    private JLineupService testee;

    private JLineupRunnerFactory jLineupRunnerFactory;
    private JLineupRunner jLineupRunnerBefore;
    private JLineupRunner jLineupRunnerAfter;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        jLineupRunnerFactory = mock(JLineupRunnerFactory.class);
        jLineupRunnerBefore = mock(JLineupRunner.class);
        jLineupRunnerAfter = mock(JLineupRunner.class);
        when(jLineupRunnerFactory.createBeforeRun(any(), any())).thenReturn(jLineupRunnerBefore);
        when(jLineupRunnerFactory.createAfterRun(any(), any())).thenReturn(jLineupRunnerAfter);
        testee = new JLineupService(jLineupRunnerFactory);
    }

    @Test
    public void shouldStartBeforeRun() throws IOException, InterruptedException {

        //given
        JobConfig jobConfig = JobConfig.exampleConfig();

        //when
        String id = testee.startBeforeRun(jobConfig).getId();
        waitForCompletion();

        //then
        verify(jLineupRunnerFactory).createBeforeRun(id, jobConfig);
        verify(jLineupRunnerBefore).run();
    }

    @Test
    public void shouldStartAfterRun() throws IOException, InterruptedException, InvalidRunStateException, RunNotFoundException {

        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        when(jLineupRunnerBefore.run()).thenReturn(true);
        String id = testee.startBeforeRun(jobConfig).getId();

        //when
        waitForCompletion();
        testee.startAfterRun(id);
        waitForCompletion();

        //then
        verify(jLineupRunnerFactory).createAfterRun(id, jobConfig);
        verify(jLineupRunnerAfter).run();
    }

    private void waitForCompletion() throws InterruptedException {
        while(testee.getRunningJobsCount() > 0) {
            Thread.sleep(10);
        }
        return;
    }

}