package de.otto.jlineup.web;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.edison.status.domain.StatusDetail;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.service.JLineupService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static de.otto.jlineup.config.JobConfig.DEFAULT_WARMUP_BROWSER_CACHE_TIME;
import static de.otto.jlineup.config.JobConfig.configBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JLineupRunsStatusDetailIndicatorTest {

    @Mock
    private JLineupService jLineupService;

    @InjectMocks
    private JLineupRunsStatusDetailIndicator jLineupRunsStatusDetailIndicator;

    @Test
    public void shouldCreateAndSortStatusDetailIndicators() {
        Instant now = Instant.now();
        when(jLineupService.getRunStatus()).thenReturn(ImmutableList.of(
                JLineupRunStatus.runStatusBuilder()
                        .withStartTime(now.minus(5, ChronoUnit.HOURS))
                        .withEndTime(now.plus(1, ChronoUnit.HOURS))
                        .withState(State.FINISHED_WITHOUT_DIFFERENCES)
                        .withId("someOldId")
                        .withReports(JLineupRunStatus.Reports.reportsBuilder().withHtmlUrl("reportHtmlUrl").build())
                        .withJobConfig(createJobConfigWithUrl("www.sample0.de"))
                        .build(),
                JLineupRunStatus.runStatusBuilder()
                        .withStartTime(now)
                        .withEndTime(now.plus(1, ChronoUnit.HOURS))
                        .withState(State.FINISHED_WITHOUT_DIFFERENCES)
                        .withId("someId")
                        .withReports(JLineupRunStatus.Reports.reportsBuilder().withHtmlUrl("reportHtmlUrl").build())
                        .withJobConfig(createJobConfigWithUrl("www.sample1.de"))
                        .build(),
                JLineupRunStatus.runStatusBuilder()
                        .withStartTime(now.minus(1, ChronoUnit.HOURS))
                        .withEndTime(now.plus(1, ChronoUnit.HOURS))
                        .withState(State.AFTER_RUNNING)
                        .withId("otherId")
                        .withJobConfig(createJobConfigWithUrl("www.other1.de"))
                        .build()

        ));

        List<StatusDetail> statusDetailList = jLineupRunsStatusDetailIndicator.statusDetails();

        assertThat(statusDetailList.get(0).getName(), is("JLineup Run for www.sample1.de"));
        assertThat(statusDetailList.get(0).getMessage(), is("Run id: someId State: FINISHED Duration: 01:00:00.000"));
        assertThat(statusDetailList.get(0).getLinks().get(0).href, is("reportHtmlUrl"));

        assertThat(statusDetailList.get(1).getName(), is("JLineup Run for www.other1.de"));
        assertThat(statusDetailList.get(1).getMessage(), is("Run id: otherId State: AFTER_RUNNING Duration: 02:00:00.000"));
        assertThat(statusDetailList.get(1).getLinks().isEmpty(), is(true));
    }

    private JobConfig createJobConfigWithUrl(String url) {
        return configBuilder()
                .withUrls(ImmutableMap.of(url,
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
                                0
                        )))
                .build();

    }
}