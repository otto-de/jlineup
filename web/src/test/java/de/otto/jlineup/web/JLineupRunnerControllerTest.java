package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.service.InvalidRunStateException;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.service.RunNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static de.otto.jlineup.web.JLineupRunStatus.jLineupRunStatusBuilder;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class JLineupRunnerControllerTest {

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
                .withConfig(JobConfig.exampleConfig())
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
        JobConfig jobConfig = JobConfig.exampleConfig();
        JLineupRunStatus run = jLineupRunStatusBuilder().withId("someNewId").withConfig(jobConfig).withState(State.BEFORE_RUNNING).build();
        when(jLineupService.startBeforeRun(any())).thenReturn(run);

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content(JobConfig.prettyPrint(jobConfig))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/runs/someNewId"));

        verify(jLineupService).startBeforeRun(jobConfig);
    }

    @Test
    public void shouldReturn400WhenConfigIsMissing() throws Exception {

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
    public void shouldStartAfterRun() throws Exception {

        // given
        JLineupRunStatus run = jLineupRunStatusBuilder().withId("someRunId").withConfig(JobConfig.exampleConfig()).withState(State.AFTER_RUNNING).build();
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
        String runId = "unknownId";
        when(jLineupService.startAfterRun(runId)).thenThrow(new RunNotFoundException(runId));

        // when
        ResultActions result = mvc
                .perform(post("/runs/" + runId)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(runId)));
    }

    @Test
    public void shouldReturn412ForAfterStepWhenRunIsNotReadyForAfterStep() throws Exception {

        // given
        String runId = "unknownId";
        when(jLineupService.startAfterRun(runId)).thenThrow(new InvalidRunStateException(runId, State.BEFORE_RUNNING, State.BEFORE_DONE));

        // when
        ResultActions result = mvc
                .perform(post("/runs/" + runId)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().string(containsString(runId)))
                .andExpect(content().string(containsString(State.BEFORE_RUNNING.name())))
                .andExpect(content().string(containsString(State.BEFORE_DONE.name())));
    }

}