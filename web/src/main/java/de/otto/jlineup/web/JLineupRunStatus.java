package de.otto.jlineup.web;

import de.otto.jlineup.config.JobConfig;

import java.time.Instant;
import java.util.Objects;

public class JLineupRunStatus {

    private final String id;
    private final JobConfig jobConfig;
    private final State state;
    private final Instant startTime;
    private final Instant endTime;

    private JLineupRunStatus(Builder builder) {
        id = builder.id;
        jobConfig = builder.jobConfig;
        state = builder.state;
        startTime = builder.startTime;
        endTime = builder.endTime;
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

    public Instant getEndTime() {
        return endTime;
    }

    public static Builder jLineupRunStatusBuilder() {
        return new Builder();
    }

    public static Builder copyOfRunStatusBuilder(JLineupRunStatus jLineupRunStatus) {
        Builder builder = new Builder(jLineupRunStatus.id, jLineupRunStatus.jobConfig, jLineupRunStatus.state, jLineupRunStatus.startTime, jLineupRunStatus.endTime);
        return builder;
    }
    public static final class Builder {
        private String id;
        private JobConfig jobConfig;
        private State state;
        private Instant startTime;
        private Instant endTime;

        public Builder() {
        }

        private Builder(String id, JobConfig jobConfig, State state, Instant startTime, Instant endTime) {
            this.id = id;
            this.jobConfig = jobConfig;
            this.state = state;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Builder withId(String val) {
            id = val;
            return this;
        }

        public Builder withConfig(JobConfig val) {
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
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, jobConfig, state, startTime, endTime);
    }

}
