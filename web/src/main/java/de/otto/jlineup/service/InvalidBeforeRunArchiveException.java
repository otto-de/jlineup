package de.otto.jlineup.service;

/**
 * Thrown when an uploaded 'before' run archive is malformed or unsafe
 * (e.g. unreadable archive, missing files.json, path traversal, size limits exceeded).
 */
public class InvalidBeforeRunArchiveException extends Exception {

    public InvalidBeforeRunArchiveException(String message) {
        super(message);
    }

    public InvalidBeforeRunArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
