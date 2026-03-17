package de.otto.jlineup.web;

import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableList.of;
import static de.otto.jlineup.config.JobConfig.copyOfBuilder;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;

@ActiveProfiles("test")
@ContextConfiguration(classes = {JLineupWebApplication.class})
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
class JLineupWebApplicationTests {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldMakeFullJLineupRun() {
        JobConfig jobConfig = createTestConfig();

        String location = startBeforeRun(jobConfig);

        awaitRunState(State.BEFORE_DONE, location);

        String locationAfter = startAfterRun(location);

        JLineupRunStatus finalState = awaitRunState(State.FINISHED_WITHOUT_DIFFERENCES, locationAfter);

        assertReportExists(finalState);
    }

    @Test
    public void shouldMakeFullJLineupRunInParallel() {
        JobConfig jobConfig = createTestConfig();
        JobConfig jobConfig2 = createTestConfig();

        String location = startBeforeRun(jobConfig);
        String location2 = startBeforeRun(jobConfig2);

        awaitRunState(State.BEFORE_DONE, location);
        awaitRunState(State.BEFORE_DONE, location2);

        String locationAfter = startAfterRun(location);
        String locationAfter2 = startAfterRun(location2);

        JLineupRunStatus finalState = awaitRunState(State.FINISHED_WITHOUT_DIFFERENCES, locationAfter);
        JLineupRunStatus finalState2 = awaitRunState(State.FINISHED_WITHOUT_DIFFERENCES, locationAfter2);

        assertReportExists(finalState);
        assertReportExists(finalState2);
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

        String location = startBeforeRun(jobConfig);
        awaitRunState(State.BEFORE_DONE, location);

        String locationAfter = startAfterRun(location);
        JLineupRunStatus finalState = awaitRunState(State.FINISHED_WITHOUT_DIFFERENCES, locationAfter);

        assertReportExists(finalState);
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

    private JLineupRunStatus awaitRunState(State expectedState, String location) {
        final JLineupRunStatus[] status = new JLineupRunStatus[1];
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    status[0] = this.testRestTemplate.getForEntity(location, JLineupRunStatus.class).getBody();
                    assertThat(status[0], notNullValue());
                    return status[0].getState() == expectedState;
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
