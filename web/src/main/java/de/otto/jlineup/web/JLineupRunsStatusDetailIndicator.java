package de.otto.jlineup.web;

import de.otto.edison.status.domain.Link;
import de.otto.edison.status.domain.Status;
import de.otto.edison.status.domain.StatusDetail;
import de.otto.edison.status.indicator.StatusDetailIndicator;
import de.otto.jlineup.service.JLineupService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toList;

@Component
public class JLineupRunsStatusDetailIndicator implements StatusDetailIndicator {

    @Autowired
    private JLineupService jLineupService;

    @Override
    public StatusDetail statusDetail() {
        return statusDetails().stream().findFirst().orElse(StatusDetail.statusDetail("ok", Status.OK, ""));
    }

    @Override
    public List<StatusDetail> statusDetails() {
        return jLineupService.getRunStatus().stream()
                .sorted(Comparator.comparing(JLineupRunStatus::getStartTime).reversed())
                .map(status -> {
                            List<Link> reportLink = status.getState() == State.FINISHED ?
                                    of(Link.link("", status.getReports().getHtmlUrl(), "Report")) : Collections.emptyList();

                            String message = String.format("Run id: %s State: %s Duration: %s", status.getId(), status.getState().toString(), getDuration(status));
                            return StatusDetail.statusDetail(
                                    "JLineup Run for " + Strings.join(status.getJobConfig().urls.keySet(), ','),
                                    Status.OK,
                                    message,
                                    reportLink);
                        }
                )
                .collect(toList());

    }

    private String getDuration(JLineupRunStatus status) {
        Instant endTime = status.getEndTime().orElse(Instant.now());
        Instant startTime = status.getStartTime();
        return DurationFormatUtils.formatDurationHMS(Duration.between(startTime, endTime).toMillis());
    }
}
