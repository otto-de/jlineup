package de.otto.jlineup.browser;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Creates the {@link CloudBrowser} of the lambda module, if it is on the classpath. The class is loaded by name, so
 * that modules without AWS dependencies don't need a compile time dependency to the lambda module.
 */
public class CloudBrowserFactory {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    /**
     * Creates a {@code de.otto.jlineup.lambda.LambdaBrowser} via reflection.
     *
     * @param runStepConfig the configuration of the current run step
     * @param jobConfig     the job configuration
     * @param fileService   the file service the browser stores the downloaded screenshots with
     * @return the cloud browser
     * @throws ClassNotFoundException if the lambda module is not available, which means a locally installed browser
     *                                has to be used
     */
    public static CloudBrowser createCloudBrowser(RunStepConfig runStepConfig, JobConfig jobConfig, FileService fileService) throws ClassNotFoundException {
        try {
            Class<?> lambdaBrowserClass = Class.forName("de.otto.jlineup.lambda.LambdaBrowser", false, CloudBrowserFactory.class.getClassLoader());
            Constructor<?> lambdaBrowserConstructor = lambdaBrowserClass.getConstructor(RunStepConfig.class, JobConfig.class, FileService.class);
            return (CloudBrowser) lambdaBrowserConstructor.newInstance(runStepConfig, jobConfig, fileService);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOG.debug("No LambdaBrowser reachable.", e);
        }
        throw new ClassNotFoundException("Using a locally installed browser.");
    }

}
