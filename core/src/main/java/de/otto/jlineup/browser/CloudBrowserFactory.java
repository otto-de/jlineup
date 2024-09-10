package de.otto.jlineup.browser;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.Scanners;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;
import static org.reflections.scanners.Scanners.*;

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
