package de.otto.jlineup.file;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class FileTracker {

    public final JobConfig jobConfig;
    public final Map<Integer, ScreenshotContextFileTracker> contexts;
    public final Map<Step, String> browsers;

    //Used by Jackson
    private FileTracker() {
        jobConfig = null;
        contexts = null;
        browsers = null;
    }

    public FileTracker(JobConfig jobConfig, Map<Integer, ScreenshotContextFileTracker> contexts, Map<Step, String> browsers) {
        this.jobConfig = jobConfig;
        this.contexts = contexts;
        this.browsers = browsers;
    }

    public static FileTracker create(JobConfig jobConfig) {
        return new FileTracker(jobConfig, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public ScreenshotContextFileTracker getScreenshotContextFileTracker(int hash) {
        return contexts.get(hash);
    }

    public Map<Integer, Map<Step, String>> getScreenshotsForContext(int hash) throws IOException {
        ScreenshotContextFileTracker screenshotContextFileTracker = contexts.get(hash);
        if (screenshotContextFileTracker == null) {
            throw new IOException("The files in the working directory don't fit the given config.\n" +
                    "Are you trying to run the 'compare' step for files made with a different config?\n" +
                    "Please run JLineup before and after with the current config before trying again.");
        }
        return screenshotContextFileTracker.screenshots;
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

    public void setBrowserAndVersion(ScreenshotContext screenshotContext, String browserAndVersion) {
        if (browsers != null) {
            browsers.put(screenshotContext.step, browserAndVersion);
        }
    }
}
