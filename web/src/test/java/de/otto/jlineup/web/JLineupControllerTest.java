package de.otto.jlineup.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.service.BrowserNotInstalledException;
import de.otto.jlineup.service.InvalidRunStateException;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.service.RunNotFoundException;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import de.otto.jlineup.web.configuration.JacksonConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static de.otto.jlineup.config.JobConfig.exampleConfig;
import static de.otto.jlineup.web.JLineupRunStatus.runStatusBuilder;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@RunWith(SpringRunner.class)
@JsonTest
@AutoConfigureJsonTesters
@ContextConfiguration(classes = JacksonConfiguration.class)
public class JLineupControllerTest {

    @Mock
    private JLineupService jLineupService;

    @Autowired
    private ObjectMapper objectMapper;

    private AutoCloseable autoCloseable;

    private MockMvc mvc;

    @Before
    public void setUp() {
        autoCloseable = openMocks(this);
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new
                MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
        JLineupController jLineupController = new JLineupController(jLineupService, new JLineupWebProperties());
        mvc = standaloneSetup(jLineupController).setMessageConverters(mappingJackson2HttpMessageConverter).build();
    }

    @After
    public void cleanUp() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void shouldReturn404WhenRunNotFound() throws Exception {
        // given
        String someRunId = UUID.randomUUID().toString();
        when(jLineupService.getRun(someRunId)).thenReturn(Optional.empty());

        // when
        ResultActions result = mvc
                .perform(get("/testContextPath/runs/" + someRunId)
                        .contextPath("/testContextPath")
                        .accept(MediaType.APPLICATION_JSON));

        //then
        result.andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnRun() throws Exception {
        // given
        String someRunId = UUID.randomUUID().toString();
        when(jLineupService.getRun(someRunId)).thenReturn(Optional.of(runStatusBuilder()
                .withId(someRunId)
                .withState(State.BEFORE_RUNNING)
                .withJobConfig(exampleConfig())
                .build()));

        // when
        ResultActions result = mvc
                .perform(get("/testContextPath/runs/" + someRunId)
                        .contextPath("/testContextPath")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    public void shouldReturn404IfRunIdIsNoValidUUID() throws Exception {
        // when
        ResultActions result = mvc
                .perform(get("/testContextPath/runs/someId")
                        .contextPath("/testContextPath")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    public void shouldIncludeLinksWithContextPath() throws Exception {
        // given
        Instant startTime = Instant.ofEpochMilli(1000);
        String someRunId = UUID.randomUUID().toString();
        when(jLineupService.getRun(someRunId)).thenReturn(Optional.of(runStatusBuilder()
                .withId(someRunId)
                .withState(State.FINISHED_WITHOUT_DIFFERENCES)
                .withJobConfig(exampleConfig())
                .withReports(JLineupRunStatus.Reports.reportsBuilder()
                        .withHtmlUrl("/htmlReport/report.html")
                        .withJsonUrl("/jsonReport/report.json")
                        .withLogUrl("/log/log.log")
                        .build())
                .withStartTime(startTime)
                .build()));

        // when
        ResultActions result = mvc
                .perform(get("/testContextPath/runs/" + someRunId)
                        .contextPath("/testContextPath")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk());
        result.andExpect(content().json(
                "{\"id\":\"" + someRunId + "\",\"state\":\"FINISHED_WITHOUT_DIFFERENCES\",\"startTime\":\"1970-01-01T00:00:01Z\",\"endTime\":null,\"reports\":{\"htmlUrl\":\"http://localhost/testContextPath/htmlReport/report.html\",\"jsonUrl\":\"http://localhost/testContextPath/jsonReport/report.json\",\"logUrl\":\"http://localhost/testContextPath/log/log.log\"}}"
        ));
    }

    @Test
    public void shouldStartNewRun() throws Exception {

        // given
        JobConfig jobConfig = exampleConfig();
        JLineupRunStatus run = runStatusBuilder().withId("someNewId").withJobConfig(jobConfig).withState(State.BEFORE_RUNNING).build();
        when(jLineupService.startBeforeRun(any())).thenReturn(run);

        // when
        ResultActions result = mvc
                .perform(post("/testContextPath/runs")
                        .contextPath("/testContextPath")
                        .content(JobConfig.prettyPrint(jobConfig))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isAccepted())
                .andExpect(content().json("{\"id\":\"someNewId\"}"))
                .andExpect(header().string("Location", "/testContextPath/runs/someNewId"));

        verify(jLineupService).startBeforeRun(jobConfig);
    }

    @Test
    public void shouldParseConfigWithCookie() throws Exception {

        String realWorldConfig = "{\n" +
                "  \"name\": \"promo-shoppromo\",\n" +
                "  \"urls\": {\n" +
                "    \"https://www.otto.de/\": {\n" +
                "      \"paths\": [\n" +
                "        \"/promo-shoppromo/shoppromo?dreson=(test.all.shoppromo.widgettypes)&preview=true\",\n" +
                "        \"/promo-shoppromo/shoppromo?dreson=(test.all.shoppromo.widgettypes)&preview=true&index=0&variant=cinema\",\n" +
                "        \"/promo-shoppromo/shoppromo?dreson=(test.all.shoppromo.widgettypes)&preview=true&index=1&variant=cinema\",\n" +
                "        \"/promo-shoppromo/shoppromo?dreson=(test.all.shoppromo.widgettypes)&preview=true&index=2\"\n" +
                "      ],\n" +
                "      \"max-diff\": 0.0,\n" +
                "      \"cookies\": [\n" +
                "        {\n" +
                "          \"name\": \"AWS-COOKIE\",\n" +
                "          \"value\": \"AWS_COOKIE_PLACEHOLDER\",\n" +
                "          \"domain\": \".otto.de\",\n" +
                "          \"path\": \"/\",\n" +
                "          \"expiry\": \"2022-01-01T01:00:01+0100\",\n" +
                "          \"secure\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"BYPASS-RATE-LIMIT\",\n" +
                "          \"value\": \"BYPASS_RATE_LIMIT_COOKIE_PLACEHOLDER\",\n" +
                "          \"domain\": \".otto.de\",\n" +
                "          \"path\": \"/\",\n" +
                "          \"expiry\": \"2020-01-01\",\n" +
                "          \"secure\": true\n" +
                "        },\n" +
                "\t{\n" +
                "          \"name\": \"trackingDisabled\",\n" +
                "          \"value\": \"true\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"survey\",\n" +
                "          \"value\": \"1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"local-storage\": {\n" +
                "        \"us_customerServiceWidget\": \"{'customerServiceWidgetNotificationHidden':{'value':true,'timestamp':9467812242358}}\",\n" +
                "        \"stopSlidingCinemaForJLineup\": \"true\"\n" +
                "      },\n" +
                "      \"window-widths\": [\n" +
                "        320,\n" +
                "        448,\n" +
                "        768,\n" +
                "        992\n" +
                "      ],\n" +
                "      \"http-check\": {\n" +
                "        \"enabled\": true,\n" +
                "        \"allowed-codes\": [\n" +
                "          200\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"browser\": \"chrome-headless\",\n" +
                "  \"check-for-errors-in-log\":false,\n" +
                "  \"wait-after-page-load\": 5.0,\n" +
                "  \"page-load-timeout\": 120,\n" +
                "  \"window-height\": 1000,\n" +
                "  \"timeout\": 600\n" +
                "}\n";

        // given
        JLineupRunStatus run = runStatusBuilder().withId("someNewId").withJobConfig(objectMapper.readValue(realWorldConfig, JobConfig.class)).withState(State.BEFORE_RUNNING).build();
        when(jLineupService.startBeforeRun(any())).thenReturn(run);

        // when
        ResultActions result = mvc
                .perform(post("/testContextPath/runs")
                        .contextPath("/testContextPath")
                        .content(realWorldConfig)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isAccepted())
                .andExpect(content().json("{\"id\":\"someNewId\"}"));
              //  .andExpect(header().string("Location", "/testContextPath/runs/someNewId"));

        //verify(jLineupService).startBeforeRun(jobConfig);

    }

    @Test
    public void shouldReturn400WhenConfigIsMissing() throws Exception {

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content("")
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldStartAfterRun() throws Exception {

        // given
        String someRunId = UUID.randomUUID().toString();
        JLineupRunStatus run = runStatusBuilder().withId(someRunId).withJobConfig(exampleConfig()).withState(State.AFTER_RUNNING).build();
        when(jLineupService.startAfterRun(someRunId)).thenReturn(run);

        // when
        ResultActions result = mvc
                .perform(post("/testContextPath/runs/" + someRunId)
                        .contextPath("/testContextPath")
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/testContextPath/runs/" + someRunId));
    }

    @Test
    public void shouldReturn404ForAfterStepWhenRunIsUnknown() throws Exception {

        // given
        String unknownRunId = UUID.randomUUID().toString();
        when(jLineupService.startAfterRun(unknownRunId)).thenThrow(new RunNotFoundException(unknownRunId));

        // when
        ResultActions result = mvc
                .perform(post("/runs/" + unknownRunId)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(unknownRunId)));
    }

    @Test
    public void shouldReturn404ForAfterStepWhenRunIdIsInvalid() throws Exception {

        // given
        String runId = "noValidUUID";

        // when
        ResultActions result = mvc
                .perform(post("/runs/" + runId)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(runId)));
    }

    @Test
    public void shouldReturn412ForAfterStepWhenRunIsNotReadyForAfterStep() throws Exception {

        // given
        String runInInvalidStateId = UUID.randomUUID().toString();
        when(jLineupService.startAfterRun(runInInvalidStateId)).thenThrow(new InvalidRunStateException(runInInvalidStateId, State.BEFORE_RUNNING, State.BEFORE_DONE));

        // when
        ResultActions result = mvc
                .perform(post("/runs/" + runInInvalidStateId)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().string(containsString(runInInvalidStateId)))
                .andExpect(content().string(containsString(State.BEFORE_RUNNING.name())))
                .andExpect(content().string(containsString(State.BEFORE_DONE.name())));
    }

    @Test
    public void shouldReturn422ForUnsupportedBrowser() throws Exception {

        // given
        JobConfig jobConfig = exampleConfig();
        when(jLineupService.startBeforeRun(jobConfig)).thenThrow(new BrowserNotInstalledException(jobConfig.browser));

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content(JobConfig.prettyPrint(jobConfig))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString(jobConfig.browser.name())));
    }

    @Test
    public void shouldReturn422IfConfigValidationFails() throws Exception {

        // given
        JobConfig jobConfig = JobConfig.copyOfBuilder(exampleConfig()).withUrls(null).build();
        when(jLineupService.startBeforeRun(jobConfig)).thenThrow(new ValidationError("Validation message"));

        // when
        ResultActions result = mvc
                .perform(post("/runs")
                        .content(JobConfig.prettyPrint(jobConfig))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Validation message")));
    }

}