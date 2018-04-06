package de.otto.jlineup.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.otto.jlineup.config.JobConfig;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JsonDeserialize(builder = JLineupRunStatus.Builder.class)
public class JLineupRunStatus {

    private final String id;
    private final State state;
    private final Instant startTime;
    private final Instant endTime;
    private final Reports reports;

    @JsonIgnore
    private final CompletableFuture<State> currentJobStepFuture;
    @JsonIgnore
    private final JobConfig jobConfig;

    private JLineupRunStatus(Builder builder) {
        id = builder.id;
        jobConfig = builder.jobConfig;
        state = builder.state;
        startTime = builder.startTime;
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
                .withStartTime(jLineupRunStatus.getStartTime());
        jLineupRunStatus.getEndTime().ifPresent(builder::withEndTime);
        jLineupRunStatus.getCurrentJobStepFuture().ifPresent(builder::withCurrentJobStepFuture);
        return builder;
    }

    public static final class Builder {
        private String id;
        private JobConfig jobConfig;
        private State state;
        private Instant startTime = Instant.now();
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
                ", jobConfig=" + jobConfig +
                ", state=" + state +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", reports=" + reports +
                ", currentJobStepFuture=" + currentJobStepFuture +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineupRunStatus that = (JLineupRunStatus) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(jobConfig, that.jobConfig) &&
                state == that.state &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(reports, that.reports) &&
                Objects.equals(currentJobStepFuture, that.currentJobStepFuture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobConfig, state, startTime, endTime, reports, currentJobStepFuture);
    }


    @JsonDeserialize(builder = Reports.Builder.class)
    public static class Reports {

        private final String htmlUrl;
        private final String jsonUrl;

        public Reports(Builder builder) {
            this.htmlUrl = builder.htmlUrl;
            this.jsonUrl = builder.jsonUrl;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public String getJsonUrl() {
            return jsonUrl;
        }

        public static JLineupRunStatus.Reports.Builder reportsBuilder() {
            return new Reports.Builder();
        }

        @Override
        public String toString() {
            return "Reports{" +
                    "htmlUrl='" + htmlUrl + '\'' +
                    ", jsonUrl='" + jsonUrl + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reports reports = (Reports) o;
            return Objects.equals(htmlUrl, reports.htmlUrl) &&
                    Objects.equals(jsonUrl, reports.jsonUrl);
        }

        @Override
        public int hashCode() {

            return Objects.hash(htmlUrl, jsonUrl);
        }

        public static final class Builder {

            private String htmlUrl;
            private String jsonUrl;

            public JLineupRunStatus.Reports.Builder withHtmlUrl(String htmlUrl) {
                this.htmlUrl = htmlUrl;
                return this;
            }

            public JLineupRunStatus.Reports.Builder withJsonUrl(String jsonUrl) {
                this.jsonUrl = jsonUrl;
                return this;
            }

            public Reports build() {
                return new Reports(this);
            }

        }
    }
}
