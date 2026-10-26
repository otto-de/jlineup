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
     * @return a human-readable location of the key for logs, e.g. {@code s3://bucket/prefix/key}
     */
    String describe(String key);

    boolean exists(String key) throws IOException;

    /**
     * @return a URL that can be used by any HTTP client to upload the file without further credentials
     */
    PresignedUpload presignUpload(String key, Duration validity) throws IOException;

    /**
     * @throws java.io.FileNotFoundException if there is no file for the key
     */
    InputStream open(String key) throws IOException;

    void delete(String key) throws IOException;

    @Override
    void close();

    record PresignedUpload(String url, String method, Instant expiresAt) {
    }
}
