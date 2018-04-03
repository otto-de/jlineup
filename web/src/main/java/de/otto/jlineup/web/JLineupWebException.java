package de.otto.jlineup.web;

public class JLineupWebException extends RuntimeException {

    private final int status;

    public JLineupWebException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
