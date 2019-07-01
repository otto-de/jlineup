package de.otto.jlineup.file;

import de.otto.jlineup.config.JobConfig;

import java.util.concurrent.ConcurrentHashMap;

public class FileTracker {

    private final JobConfig jobConfig;
    private final ConcurrentHashMap<String, FileTrackerForScreenshotContext> contexts;

    public FileTracker(JobConfig jobConfig, ConcurrentHashMap<String, FileTrackerForScreenshotContext> contexts) {
        this.jobConfig = jobConfig;
        this.contexts = contexts;
    }

    public FileTracker create(JobConfig jobConfig) {
        return new FileTracker(jobConfig, new ConcurrentHashMap<>());

    }
}
