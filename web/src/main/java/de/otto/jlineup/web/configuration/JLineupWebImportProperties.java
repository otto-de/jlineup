package de.otto.jlineup.web.configuration;

import java.util.Objects;

/**
 * Limits for importing an already completed 'before' run (uploaded as archive).
 */
public class JLineupWebImportProperties {

    public static final long DEFAULT_MAX_UNCOMPRESSED_SIZE_BYTES = 2L * 1024 * 1024 * 1024; // 2 GiB
    public static final int DEFAULT_MAX_ENTRIES = 50_000;

    private long maxUncompressedSizeBytes = DEFAULT_MAX_UNCOMPRESSED_SIZE_BYTES;
    private int maxEntries = DEFAULT_MAX_ENTRIES;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JLineupWebImportProperties that = (JLineupWebImportProperties) o;
        return maxUncompressedSizeBytes == that.maxUncompressedSizeBytes && maxEntries == that.maxEntries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxUncompressedSizeBytes, maxEntries);
    }

    @Override
    public String toString() {
        return "JLineupWebImportProperties{" +
                "maxUncompressedSizeBytes=" + maxUncompressedSizeBytes +
                ", maxEntries=" + maxEntries +
                '}';
    }
}
