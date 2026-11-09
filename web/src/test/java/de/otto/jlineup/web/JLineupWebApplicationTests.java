package de.otto.jlineup.web;

import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableList.of;
import static de.otto.jlineup.config.JobConfig.copyOfBuilder;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;

@ActiveProfiles("test")
@ContextConfiguration(classes = {JLineupWebApplication.class})
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
class JLineupWebApplicationTests {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupWebApplicationTests.class);
    private static final int MAX_ATTEMPTS = 3;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldMakeFullJLineupRun() {
        List<JLineupRunStatus> finalStates = runBeforeAndAfterWithRetries(List.of(createTestConfig()));

        finalStates.forEach(this::assertReportExists);
    }

    @Test
    public void shouldMakeFullJLineupRunInParallel() {
        List<JLineupRunStatus> finalStates = runBeforeAndAfterWithRetries(List.of(createTestConfig(), createTestConfig()));

        finalStates.forEach(this::assertReportExists);
    }

    @Test
    public void shouldMakeFullJLineupRunWithMultipleThreads() {
        JobConfig jobConfig = jobConfigBuilder().addUrlConfig("https://www.example.com",
                        UrlConfig.urlConfigBuilder()
                                .withDevices(of(
                                        DeviceConfig.deviceConfig(500, 500),
                                        DeviceConfig.deviceConfig(700, 700)))
                                .build())
                .withThreads(2)
                .build();

        List<JLineupRunStatus> finalStates = runBeforeAndAfterWithRetries(List.of(jobConfig));

        finalStates.forEach(this::assertReportExists);
    }

    @Test
    public void shouldFailIfBrowserIsNotConfigured() {
        JobConfig jobConfig = copyOfBuilder(createTestConfig()).withBrowser(Browser.Type.FIREFOX).build();
        expectStatusCodeForConfig(jobConfig, 422);
    }

    @Test
    public void shouldFailForForbiddenUrl() {
        JobConfig jobConfig = copyOfBuilder(createTestConfig()).addUrlConfig("https://www.forbidden.com", UrlConfig.urlConfigBuilder().build()).build();
        expectStatusCodeForConfig(jobConfig, 422);
    }

    private void assertReportExists(JLineupRunStatus finalState) {
        ResponseEntity<String> response = this.testRestTemplate.getForEntity("/reports/report-" + finalState.getId() + "/report.json", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful(), is(true));
    }

    private String startBeforeRun(JobConfig jobConfig) {
        ResponseEntity<String> response = this.testRestTemplate.postForEntity( "/runs", jobConfig, String.class);
        assertThat(response.getStatusCode(), equalTo(ACCEPTED));
        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(location, matchesPattern(contextPath + "/runs/[a-zA-Z0-9\\-]*"));
        return location.substring(contextPath.length());
    }

    private String startAfterRun(String location) {
        ResponseEntity<String> response = this.testRestTemplate.postForEntity(location, null, String.class);
        assertThat(response.getStatusCode(), equalTo(ACCEPTED));
        String afterLocation = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(afterLocation, matchesPattern(contextPath + "/runs/[a-zA-Z0-9\\-]*"));
        return afterLocation.substring(contextPath.length());
    }

    private void expectStatusCodeForConfig(JobConfig jobConfig, int statusCode) {

        ResponseEntity<String> response = this.testRestTemplate.postForEntity( "/runs", jobConfig, String.class);
        assertThat(response.getStatusCode().value(), equalTo(statusCode));
    }

    /**
     * Runs 'before' for all given configs, then 'after' for all of them, and expects every run to finish without
     * differences. The tests screenshot a live website, which occasionally renders differently between 'before'
     * and 'after'. Only that case (FINISHED_WITH_DIFFERENCES) is retried with a completely fresh before/after cycle;
     * errors and timeouts fail immediately.
     */
    private List<JLineupRunStatus> runBeforeAndAfterWithRetries(List<JobConfig> jobConfigs) {
        for (int attempt = 1; ; attempt++) {
            List<String> beforeLocations = jobConfigs.stream().map(this::startBeforeRun).toList();
            beforeLocations.forEach(location -> awaitRunState(State.BEFORE_DONE, location));

            List<String> afterLocations = beforeLocations.stream().map(this::startAfterRun).toList();
            List<JLineupRunStatus> finalStates = afterLocations.stream().map(this::awaitRunDone).toList();

            if (finalStates.stream().allMatch(status -> status.getState() == State.FINISHED_WITHOUT_DIFFERENCES)) {
                return finalStates;
            }

            List<String> runsWithDifferences = finalStates.stream()
                    .filter(status -> status.getState() == State.FINISHED_WITH_DIFFERENCES)
                    .map(JLineupRunStatus::getId)
                    .toList();
            if (attempt >= MAX_ATTEMPTS) {
                fail("Runs " + runsWithDifferences + " still finished with differences after " + MAX_ATTEMPTS + " attempts.");
            }
            LOG.warn("Attempt {}/{}: runs {} finished with differences, retrying the whole before/after cycle.",
                    attempt, MAX_ATTEMPTS, runsWithDifferences);
        }
    }

    private JLineupRunStatus awaitRunDone(String location) {
        JLineupRunStatus status = awaitStatus(location, State::isDone);
        assertThat("Run " + status.getId() + " ended in an unexpected state",
                status.getState(), is(oneOf(State.FINISHED_WITHOUT_DIFFERENCES, State.FINISHED_WITH_DIFFERENCES)));
        return status;
    }

    private JLineupRunStatus awaitRunState(State expectedState, String location) {
        JLineupRunStatus status = awaitStatus(location, state -> state == expectedState || state.isDone());
        assertThat("Run " + status.getId() + " ended in an unexpected state", status.getState(), is(expectedState));
        return status;
    }

    private JLineupRunStatus awaitStatus(String location, Predicate<State> stopCondition) {
        final JLineupRunStatus[] status = new JLineupRunStatus[1];
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    status[0] = this.testRestTemplate.getForEntity(location, JLineupRunStatus.class).getBody();
                    assertThat(status[0], notNullValue());
                    return stopCondition.test(status[0].getState());
                });
        return status[0];
    }

    private JobConfig createTestConfig() {
        return jobConfigBuilder()
                .withUrls(ImmutableMap.of("https://www.example.com",
                        UrlConfig.urlConfigBuilder().withWindowWidths(of(600)).withMaxScrollHeight(100000).build()))
                .withGlobalWaitAfterPageLoad(2f)
                .build()
                .insertDefaults();
    }
}
