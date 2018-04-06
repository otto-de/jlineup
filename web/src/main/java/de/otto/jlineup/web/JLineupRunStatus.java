package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JLineupRunStatus {

    private final String id;
    private final JobConfig jobConfig;
    private final State state;
    private final Instant startTime;
    private final Instant endTime;
    private final CompletableFuture<State> currentJobStepFuture;

    private JLineupRunStatus(Builder builder) {
        id = builder.id;
        jobConfig = builder.jobConfig;
        state = builder.state;
        startTime = builder.startTime;
        endTime = builder.endTime;
        currentJobStepFuture = builder.currentJobStepFuture;
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

    public Optional<Instant> getStartTime() {
        return Optional.ofNullable(startTime);
    }

    public Optional<Instant> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    public Optional<CompletableFuture<State>> getCurrentJobStepFuture() {
        return Optional.ofNullable(currentJobStepFuture);
    }

    public static Builder runStatusBuilder() {
        return new Builder();
    }

    public static Builder copyOfRunStatusBuilder(JLineupRunStatus jLineupRunStatus) {
        Builder builder = runStatusBuilder()
                .withId(jLineupRunStatus.getId())
                .withJobConfig(jLineupRunStatus.getJobConfig())
                .withState(jLineupRunStatus.getState());
        jLineupRunStatus.getStartTime().ifPresent(builder::withStartTime);
        jLineupRunStatus.getEndTime().ifPresent(builder::withEndTime);
        jLineupRunStatus.getCurrentJobStepFuture().ifPresent(builder::withCurrentJobStepFuture);
        return builder;
    }

    public static final class Builder {
        private String id;
        private JobConfig jobConfig;
        private State state;
        private Instant startTime;
        private Instant endTime;
        private CompletableFuture<State> currentJobStepFuture;

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
                Objects.equals(currentJobStepFuture, that.currentJobStepFuture);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, jobConfig, state, startTime, endTime, currentJobStepFuture);
    }
}
