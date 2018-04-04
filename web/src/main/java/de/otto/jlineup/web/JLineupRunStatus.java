package de.otto.jlineup.web;

import de.otto.jlineup.config.Config;

import java.time.Instant;
import java.util.Objects;

public class JLineupRunStatus {

    private final String id;
    private final Config config;
    private final State state;
    private final Instant startTime;
    private final Instant endTime;

    private JLineupRunStatus(Builder builder) {
        id = builder.id;
        config = builder.config;
        state = builder.state;
        startTime = builder.startTime;
        endTime = builder.endTime;
    }

    public String getId() {
        return id;
    }

    public Config getConfig() {
        return config;
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
        Builder builder = new Builder(jLineupRunStatus.id, jLineupRunStatus.config, jLineupRunStatus.state, jLineupRunStatus.startTime, jLineupRunStatus.endTime);
        return builder;
    }
    public static final class Builder {
        private String id;
        private Config config;
        private State state;
        private Instant startTime;
        private Instant endTime;

        public Builder() {
        }

        private Builder(String id, Config config, State state, Instant startTime, Instant endTime) {
            this.id = id;
            this.config = config;
            this.state = state;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Builder withId(String val) {
            id = val;
            return this;
        }

        public Builder withConfig(Config val) {
            config = val;
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
                ", config=" + config +
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
                Objects.equals(config, that.config) &&
                state == that.state &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, config, state, startTime, endTime);
    }

}
