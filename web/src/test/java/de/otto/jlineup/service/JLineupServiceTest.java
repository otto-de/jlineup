package de.otto.jlineup.service;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.web.JLineupSpawner;
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

    private JLineupSpawner jLineupSpawner;
    private JLineup jLineupBefore;
    private JLineup jLineupAfter;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        jLineupSpawner = mock(JLineupSpawner.class);
        jLineupBefore = mock(JLineup.class);
        jLineupAfter = mock(JLineup.class);
        when(jLineupSpawner.createBeforeRun(any(), any())).thenReturn(jLineupBefore);
        when(jLineupSpawner.createAfterRun(any(), any())).thenReturn(jLineupAfter);
        testee = new JLineupService(jLineupSpawner, new JLineupWebProperties());
    }

    @Test
    public void shouldStartBeforeRun() throws IOException, InterruptedException {

        //given
        Config config = Config.exampleConfig();

        //when
        String id = testee.startBeforeRun(config).getId();
        Thread.sleep(100);

        //then
        verify(jLineupSpawner).createBeforeRun(id, config);
        verify(jLineupBefore).run();
    }

    @Test
    public void shouldStartAfterRun() throws IOException, InterruptedException, InvalidRunStateException, RunNotFoundException {

        //given
        Config config = Config.exampleConfig();
        String id = testee.startBeforeRun(config).getId();

        //when
        Thread.sleep(100);
        testee.startAfterRun(id);

        //then
        verify(jLineupSpawner).createAfterRun(id, config);
        verify(jLineupAfter).run();
    }

}