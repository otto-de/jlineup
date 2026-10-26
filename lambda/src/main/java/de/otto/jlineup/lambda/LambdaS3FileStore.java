package de.otto.jlineup.lambda;

import de.otto.jlineup.GlobalOption;
import de.otto.jlineup.GlobalOptions;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.file.CloudFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.EnvironmentResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * {@link CloudFileStore} backed by the S3 bucket that the JLineup lambdas use to hand over screenshots.
 * <p>
 * Bucket and prefix are read from the environment of the lambda function (JLINEUP_LAMBDA_S3_BUCKET,
 * JLINEUP_LAMBDA_S3_PREFIX), exactly like {@link LambdaBrowser} does. That way, lifecycle rules configured
 * for the lambda prefix (e.g. expiration) apply to these files as well.
 * <p>
 * Instantiated via reflection by {@link de.otto.jlineup.file.CloudFileStoreFactory}. AWS is contacted lazily
 * on first use, not in the constructor.
 */
public class LambdaS3FileStore implements CloudFileStore {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final Object lock = new Object();

    private DefaultCredentialsProvider credentialsProvider;
    private S3Client s3Client;
    private S3Presigner presigner;
    private String bucket;
    private String prefix;
    private boolean initialized;

    public LambdaS3FileStore() {
    }

    LambdaS3FileStore(S3Client s3Client, S3Presigner presigner, String bucket, String prefix) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.prefix = prefix;
        this.initialized = true;
    }

    private void initialize() throws IOException {
        synchronized (lock) {
            if (initialized) {
                return;
            }
            final Region region = Region.of(GlobalOptions.getOption(GlobalOption.JLINEUP_LAMBDA_AWS_REGION));
            final String functionName = LambdaBrowser.resolveLambdaFunctionName(Browser.Type.CHROME_HEADLESS);
            final DefaultCredentialsProvider cp = DefaultCredentialsProvider.builder().build();
            try (LambdaClient lambdaClient = LambdaClient.builder().credentialsProvider(cp).region(region).build()) {
                EnvironmentResponse environment = lambdaClient.getFunction(r -> r.functionName(functionName)).configuration().environment();
                Map<String, String> variables = environment != null ? environment.variables() : Map.of();
                String configuredBucket = variables.get(GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name());
                if (configuredBucket == null || configuredBucket.isBlank()) {
                    throw new IOException("Lambda function '" + functionName + "' has no " + GlobalOption.JLINEUP_LAMBDA_S3_BUCKET.name() + " configured.");
                }
                bucket = configuredBucket;
                prefix = variables.get(GlobalOption.JLINEUP_LAMBDA_S3_PREFIX.name());
            } catch (SdkException e) {
                cp.close();
                throw new IOException("Could not read S3 configuration of lambda function '" + functionName + "': " + e.getMessage(), e);
            } catch (IOException e) {
                cp.close();
                throw e;
            }
            s3Client = S3Client.builder().credentialsProvider(cp).region(region).build();
            presigner = S3Presigner.builder().credentialsProvider(cp).region(region).build();
            credentialsProvider = cp;
            initialized = true;
            LOG.info("Cloud file store uses s3://{}/{}", bucket, prefix != null ? prefix : "");
        }
    }

    String fullKey(String key) {
        if (prefix == null || prefix.isEmpty()) {
            return key;
        }
        return (prefix.endsWith("/") ? prefix : prefix + "/") + key;
    }

    @Override
    public String describe(String key) {
        return "s3://" + bucket + "/" + fullKey(key);
    }

    @Override
    public boolean exists(String key) throws IOException {
        initialize();
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(fullKey(key)).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new IOException("Could not check " + describe(key) + ": " + e.getMessage(), e);
        } catch (SdkException e) {
            throw new IOException("Could not check " + describe(key) + ": " + e.getMessage(), e);
        }
    }

    @Override
    public PresignedUpload presignUpload(String key, Duration validity) throws IOException {
        initialize();
        try {
            PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(validity)
                    .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(fullKey(key)).build())
                    .build());
            return new PresignedUpload(presigned.url().toString(), presigned.httpRequest().method().name(), presigned.expiration());
        } catch (SdkException e) {
            throw new IOException("Could not create upload URL for " + describe(key) + ": " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream open(String key) throws IOException {
        initialize();
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(fullKey(key)).build());
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(describe(key));
        } catch (SdkException e) {
            throw new IOException("Could not read " + describe(key) + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        initialize();
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(fullKey(key)).build());
        } catch (SdkException e) {
            throw new IOException("Could not delete " + describe(key) + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (presigner != null) presigner.close();
            if (s3Client != null) s3Client.close();
            if (credentialsProvider != null) credentialsProvider.close();
        }
    }
}
