package de.otto.jlineup.lambda;

import de.otto.jlineup.file.CloudFileStore;
import de.otto.jlineup.file.CloudFileStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LambdaS3FileStoreTest {

    private S3Client s3Client;
    private S3Presigner presigner;
    private LambdaS3FileStore testee;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        // The presigner signs locally, no AWS access needed
        presigner = S3Presigner.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIAEXAMPLE", "secret")))
                .build();
        testee = new LambdaS3FileStore(s3Client, presigner, "jlineup-bucket", "expire-after-7-days/");
    }

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    void shouldBeCreatedByCloudFileStoreFactory() {
        Optional<CloudFileStore> store = CloudFileStoreFactory.createCloudFileStore();
        assertTrue(store.isPresent());
        assertInstanceOf(LambdaS3FileStore.class, store.get());
        store.get().close();
    }

    @Test
    void shouldPrefixKeysWithLambdaPrefix() {
        assertEquals("expire-after-7-days/uploads/abc", testee.fullKey("uploads/abc"));
        assertEquals("prefix/uploads/abc", new LambdaS3FileStore(s3Client, presigner, "b", "prefix").fullKey("uploads/abc"));
        assertEquals("uploads/abc", new LambdaS3FileStore(s3Client, presigner, "b", null).fullKey("uploads/abc"));
        assertEquals("s3://jlineup-bucket/expire-after-7-days/uploads/abc", testee.describe("uploads/abc"));
    }

    @Test
    void shouldPresignPutUrlForPrefixedKey() throws IOException {
        Instant before = Instant.now();

        CloudFileStore.PresignedUpload upload = testee.presignUpload("uploads/abc", Duration.ofMinutes(15));

        assertEquals("PUT", upload.method());
        assertTrue(upload.url().startsWith("https://jlineup-bucket.s3.eu-central-1.amazonaws.com/expire-after-7-days/uploads/abc?"), upload.url());
        assertTrue(upload.url().contains("X-Amz-Expires=900"), upload.url());
        assertFalse(upload.expiresAt().isBefore(before.plus(Duration.ofMinutes(15)).minusSeconds(5)));
    }

    @Test
    void shouldCheckExistence() throws IOException {
        ArgumentCaptor<HeadObjectRequest> request = ArgumentCaptor.forClass(HeadObjectRequest.class);
        when(s3Client.headObject(request.capture())).thenReturn(HeadObjectResponse.builder().build());

        assertTrue(testee.exists("uploads/abc"));
        assertEquals("jlineup-bucket", request.getValue().bucket());
        assertEquals("expire-after-7-days/uploads/abc", request.getValue().key());
    }

    @Test
    void shouldReturnFalseForMissingFiles() throws IOException {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build())
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertFalse(testee.exists("uploads/abc"));
        assertFalse(testee.exists("uploads/abc"));
    }

    @Test
    void shouldFailForOtherErrorsWhenCheckingExistence() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(403).message("Access Denied").build());

        assertThrows(IOException.class, () -> testee.exists("uploads/abc"));
    }

    @Test
    void shouldThrowFileNotFoundForMissingFileWhenOpening() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThrows(FileNotFoundException.class, () -> testee.open("uploads/abc"));
    }

    @Test
    void shouldDeletePrefixedKey() throws IOException {
        testee.delete("uploads/abc");

        verify(s3Client).deleteObject(DeleteObjectRequest.builder().bucket("jlineup-bucket").key("expire-after-7-days/uploads/abc").build());
    }
}
