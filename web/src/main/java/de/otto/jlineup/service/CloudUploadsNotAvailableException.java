package de.otto.jlineup.service;

/**
 * Thrown when direct uploads (POST /uploads) are requested, but no cloud file store is available,
 * e.g. because the server runs without the lambda module (jlineup-web instead of jlineup-web-lambda).
 */
public class CloudUploadsNotAvailableException extends Exception {

    public CloudUploadsNotAvailableException(String message) {
        super(message);
    }
}
