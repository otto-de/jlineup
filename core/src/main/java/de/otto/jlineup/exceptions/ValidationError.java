package de.otto.jlineup.exceptions;

public class ValidationError extends RuntimeException {
    public ValidationError(String message) {
        super("Error during job config validation:\n" + message + "\nPlease check your jlineup job config.");
    }
}
