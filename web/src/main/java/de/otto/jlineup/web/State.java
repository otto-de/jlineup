package de.otto.jlineup.web;

public enum State {
    BEFORE_PENDING("'before' pending"),
    BEFORE_RUNNING("'before' running"),
    BEFORE_DONE("'before' done"),
    AFTER_PENDING("'after' pending"),
    AFTER_RUNNING("'after' running"),
    FINISHED_WITHOUT_DIFFERENCES("finished without differences"),
    FINISHED_WITH_DIFFERENCES("finished with differences"),
    ERROR("error"),
    DEAD("dead");

    private final String humanReadableName;

    State(String humanReadableName) {
        this.humanReadableName = humanReadableName;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }
}
