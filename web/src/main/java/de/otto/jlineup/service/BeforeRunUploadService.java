package de.otto.jlineup.service;

import de.otto.jlineup.file.CloudFileStore;
import de.otto.jlineup.file.CloudFileStoreFactory;
import de.otto.jlineup.web.BeforeUploadResponse;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Lets clients upload 'before' run archives directly to cloud storage (the S3 bucket shared with the lambdas)
 * instead of sending them through the web server. This is much faster if the web server can only be reached
 * through a slow connection (e.g. an SSM tunnel).
 * <p>
 * Optionally, a reference hash (e.g. the git tree hash of a checked-in reference) can be given. The upload is
 * then stored under a key derived from that hash, and later requests with the same hash can reuse it without
 * uploading again - as long as the file hasn't been removed by the bucket's lifecycle rules.
 */
@Service
public class BeforeRunUploadService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    static final String UPLOAD_KEY_PREFIX = "jlineup-before-uploads/";
    static final String REFERENCE_UPLOAD_ID_PREFIX = "ref-";

    private static final Pattern REFERENCE_HASH = Pattern.compile("^[0-9a-f]{7,64}$");
    private static final Pattern UPLOAD_ID = Pattern.compile("^(ref-[0-9a-f]{7,64}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$");

    private final JLineupWebProperties properties;
    private final Optional<CloudFileStore> cloudFileStore;

    @Autowired
    public BeforeRunUploadService(JLineupWebProperties properties) {
        this(properties, CloudFileStoreFactory.createCloudFileStore());
    }

    BeforeRunUploadService(JLineupWebProperties properties, Optional<CloudFileStore> cloudFileStore) {
        this.properties = properties;
        this.cloudFileStore = cloudFileStore;
        LOG.info("Direct uploads of 'before' runs to cloud storage are {}.", cloudFileStore.isPresent() ? "available" : "not available (no lambda module on classpath)");
    }

    /**
     * @param referenceHash optional hash identifying the content. If an upload with this hash exists already,
     *                      no upload URL is created and the existing upload can be used right away.
     */
    public BeforeUploadResponse createUpload(String referenceHash) throws CloudUploadsNotAvailableException, IOException {
        final CloudFileStore store = store();

        final String uploadId;
        if (referenceHash != null && !referenceHash.isBlank()) {
            String normalizedHash = referenceHash.trim().toLowerCase(Locale.ROOT);
            if (!REFERENCE_HASH.matcher(normalizedHash).matches()) {
                throw new IllegalArgumentException("Reference hash must consist of 7 to 64 hex characters (e.g. a git hash), but was '" + referenceHash + "'.");
            }
            uploadId = REFERENCE_UPLOAD_ID_PREFIX + normalizedHash;
            if (store.exists(keyFor(uploadId))) {
                LOG.info("Upload '{}' exists already at {}, no upload necessary.", uploadId, store.describe(keyFor(uploadId)));
                return new BeforeUploadResponse(uploadId, true, null, null, null);
            }
        } else {
            uploadId = UUID.randomUUID().toString();
        }

        CloudFileStore.PresignedUpload presignedUpload = store.presignUpload(keyFor(uploadId), properties.getImport().getUploadUrlValidity());
        LOG.info("Created upload URL for '{}' ({}), valid until {}.", uploadId, store.describe(keyFor(uploadId)), presignedUpload.expiresAt());
        return new BeforeUploadResponse(uploadId, false, presignedUpload.url(), presignedUpload.method(), presignedUpload.expiresAt().toString());
    }

    public InputStream openUpload(String uploadId) throws CloudUploadsNotAvailableException, BeforeUploadNotFoundException, IOException {
        final CloudFileStore store = store();
        validateUploadId(uploadId);
        try {
            LOG.info("Reading upload '{}' from {}", uploadId, store.describe(keyFor(uploadId)));
            return store.open(keyFor(uploadId));
        } catch (FileNotFoundException e) {
            throw new BeforeUploadNotFoundException(uploadId);
        }
    }

    /**
     * Deletes one-time uploads after they were imported. Uploads identified by a reference hash are kept,
     * so they can be reused - they are removed by the bucket's lifecycle rules.
     */
    public void discardUpload(String uploadId) {
        if (uploadId == null || uploadId.startsWith(REFERENCE_UPLOAD_ID_PREFIX) || cloudFileStore.isEmpty()) {
            return;
        }
        try {
            validateUploadId(uploadId);
            cloudFileStore.get().delete(keyFor(uploadId));
        } catch (Exception e) {
            LOG.warn("Could not delete upload '{}', it will be removed by the bucket's lifecycle rules (if configured).", uploadId, e);
        }
    }

    private CloudFileStore store() throws CloudUploadsNotAvailableException {
        return cloudFileStore.orElseThrow(() -> new CloudUploadsNotAvailableException(
                "Direct uploads are not available on this server, because it runs without the JLineup lambda module (use jlineup-web-lambda). Upload the archive as multipart 'before' part to POST /runs instead."));
    }

    private static void validateUploadId(String uploadId) {
        if (uploadId == null || !UPLOAD_ID.matcher(uploadId).matches()) {
            throw new IllegalArgumentException("Invalid upload id '" + uploadId + "'.");
        }
    }

    static String keyFor(String uploadId) {
        return UPLOAD_KEY_PREFIX + uploadId;
    }

    @PreDestroy
    public void close() {
        cloudFileStore.ifPresent(CloudFileStore::close);
    }
}
