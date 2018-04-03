package de.otto.jlineup.web;

import com.google.gson.JsonParseException;
import de.otto.jlineup.config.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static de.otto.jlineup.web.JLineupRunStatus.jLineupRunStatusBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class JLineupControllerTest {

    @Mock
    private JLineupService jLineupService;

    private JLineupController jLineupController;

    private MockMvc mvc;

    @Before
    public void setUp() {
        initMocks(this);
        jLineupController = new JLineupController(jLineupService);
        mvc = standaloneSetup(jLineupController).build();
    }

    @Test
    public void shouldReturn404WhenRunNotFound() throws Exception {
        // given
        when(jLineupService.getRun("someId")).thenReturn(Optional.empty());

        // when
        ResultActions result = mvc
                .perform(get("/runs/someId")
                        .accept(MediaType.APPLICATION_JSON));

        //then
        result.andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnRun() throws Exception {
        // given
        when(jLineupService.getRun("someId")).thenReturn(Optional.of(jLineupRunStatusBuilder()
                .withId("someId")
                .withState(State.BEFORE_RUNNING)
                .withConfig(Config.exampleConfig())
                .build()));

        // when
        ResultActions result = mvc
                .perform(get("/runs/someId")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    public void shouldStartNewRun() throws Exception {

        // given
        String config = Config.prettyPrint(Config.exampleConfig());
        JLineupRunStatus run = jLineupRunStatusBuilder().withId("someNewId").withConfig(Config.exampleConfig()).withState(State.BEFORE_RUNNING).build();
        when(jLineupService.startBeforeRun(config)).thenReturn(run);

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content(config)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/runs/someNewId"));
    }

    @Test
    public void shouldReturn400WhenConfigIsMissing() throws Exception {

        // given
        when(jLineupService.startBeforeRun("")).thenThrow(new JsonParseException("Ex"));

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content("")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnUnprocessableWhenConfigIsInvalid() throws Exception {

        // given
        when(jLineupService.startBeforeRun("invalid")).thenThrow(new JsonParseException("Ex"));

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content("invalid")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void shouldStartAfterRun() throws Exception {

        // given
        JLineupRunStatus run = jLineupRunStatusBuilder().withId("someRunId").withConfig(Config.exampleConfig()).withState(State.AFTER_RUNNING).build();
        when(jLineupService.startAfterRun("someRunId")).thenReturn(run);

        // when
        ResultActions result = mvc
                .perform(post("/runs/someRunId")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/runs/someRunId"));
    }

    @Test
    public void shouldReturn404ForAfterStepWhenRunIsUnknown() throws Exception {

        // given
        when(jLineupService.startAfterRun("unknownId")).thenThrow(new JLineupWebException(SC_NOT_FOUND, "Run not found, cannot start after step"));

        // when
        ResultActions result = mvc
                .perform(post("/runs/unknownId")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isNotFound());

    }

}