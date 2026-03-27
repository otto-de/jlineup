package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.service.JLineupService;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ReportsAndRunWebController {

    private final JLineupService jLineupService;
    private final JLineupWebProperties jLineupWebProperties;

    private final String managementBasePath;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm").withZone(ZoneId.systemDefault());

    @Autowired
    public ReportsAndRunWebController(JLineupService jLineupService, JLineupWebProperties jLineupWebProperties,
                                      @Value("${edison.application.management.base-path:/internal}") String managementBasePath) {
        this.jLineupService = jLineupService;
        this.jLineupWebProperties = jLineupWebProperties;
        this.managementBasePath = managementBasePath;
    }

    @RequestMapping(
            value = "${edison.application.management.base-path:/internal}/reports",
            produces = "text/html",
            method = GET)
    public ModelAndView getReportsPage() {
        return new ModelAndView("reports") {{

            addObject("reportList", jLineupService.getRunStatus().stream()
                    .sorted(Comparator.comparing(JLineupRunStatus::getStartTime).reversed())
                    .map(s -> new Report(s, managementBasePath))
                    .limit(jLineupWebProperties.getMaxPersistedRuns())
                    .collect(toList()));
        }};
    }

    @RequestMapping(
            value = "${edison.application.management.base-path:/internal}/run",
            produces = "text/html",
            method = GET)
    public ModelAndView getRunPage() {
        return new ModelAndView("run") {{
            addObject("exampleConfig", JobConfig.prettyPrintWithAllFields(JobConfig.exampleConfig()));
            addObject("reportsUrl", managementBasePath + "/reports");
        }};
    }

    private static String getDurationAsString(JLineupRunStatus status) {

        final AtomicLong durationMillis = new AtomicLong();
        Instant startTime = status.getStartTime();

        status.getPauseTime().ifPresent(pauseTime -> durationMillis.addAndGet(Duration.between(startTime, pauseTime).toMillis()));
        status.getEndTime().ifPresent(endTime -> durationMillis.addAndGet(Duration.between(status.getResumeTime().orElse(endTime), endTime).toMillis()));

        if (status.getState() == State.BEFORE_RUNNING) {
           durationMillis.addAndGet(Duration.between(startTime, Instant.now()).toMillis());
        } else if (status.getState() == State.AFTER_RUNNING) {
            durationMillis.addAndGet(Duration.between(status.getResumeTime().orElse(Instant.now()), Instant.now()).toMillis());
        }

        Duration duration = Duration.ofMillis(durationMillis.get());
        long HH = duration.toHours();
        long MM = duration.toMinutesPart();
        long SS = duration.toSecondsPart();

        return String.format("%02d:%02d:%02d", HH, MM, SS);
    }

    private static String formatTime(Instant time) {
        return dateTimeFormatter.format(time);
    }

    public static class Report {

        private String id;
        private final String name;
        private String reportUrl;
        private String logUrl;
        private String duration;
        private String startTime;
        private List<String> urls;
        private State state;
        private String afterRunUrl;

        public Report(JLineupRunStatus lineupRunStatus, String managementBasePath) {
            this.id = lineupRunStatus.getId();
            this.name = lineupRunStatus.getJobConfig().name;
            this.urls = lineupRunStatus.getUrls();
            this.reportUrl = lineupRunStatus.getReports() != null ?
                    lineupRunStatus.getReports().getHtmlUrlFromCurrentContext() : null;
            this.logUrl = lineupRunStatus.getReports() != null ?
                    lineupRunStatus.getReports().getLogUrlFromCurrentContext() : null;
            this.duration = getDurationAsString(lineupRunStatus);
            this.startTime = formatTime(lineupRunStatus.getStartTime());
            this.state = lineupRunStatus.getState();
            if (lineupRunStatus.getState() == State.BEFORE_DONE) {
                this.afterRunUrl = "/runs/" + lineupRunStatus.getId();
            }
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

        public String getState() {
            return state.getHumanReadableName();
        }

        public String getStateCssClass() {
            return switch (state) {
                case FINISHED_WITHOUT_DIFFERENCES -> "table-success";
                case FINISHED_WITH_DIFFERENCES -> "table-warning";
                case ERROR, DEAD -> "table-danger";
                case BEFORE_RUNNING, AFTER_RUNNING -> "table-info";
                default -> "table-secondary";
            };
        }

        public String getLogButtonCssClass() {
            return (state == State.ERROR || state == State.DEAD) ? "btn-danger" : "btn-light";
        }

        public void setState(State state) {
            this.state = state;
        }

        public String getLogUrl() {
            return logUrl;
        }

        public void setLogUrl(String logUrl) {
            this.logUrl = logUrl;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public List<String> getUrls() {
            return urls;
        }

        public void setUrls(List<String> urls) {
            this.urls = urls;
        }

        public String getAfterRunUrl() {
            return afterRunUrl;
        }

        @Override
        public String toString() {
            return "Report{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", reportUrl='" + reportUrl + '\'' +
                    ", logUrl='" + logUrl + '\'' +
                    ", duration='" + duration + '\'' +
                    ", startTime='" + startTime + '\'' +
                    ", urls=" + urls +
                    ", state=" + state +
                    ", afterRunUrl='" + afterRunUrl + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Report report = (Report) o;
            return Objects.equals(id, report.id) &&
                    Objects.equals(name, report.name) &&
                    Objects.equals(reportUrl, report.reportUrl) &&
                    Objects.equals(logUrl, report.logUrl) &&
                    Objects.equals(duration, report.duration) &&
                    Objects.equals(startTime, report.startTime) &&
                    Objects.equals(urls, report.urls) &&
                    Objects.equals(afterRunUrl, report.afterRunUrl) &&
                    state == report.state;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, reportUrl, logUrl, duration, startTime, urls, state, afterRunUrl);
        }

        public String getName() {
            return name;
        }
    }
}
