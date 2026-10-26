package de.otto.jlineup.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Creates the {@link CloudFileStore} of the lambda module, if it's on the classpath (e.g. in jlineup-web-lambda).
 * Works like {@link de.otto.jlineup.browser.CloudBrowserFactory}, so that modules without AWS dependencies
 * (e.g. jlineup-web) don't need a compile time dependency to the lambda module.
 */
public class CloudFileStoreFactory {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    static final String LAMBDA_FILE_STORE_CLASS = "de.otto.jlineup.lambda.LambdaS3FileStore";

    public static Optional<CloudFileStore> createCloudFileStore() {
        try {
            Class<?> fileStoreClass = Class.forName(LAMBDA_FILE_STORE_CLASS, false, CloudFileStoreFactory.class.getClassLoader());
            return Optional.of((CloudFileStore) fileStoreClass.getConstructor().newInstance());
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.debug("No cloud file store reachable.", e);
            return Optional.empty();
        }
    }
}
