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

    public boolean isDone() {
        return this == FINISHED_WITH_DIFFERENCES || this == FINISHED_WITHOUT_DIFFERENCES || this == ERROR || this == DEAD;
    }

    public boolean isNonPersistable() {
        return this == BEFORE_PENDING || this == BEFORE_RUNNING || this == AFTER_PENDING || this == AFTER_RUNNING;
    }

    /**
     * Returns true if the run failed after the 'before' step completed,
     * meaning it can be retried from the 'after' step.
     */
    public boolean isRetryableForAfter() {
        return this == ERROR || this == DEAD || this == FINISHED_WITH_DIFFERENCES;
    }

    /**
     * Returns true if this run's 'before' screenshots can be reused for a
     * new after+compare run. This includes any state where the before step
     * has completed: BEFORE_DONE plus all terminal states.
     */
    public boolean isRerunnableForAfter() {
        return this == BEFORE_DONE || isDone();
    }
}
