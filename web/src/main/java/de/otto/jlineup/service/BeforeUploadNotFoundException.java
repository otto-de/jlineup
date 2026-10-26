package de.otto.jlineup.service;

/**
 * Thrown when a run should be created from an upload that doesn't exist (anymore).
 */
public class BeforeUploadNotFoundException extends Exception {

    private final String uploadId;

    public BeforeUploadNotFoundException(String uploadId) {
        super("Upload '" + uploadId + "' was not found. It was either never uploaded or has expired already.");
        this.uploadId = uploadId;
    }

    public String getUploadId() {
        return uploadId;
    }
}
