package de.otto.jlineup.web;

import de.otto.jlineup.service.JLineupService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ReportController {

    private JLineupService jLineupService;

    @Autowired
    public ReportController(JLineupService jLineupService) {
        this.jLineupService = jLineupService;
    }

    @RequestMapping(
            value = "${edison.application.management.base-path:/internal}/reports",
            produces = "text/html",
            method = GET)
    public ModelAndView getReports() {
        return new ModelAndView("reports") {{

            addObject("reportList", jLineupService.getRunStatus().stream()
                    .sorted(Comparator.comparing(JLineupRunStatus::getStartTime).reversed())
                    .map(Report::new)
                    .collect(toList()));
        }};
    }

    private static String getDurationAsString(JLineupRunStatus status) {
        Instant endTime = status.getEndTime().orElse(Instant.now());
        Instant startTime = status.getStartTime();
        return DurationFormatUtils.formatDurationHMS(Duration.between(startTime, endTime).toMillis());
    }

    public static class Report {

        private String id;
        private String reportUrl;
        private String duration;
        private State state;

        public Report(JLineupRunStatus lineupRunStatus) {
            this.id = lineupRunStatus.getId();
            this.reportUrl = lineupRunStatus.getReports() != null ?
                    lineupRunStatus.getReports().getHtmlUrl() : null;
            this.duration = getDurationAsString(lineupRunStatus);
            this.state = lineupRunStatus.getState();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getReportUrl() {
            return reportUrl;
        }

        public void setReportUrl(String reportUrl) {
            this.reportUrl = reportUrl;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Report report = (Report) o;
            return Objects.equals(id, report.id) &&
                    Objects.equals(reportUrl, report.reportUrl) &&
                    Objects.equals(duration, report.duration) &&
                    state == report.state;
        }

        @Override
        public int hashCode() {

            return Objects.hash(id, reportUrl, duration, state);
        }

        @Override
        public String toString() {
            return "Report{" +
                    "id='" + id + '\'' +
                    ", reportUrl='" + reportUrl + '\'' +
                    ", duration='" + duration + '\'' +
                    ", state=" + state +
                    '}';
        }
    }
}
