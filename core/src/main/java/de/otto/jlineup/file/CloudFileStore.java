package de.otto.jlineup.file;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

/**
 * Cloud storage (e.g. the S3 bucket shared with the JLineup lambdas) that clients can upload files to
 * directly, without sending them through the JLineup web server.
 * <p>
 * Keys are relative, the implementation adds its own prefix.
 */
public interface CloudFileStore extends AutoCloseable {

    /**
     * Describes the location of a key for log messages.
     *
     * @param key the relative key
     * @return a human-readable location of the key for logs, e.g. {@code s3://bucket/prefix/key}
     */
    String describe(String key);

    /**
     * Checks if a file exists.
     *
     * @param key the relative key
     * @return true if there is a file for the key
     * @throws IOException if the store can't be reached
     */
    boolean exists(String key) throws IOException;

    /**
     * Creates a URL for uploading a single file.
     *
     * @param key      the relative key the file will be stored under
     * @param validity how long the URL can be used
     * @return a URL that can be used by any HTTP client to upload the file without further credentials
     * @throws IOException if the URL can't be created
     */
    PresignedUpload presignUpload(String key, Duration validity) throws IOException;

    /**
     * Opens a stored file for reading. The caller has to close the stream.
     *
     * @param key the relative key
     * @return the content of the file
     * @throws java.io.FileNotFoundException if there is no file for the key
     * @throws IOException                   if the file can't be read
     */
    InputStream open(String key) throws IOException;

    /**
     * Deletes a stored file.
     *
     * @param key the relative key
     * @throws IOException if the file can't be deleted
     */
    void delete(String key) throws IOException;

    /**
     * Releases the connections to the storage. Does not throw checked exceptions.
     */
    @Override
    void close();

    /**
     * A URL to upload one file to with a plain HTTP request.
     *
     * @param url       the URL to send the request to
     * @param method    the HTTP method the request has to use, i.e. {@code PUT}
     * @param expiresAt the time after which the URL stops working
     */
    record PresignedUpload(String url, String method, Instant expiresAt) {
    }
}
