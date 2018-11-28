package de.otto.jlineup.web;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.HttpCheckConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.utils.RegexMatcher;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static de.otto.jlineup.config.JobConfig.*;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JLineupWebApplication.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class JLineupWebApplicationTests {

    @Value("${local.server.port}")
    private Integer port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void shouldMakeFullJLineupRun() {
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
    public void shouldFailIfBrowserIsNotConfigured() {
        JobConfig jobConfig = copyOfBuilder(createTestConfig()).withBrowser(Browser.Type.FIREFOX).build();
        expectStatusCodeForConfig(jobConfig, UNPROCESSABLE_ENTITY.value());
    }

    private void assertReportExists(JLineupRunStatus finalState) {
        when()
                .get(contextPath + "/reports/report-" + finalState.getId() + "/report.json")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .assertThat()
                .body("summary.differenceMax", is(0.0f));
    }

    private String startBeforeRun(JobConfig jobConfig) {
        return given()
                .body(jobConfig)
                .contentType(ContentType.JSON)
                .when()
                .post(contextPath + "/runs")
                .then()
                .assertThat()
                .statusCode(ACCEPTED.value())
                .header(HttpHeaders.LOCATION, RegexMatcher.regex(contextPath + "/runs/[a-zA-Z0-9\\-]*"))
                .and()
                .extract().header(HttpHeaders.LOCATION);
    }

    private String startAfterRun(String location) {
        return given()
                .when()
                .post(location)
                .then()
                .assertThat()
                .statusCode(ACCEPTED.value())
                .header(HttpHeaders.LOCATION, RegexMatcher.regex(contextPath + "/runs/[a-zA-Z0-9\\-]*"))
                .and()
                .extract().header(HttpHeaders.LOCATION);
    }

    private void expectStatusCodeForConfig(JobConfig jobConfig, int statusCode) {
        given()
                .body(jobConfig)
                .contentType(ContentType.JSON)
                .when()
                .post(contextPath + "/runs")
                .then()
                .assertThat()
                .statusCode(statusCode);
    }

    private JLineupRunStatus awaitRunState(State expectedState, String location) {
        final JLineupRunStatus[] status = new JLineupRunStatus[1];
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    status[0] = when().get(location)
                            .then().extract().body().as(JLineupRunStatus.class);
                    return status[0].getState() == expectedState;
                });
        return status[0];
    }

    private JobConfig createTestConfig() {
        return configBuilder()
                .withUrls(ImmutableMap.of("http://www.example.com",
                        new UrlConfig(
                                ImmutableList.of("/"),
                                0,
                                ImmutableList.of(),
                                ImmutableMap.of(),
                                ImmutableMap.of(),
                                ImmutableMap.of(),
                                ImmutableList.of(600),
                                100000,
                                0,
                                0,
                                0,
                                DEFAULT_WARMUP_BROWSER_CACHE_TIME,
                                null,
                                0,
                                new HttpCheckConfig(),
                                0,
                                false
                        )))
                .build();
    }
}
