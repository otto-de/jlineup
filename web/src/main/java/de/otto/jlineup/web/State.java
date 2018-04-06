package de.otto.jlineup.web;

public enum State {
    BEFORE_PENDING,
    BEFORE_RUNNING,
    BEFORE_DONE,
    AFTER_PENDING,
    AFTER_RUNNING,
    FINISHED,
    ERROR,
    DEAD
}
