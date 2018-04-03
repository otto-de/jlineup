package de.otto.jlineup.web;

import com.google.gson.JsonParseException;
import de.otto.jlineup.JLineup;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.Before;
import org.junit.Ignore;
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
    public void setUp() throws Exception {
        jLineupSpawner = mock(JLineupSpawner.class);
        jLineupBefore = mock(JLineup.class);
        jLineupAfter = mock(JLineup.class);
        when(jLineupSpawner.createBeforeRun(any(), any())).thenReturn(jLineupBefore);
        when(jLineupSpawner.createAfterRun(any(), any())).thenReturn(jLineupAfter);
        testee = new JLineupService(jLineupSpawner, new JLineupWebProperties());
    }

    @Test
    public void shouldThrowExceptionWithEmptyConfig() throws IOException {
        exception.expect(JsonParseException.class);
        exception.expectMessage("Config is not valid: ''");
        testee.startBeforeRun("");
    }

    @Test
    public void shouldThrowExceptionWithConfigWithoutURL() throws IOException {
        exception.expect(JsonParseException.class);
        exception.expectMessage("No urls in JLineup config.");
        testee.startBeforeRun("{}");
    }

    @Test
    public void shouldStartBeforeRun() throws IOException, InterruptedException {

        //given
        Config config = Config.exampleConfig();

        //when
        String id = testee.startBeforeRun(Config.prettyPrint(config)).getId();
        Thread.sleep(100);

        //then
        verify(jLineupSpawner).createBeforeRun(id, config);
        verify(jLineupBefore).run();
    }

    @Test
    public void shouldStartAfterRun() throws IOException, InterruptedException {

        //given
        Config config = Config.exampleConfig();
        String id = testee.startBeforeRun(Config.prettyPrint(config)).getId();

        //when
        Thread.sleep(100);
        testee.startAfterRun(id);

        //then
        verify(jLineupSpawner).createAfterRun(id, config);
        verify(jLineupAfter).run();
    }

    @Test
    @Ignore
    public void test() throws IOException {
        JLineupWebProperties jLineupWebProperties = new JLineupWebProperties();
        testee = new JLineupService(new JLineupSpawner(jLineupWebProperties), jLineupWebProperties);
        String id = testee.startBeforeRun(Config.prettyPrint(Config.exampleConfig())).getId();
        testee.startAfterRun(id);
    }
}