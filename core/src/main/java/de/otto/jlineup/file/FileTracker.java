package de.otto.jlineup.file;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class FileTracker {

    public final JobConfig jobConfig;
    public final Map<Integer, ScreenshotContextFileTracker> contexts;

    //Used by Jackson
    private FileTracker() {
        jobConfig = null;
        contexts = null;
    }

    public FileTracker(JobConfig jobConfig, Map<Integer, ScreenshotContextFileTracker> contexts) {
        this.jobConfig = jobConfig;
        this.contexts = contexts;
    }

    public static FileTracker create(JobConfig jobConfig) {
        return new FileTracker(jobConfig, new ConcurrentHashMap<>());
    }

    public ScreenshotContextFileTracker getScreenshotContextFileTracker(int hash) {
        return contexts.get(hash);
    }

    public Map<Integer, Map<Step, String>> getScreenshotsForContext(int hash) {
        return contexts.get(hash).screenshots;
    }

    public void addScreenshot(final ScreenshotContext screenshotContext, final String path, final int yPosition) {
        ScreenshotContextFileTracker screenshotContextFileTracker = contexts.get(screenshotContext.contextHash());
        if (screenshotContextFileTracker == null) {
            ScreenshotContextFileTracker fileTrackerToPut = new ScreenshotContextFileTracker(screenshotContext);
            screenshotContextFileTracker = contexts.putIfAbsent(screenshotContext.contextHash(), fileTrackerToPut);
            if (screenshotContextFileTracker == null) {
                screenshotContextFileTracker = fileTrackerToPut;
            }
        }
        screenshotContextFileTracker.addScreenshot(screenshotContext, path, yPosition);
    }
}
