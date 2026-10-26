package de.otto.jlineup.service;

import de.otto.jlineup.file.CloudFileStore;
import de.otto.jlineup.web.BeforeUploadResponse;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BeforeRunUploadServiceTest {

    private static final String GIT_TREE_HASH = "3f2c1a9e8b7d6c5f4e3d2c1b0a9f8e7d6c5b4a39";

    private InMemoryCloudFileStore store;
    private BeforeRunUploadService testee;

    @BeforeEach
    void setUp() {
        store = new InMemoryCloudFileStore();
        testee = new BeforeRunUploadService(new JLineupWebProperties(), Optional.of(store));
    }

    @Test
    void shouldCreateOneTimeUploadWithoutReferenceHash() throws Exception {
        BeforeUploadResponse response = testee.createUpload(null);

        assertFalse(response.exists());
        assertTrue(response.uploadId().matches("[0-9a-f-]{36}"));
        assertEquals("PUT", response.uploadMethod());
        assertEquals("https://upload/jlineup-before-uploads/" + response.uploadId(), response.uploadUrl());
        assertEquals(Duration.ofMinutes(15), store.lastValidity);
        assertNotNull(response.expiresAt());
    }

    @Test
    void shouldCreateUploadForUnknownReferenceHash() throws Exception {
        BeforeUploadResponse response = testee.createUpload(GIT_TREE_HASH.toUpperCase());

        assertFalse(response.exists());
        assertEquals("ref-" + GIT_TREE_HASH, response.uploadId());
        assertEquals("https://upload/jlineup-before-uploads/ref-" + GIT_TREE_HASH, response.uploadUrl());
    }

    @Test
    void shouldReuseExistingUploadForKnownReferenceHash() throws Exception {
        store.files.put("jlineup-before-uploads/ref-" + GIT_TREE_HASH, "archive");

        BeforeUploadResponse response = testee.createUpload(GIT_TREE_HASH);

        assertTrue(response.exists());
        assertEquals("ref-" + GIT_TREE_HASH, response.uploadId());
        assertNull(response.uploadUrl());
    }

    @Test
    void shouldRejectInvalidReferenceHash() {
        assertThrows(IllegalArgumentException.class, () -> testee.createUpload("../../etc"));
        assertThrows(IllegalArgumentException.class, () -> testee.createUpload("abc"));
    }

    @Test
    void shouldOpenUpload() throws Exception {
        store.files.put("jlineup-before-uploads/ref-" + GIT_TREE_HASH, "archive");

        try (InputStream in = testee.openUpload("ref-" + GIT_TREE_HASH)) {
            assertEquals("archive", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldFailForUnknownOrInvalidUploadIds() {
        assertThrows(BeforeUploadNotFoundException.class, () -> testee.openUpload("ref-" + GIT_TREE_HASH));
        assertThrows(IllegalArgumentException.class, () -> testee.openUpload("../files.json"));
        assertThrows(IllegalArgumentException.class, () -> testee.openUpload(null));
    }

    @Test
    void shouldDeleteOneTimeUploadsButKeepReferenceUploads() throws Exception {
        String oneTimeId = testee.createUpload(null).uploadId();
        store.files.put("jlineup-before-uploads/" + oneTimeId, "archive");
        store.files.put("jlineup-before-uploads/ref-" + GIT_TREE_HASH, "archive");

        testee.discardUpload(oneTimeId);
        testee.discardUpload("ref-" + GIT_TREE_HASH);

        assertFalse(store.files.containsKey("jlineup-before-uploads/" + oneTimeId));
        assertTrue(store.files.containsKey("jlineup-before-uploads/ref-" + GIT_TREE_HASH));
    }

    @Test
    void shouldReportUnavailabilityWithoutCloudFileStore() {
        BeforeRunUploadService withoutStore = new BeforeRunUploadService(new JLineupWebProperties(), Optional.empty());

        assertThrows(CloudUploadsNotAvailableException.class, () -> withoutStore.createUpload(null));
        assertThrows(CloudUploadsNotAvailableException.class, () -> withoutStore.openUpload("ref-" + GIT_TREE_HASH));
        withoutStore.discardUpload("ref-" + GIT_TREE_HASH);
    }

    private static class InMemoryCloudFileStore implements CloudFileStore {

        final Map<String, String> files = new HashMap<>();
        Duration lastValidity;

        @Override
        public String describe(String key) {
            return "mem://" + key;
        }

        @Override
        public boolean exists(String key) {
            return files.containsKey(key);
        }

        @Override
        public PresignedUpload presignUpload(String key, Duration validity) {
            lastValidity = validity;
            return new PresignedUpload("https://upload/" + key, "PUT", Instant.now().plus(validity));
        }

        @Override
        public InputStream open(String key) throws IOException {
            String content = files.get(key);
            if (content == null) {
                throw new FileNotFoundException(key);
            }
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void delete(String key) {
            files.remove(key);
        }

        @Override
        public void close() {
        }
    }
}
