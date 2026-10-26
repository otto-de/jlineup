package de.otto.jlineup.web.configuration;

import java.time.Duration;
import java.util.Objects;

/**
 * Limits for importing an already completed 'before' run (uploaded as archive).
 */
public class JLineupWebImportProperties {

    public static final long DEFAULT_MAX_UNCOMPRESSED_SIZE_BYTES = 2L * 1024 * 1024 * 1024; // 2 GiB
    public static final int DEFAULT_MAX_ENTRIES = 50_000;
    public static final Duration DEFAULT_UPLOAD_URL_VALIDITY = Duration.ofMinutes(15);

    private long maxUncompressedSizeBytes = DEFAULT_MAX_UNCOMPRESSED_SIZE_BYTES;
    private int maxEntries = DEFAULT_MAX_ENTRIES;
    // How long a pre-signed upload URL (POST /uploads) is valid
    private Duration uploadUrlValidity = DEFAULT_UPLOAD_URL_VALIDITY;

    public long getMaxUncompressedSizeBytes() {
        return maxUncompressedSizeBytes;
    }

    public void setMaxUncompressedSizeBytes(long maxUncompressedSizeBytes) {
        this.maxUncompressedSizeBytes = maxUncompressedSizeBytes;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Duration getUploadUrlValidity() {
        return uploadUrlValidity;
    }

    public void setUploadUrlValidity(Duration uploadUrlValidity) {
        this.uploadUrlValidity = uploadUrlValidity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineupWebImportProperties that = (JLineupWebImportProperties) o;
        return maxUncompressedSizeBytes == that.maxUncompressedSizeBytes && maxEntries == that.maxEntries && Objects.equals(uploadUrlValidity, that.uploadUrlValidity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxUncompressedSizeBytes, maxEntries, uploadUrlValidity);
    }

    @Override
    public String toString() {
        return "JLineupWebImportProperties{" +
                "maxUncompressedSizeBytes=" + maxUncompressedSizeBytes +
                ", maxEntries=" + maxEntries +
                ", uploadUrlValidity=" + uploadUrlValidity +
                '}';
    }
}
