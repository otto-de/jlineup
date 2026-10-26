package de.otto.jlineup.web;

import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Response of POST /uploads.
 *
 * @param uploadId     id to pass as 'beforeUploadId' when creating the run
 * @param exists       true if an upload with the given reference hash exists already, no upload necessary
 * @param uploadUrl    pre-signed URL to upload the archive to (null if exists is true)
 * @param uploadMethod HTTP method to use for uploadUrl (null if exists is true)
 * @param expiresAt    ISO-8601 timestamp until which uploadUrl can be used (null if exists is true)
 */
@JsonNaming(PropertyNamingStrategy.class)
public record BeforeUploadResponse(String uploadId, boolean exists, String uploadUrl, String uploadMethod, String expiresAt) {
}
