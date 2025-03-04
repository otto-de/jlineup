package de.otto.jlineup.browser;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.invoke.MethodHandles.lookup;

public class CloudBrowserFactory {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

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
