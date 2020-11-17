package de.otto.jlineup.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@JsonNaming(PropertyNamingStrategy.class)
@JsonDeserialize(builder = JLineupRunStatus.Builder.class)
public class JLineupRunStatus {

    private final String id;
    private final State state;
    private final Instant startTime;
    private final Instant pauseTime;
    private final Instant resumeTime;
    private final Instant endTime;
    private final Reports reports;

    /*
    private final Phase phase;
    private final Step step;
    private final Result result;
    */

    @JsonIgnore
    private final CompletableFuture<State> currentJobStepFuture;

    private final JobConfig jobConfig;

    private JLineupRunStatus(Builder builder) {
        id = builder.id;
        jobConfig = builder.jobConfig;
        state = builder.state;
        startTime = builder.startTime;
        pauseTime = builder.pauseTime;
        resumeTime = builder.resumeTime;
        endTime = builder.endTime;
        currentJobStepFuture = builder.currentJobStepFuture;
        reports = builder.reports;
    }

    public String getId() {
        return id;
    }

    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public State getState() {
        return state;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Optional<Instant> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    public Optional<Instant> getPauseTime() {
        return Optional.ofNullable(pauseTime);
    }

    public Optional<Instant> getResumeTime() {
        return Optional.ofNullable(resumeTime);
    }

    @JsonIgnore
    public List<String> getUrls() {
        List<String> urls = new ArrayList<>();
        Set<Map.Entry<String, UrlConfig>> urlMap = this.jobConfig.urls.entrySet();
        urlMap.forEach(urlMapEntry -> urlMapEntry.getValue().paths.forEach(path -> urls.add(BrowserUtils.buildUrl(urlMapEntry.getKey(), path, urlMapEntry.getValue().envMapping))));
        return urls;
    }

    public Optional<CompletableFuture<State>> getCurrentJobStepFuture() {
        return Optional.ofNullable(currentJobStepFuture);
    }

    public Reports getReports() {
        return reports;
    }

    public static Builder runStatusBuilder() {
        return new Builder();
    }

    public static Builder copyOfRunStatusBuilder(JLineupRunStatus jLineupRunStatus) {
        Builder builder = runStatusBuilder()
                .withId(jLineupRunStatus.getId())
                .withJobConfig(jLineupRunStatus.getJobConfig())
                .withState(jLineupRunStatus.getState())
                .withStartTime(jLineupRunStatus.getStartTime())
                .withPauseTime(jLineupRunStatus.pauseTime)
                .withResumeTime(jLineupRunStatus.resumeTime)
                .withReports(jLineupRunStatus.getReports());
        jLineupRunStatus.getEndTime().ifPresent(builder::withEndTime);
        jLineupRunStatus.getCurrentJobStepFuture().ifPresent(builder::withCurrentJobStepFuture);
        return builder;
    }

    @JsonNaming(PropertyNamingStrategy.class)
    public static final class Builder {
        private String id;
        private JobConfig jobConfig;
        private State state;
        private Instant startTime = Instant.now();
        private Instant pauseTime;
        private Instant resumeTime;
        private Instant endTime;
        private CompletableFuture<State> currentJobStepFuture;
        private Reports reports;

        public Builder() {
        }

        public Builder withId(String val) {
            id = val;
            return this;
        }

        public Builder withJobConfig(JobConfig val) {
            jobConfig = val;
            return this;
        }

        public Builder withState(State val) {
            state = val;
            return this;
        }

        public Builder withStartTime(Instant val) {
            startTime = val;
            return this;
        }

        public Builder withEndTime(Instant val) {
            endTime = val;
            return this;
        }

        public Builder withPauseTime(Instant val) {
            pauseTime = val;
            return this;
        }

        public Builder withResumeTime(Instant val) {
            resumeTime = val;
            return this;
        }

        public Builder withCurrentJobStepFuture(CompletableFuture<State> val) {
            currentJobStepFuture = val;
            return this;
        }

        public Builder withReports(Reports val) {
            reports = val;
            return this;
        }

        public JLineupRunStatus build() {
            return new JLineupRunStatus(this);
        }

    }

    @Override
    public String toString() {
        return "JLineupRunStatus{" +
                "id='" + id + '\'' +
                ", state=" + state +
                ", startTime=" + startTime +
                ", pauseTime=" + pauseTime +
                ", resumeTime=" + resumeTime +
                ", endTime=" + endTime +
                ", reports=" + reports +
                ", currentJobStepFuture=" + currentJobStepFuture +
                ", jobConfig=" + jobConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineupRunStatus that = (JLineupRunStatus) o;
        return Objects.equals(id, that.id) &&
                state == that.state &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(pauseTime, that.pauseTime) &&
                Objects.equals(resumeTime, that.resumeTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(reports, that.reports) &&
                Objects.equals(currentJobStepFuture, that.currentJobStepFuture) &&
                Objects.equals(jobConfig, that.jobConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, state, startTime, pauseTime, resumeTime, endTime, reports, currentJobStepFuture, jobConfig);
    }

    @JsonDeserialize(builder = Reports.Builder.class)
    @JsonNaming(PropertyNamingStrategy.class)
    public static class Reports {

        private final String htmlUrl;
        private final String jsonUrl;
        private final String logUrl;

        public Reports(Builder builder) {
            this.htmlUrl = builder.htmlUrl;
            this.jsonUrl = builder.jsonUrl;
            this.logUrl = builder.logUrl;
        }

        @JsonProperty("htmlUrl")
        public String getHtmlUrlFromCurrentContext() {
            if (htmlUrl == null) {
                return null;
            }
            try {
                return fromCurrentContextPath().path(htmlUrl).build().toString();
            } catch (IllegalStateException e) {
                return htmlUrl;
            }
        }

        @JsonProperty("jsonUrl")
        public String getJsonUrlFromCurrentContext() {
            if (jsonUrl == null) {
                return null;
            }
            try {
                return fromCurrentContextPath().path(jsonUrl).build().toString();
            } catch (IllegalStateException e) {
                return jsonUrl;
            }
        }

        @JsonProperty("logUrl")
        public String getLogUrlFromCurrentContext() {
            if (logUrl == null) {
                return null;
            }
            try {
                return fromCurrentContextPath().path(logUrl).build().toString();
            } catch (IllegalStateException e) {
                return logUrl;
            }
        }

        @JsonIgnore
        public String getHtmlUrl() {
            return htmlUrl;
        }

        @JsonIgnore
        public String getJsonUrl() {
            return jsonUrl;
        }

        @JsonIgnore
        public String getLogUrl() {
            return logUrl;
        }

        public static JLineupRunStatus.Reports.Builder reportsBuilder() {
            return new Reports.Builder();
        }

        @Override
        public String toString() {
            return "Reports{" +
                    "htmlUrl='" + htmlUrl + '\'' +
                    ", jsonUrl='" + jsonUrl + '\'' +
                    ", logUrl='" + logUrl + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reports reports = (Reports) o;
            return Objects.equals(htmlUrl, reports.htmlUrl) &&
                    Objects.equals(jsonUrl, reports.jsonUrl) &&
                    Objects.equals(logUrl, reports.logUrl);
        }

        @Override
        public int hashCode() {

            return Objects.hash(htmlUrl, jsonUrl, logUrl);
        }

        @JsonNaming(PropertyNamingStrategy.class)
        public static final class Builder {

            private String htmlUrl;
            private String jsonUrl;
            private String logUrl;

            public JLineupRunStatus.Reports.Builder withHtmlUrl(String htmlUrl) {
                this.htmlUrl = htmlUrl;
                return this;
            }

            public JLineupRunStatus.Reports.Builder withJsonUrl(String jsonUrl) {
                this.jsonUrl = jsonUrl;
                return this;
            }

            public JLineupRunStatus.Reports.Builder withLogUrl(String logUrl) {
                this.logUrl = logUrl;
                return this;
            }

            public Reports build() {
                return new Reports(this);
            }

        }
    }
}
