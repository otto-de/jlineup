package de.otto.jlineup.web;

import org.springframework.http.HttpStatus;

public class JLineupWebException extends RuntimeException {

    private final HttpStatus status;

    public JLineupWebException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
