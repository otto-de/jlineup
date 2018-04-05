package de.otto.jlineup.service;

public class RunNotFoundException extends Exception {

    private final String id;

    public RunNotFoundException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
