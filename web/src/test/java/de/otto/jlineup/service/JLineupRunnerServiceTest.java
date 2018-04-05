package de.otto.jlineup.service;

import de.otto.jlineup.JLineupRunner;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.web.JLineupSpawner;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JLineupRunnerServiceTest {

    private JLineupService testee;

    private JLineupSpawner jLineupSpawner;
    private JLineupRunner jLineupRunnerBefore;
    private JLineupRunner jLineupRunnerAfter;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        jLineupSpawner = mock(JLineupSpawner.class);
        jLineupRunnerBefore = mock(JLineupRunner.class);
        jLineupRunnerAfter = mock(JLineupRunner.class);
        when(jLineupSpawner.createBeforeRun(any(), any())).thenReturn(jLineupRunnerBefore);
        when(jLineupSpawner.createAfterRun(any(), any())).thenReturn(jLineupRunnerAfter);
        testee = new JLineupService(jLineupSpawner, new JLineupWebProperties());
    }

    @Test
    public void shouldStartBeforeRun() throws IOException, InterruptedException {

        //given
        JobConfig jobConfig = JobConfig.exampleConfig();

        //when
        String id = testee.startBeforeRun(jobConfig).getId();
        Thread.sleep(100);

        //then
        verify(jLineupSpawner).createBeforeRun(id, jobConfig);
        verify(jLineupRunnerBefore).run();
    }

    @Test
    public void shouldStartAfterRun() throws IOException, InterruptedException, InvalidRunStateException, RunNotFoundException {

        //given
        JobConfig jobConfig = JobConfig.exampleConfig();
        String id = testee.startBeforeRun(jobConfig).getId();

        //when
        Thread.sleep(100);
        testee.startAfterRun(id);

        //then
        verify(jLineupSpawner).createAfterRun(id, jobConfig);
        verify(jLineupRunnerAfter).run();
    }

}