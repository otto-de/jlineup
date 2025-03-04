package de.otto.jlineup.file;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@JsonDeserialize(builder = FileTracker.Builder.class)
public class FileTracker {

    public final JobConfig jobConfig;
    @JsonMerge
    public final ConcurrentHashMap<Integer, ScreenshotContextFileTracker> contexts;
    public final ConcurrentHashMap<BrowserStep, String> browsers;

    //Used by Jackson
    private FileTracker() {
        jobConfig = null;
        contexts = null;
        browsers = null;
    }

    public FileTracker(JobConfig jobConfig, ConcurrentHashMap<Integer, ScreenshotContextFileTracker> contexts, ConcurrentHashMap<BrowserStep, String> browsers) {
        this.jobConfig = jobConfig;
        this.contexts = contexts;
        this.browsers = browsers;
    }

    private FileTracker(Builder builder) {
        jobConfig = builder.jobConfig;
        contexts = builder.contexts;
        browsers = builder.browsers;
    }

    public static Builder fileTrackerBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(FileTracker copy) {
        Builder builder = new Builder();
        builder.jobConfig = copy.getJobConfig();
        builder.contexts = copy.getContexts();
        builder.browsers = copy.getBrowsers();
        return builder;
    }

    /*
     *
     *
     *
     *  BEGIN of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    @JsonProperty("job-config")
    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public ConcurrentHashMap<Integer, ScreenshotContextFileTracker> getContexts() {
        return contexts;
    }

    public ConcurrentHashMap<BrowserStep, String> getBrowsers() {
        return browsers;
    }

    /*
     *
     *
     *
     *  END of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    public static FileTracker create(JobConfig jobConfig) {
        return new FileTracker(jobConfig, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public ScreenshotContextFileTracker getScreenshotContextFileTracker(int hash) {
        assert contexts != null;
        return contexts.get(hash);
    }

    public Map<Integer, Map<BrowserStep, String>> getScreenshotsForContext(int hash) throws IOException {
        assert contexts != null;
        ScreenshotContextFileTracker screenshotContextFileTracker = contexts.get(hash);
        if (screenshotContextFileTracker == null) {
            throw new IOException("""
                    The files in the working directory don't fit the given config.
                    Are you trying to run the 'compare' step for files made with a different config?
                    Please run JLineup before and after with the current config before trying again.""");
        }
        return screenshotContextFileTracker.screenshots;
    }

    public void addScreenshot(final ScreenshotContext screenshotContext, final String path, final int yPosition) {
        assert contexts != null;
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

    public boolean isContextAlreadyThere(final ScreenshotContext screenshotContext) {
        assert contexts != null;
        return contexts.get(screenshotContext.contextHash()) != null;
    }

    public void setBrowserAndVersion(ScreenshotContext screenshotContext, String browserAndVersion) {
        if (browsers != null) {
            browsers.put(screenshotContext.step, browserAndVersion);
        }
    }

    public void removeContext(ScreenshotContext screenshotContext) {
        assert contexts != null;
        contexts.remove(screenshotContext.contextHash());
    }


    public static final class Builder {
        private JobConfig jobConfig;
        private ConcurrentHashMap<Integer, ScreenshotContextFileTracker> contexts;
        private ConcurrentHashMap<BrowserStep, String> browsers;

        private Builder() {
        }

        public Builder withJobConfig(JobConfig val) {
            jobConfig = val;
            return this;
        }

        public Builder withContexts(ConcurrentHashMap<Integer, ScreenshotContextFileTracker> val) {
            contexts = val;
            return this;
        }

        public Builder withBrowsers(ConcurrentHashMap<BrowserStep, String> val) {
            browsers = val;
            return this;
        }

        public FileTracker build() {
            return new FileTracker(this);
        }
    }

    @Override
    public String toString() {
        return "FileTracker{" +
                "jobConfig=" + jobConfig +
                ", contexts=" + contexts +
                ", browsers=" + browsers +
                '}';
    }
}
