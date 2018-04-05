package de.otto.jlineup.service;

import de.otto.jlineup.web.State;

public class InvalidRunStateException extends Exception {

    private final String id;
    private final State currentState;
    private final State expectedState;

    public InvalidRunStateException(String id, State currentState, State expectedState) {
        this.id = id;
        this.currentState = currentState;
        this.expectedState = expectedState;
    }

    public String getId() {
        return id;
    }

    public State getCurrentState() {
        return currentState;
    }

    public State getExpectedState() {
        return expectedState;
    }

}
