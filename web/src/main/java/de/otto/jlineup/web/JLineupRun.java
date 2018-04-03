package de.otto.jlineup.web;

import de.otto.jlineup.config.Config;

import java.util.Objects;

public class JLineupRun {

    private final String id;
    private final Config config;
    private final State state;


    public static Builder copyOfBuilder(JLineupRun jLineupRun) {
        Builder builder = new Builder(jLineupRun.id, jLineupRun.config, jLineupRun.state);
        return builder;
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

    private JLineupRun(Builder builder) {
        id = builder.id;
        config = builder.config;
        state = builder.state;
    }

    public static Builder jLineupRunBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private String id;
        private Config config;
        private State state;

        public Builder() {
        }

        private Builder(String id, Config config, State state) {
            this.id = id;
            this.config = config;
            this.state = state;
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

        public JLineupRun build() {
            return new JLineupRun(this);
        }
    }



    @Override
    public String toString() {
        return "JLineupRun{" +
                "id='" + id + '\'' +
                ", config=" + config +
                ", state=" + state +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineupRun that = (JLineupRun) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(config, that.config) &&
                state == that.state;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, config, state);
    }
}
