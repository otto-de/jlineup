package de.otto.jlineup.web;

import de.otto.jlineup.JLineup;
import de.otto.jlineup.Util;
import de.otto.jlineup.config.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JLineupServiceTest {

    private JLineupService testee;

    private JLineupSpawner jLineupSpawner;
    private JLineup jLineupBefore;
    private JLineup jLineupAfter;

    @Before
    public void setUp() throws Exception {
        jLineupSpawner = mock(JLineupSpawner.class);
        jLineupBefore = mock(JLineup.class);
        jLineupAfter = mock(JLineup.class);
        when(jLineupSpawner.createBeforeRun(any(), any())).thenReturn(jLineupBefore);
        when(jLineupSpawner.createAfterRun(any(), any())).thenReturn(jLineupAfter);
        testee = new JLineupService(jLineupSpawner);
    }

    @Test
    public void shouldStartBeforeRun() throws IOException {

        //given
        Config config = Config.exampleConfig();

        //when
        String id = testee.startBeforeRun(Config.prettyPrint(config));

        //then
        verify(jLineupSpawner).createBeforeRun(id, config);
        verify(jLineupBefore).run();
    }

    @Test
    public void shouldStartAfterRun() throws IOException {

        //given
        Config config = Config.exampleConfig();
        String id = testee.startBeforeRun(Config.prettyPrint(config));

        //when
        testee.startAfterRun(id);

        //then
        verify(jLineupSpawner).createAfterRun(id, config);
        verify(jLineupAfter).run();
    }
}