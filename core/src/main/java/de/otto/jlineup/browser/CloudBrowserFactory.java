package de.otto.jlineup.browser;

import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.invoke.MethodHandles.lookup;

public class CloudBrowserFactory {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static CloudBrowser createCloudBrowser(JobConfig jobConfig, FileService fileService) {
        try {
            Class<?> lambdaBrowserClass = Class.forName("de.otto.jlineup.lambda.LambdaBrowser");
            Constructor<?> lambdaBrowserConstructor = lambdaBrowserClass.getConstructor(JobConfig.class, FileService.class);
            return (CloudBrowser) lambdaBrowserConstructor.newInstance(jobConfig, fileService);

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOG.error("No LambdaBrowser reachable.", e);
        }

        throw new RuntimeException("Could not create any CloudBrowser instance.");
    }

}
