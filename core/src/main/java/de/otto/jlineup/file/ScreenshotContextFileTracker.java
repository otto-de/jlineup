package de.otto.jlineup.file;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.Step;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@JsonDeserialize(builder = ScreenshotContextFileTracker.Builder.class)
public class ScreenshotContextFileTracker {

    public final ScreenshotContext screenshotContext;
    public final ConcurrentSkipListMap<Integer, Map<Step, String>> screenshots;

    ScreenshotContextFileTracker(ScreenshotContext screenshotContext) {
        this.screenshotContext = screenshotContext;
        this.screenshots = new ConcurrentSkipListMap<>();
    }

    private ScreenshotContextFileTracker(Builder builder) {
        screenshotContext = builder.screenshotContext;
        screenshots = builder.screenshots;
    }

    public static Builder screenshotContextFileTrackerBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(ScreenshotContextFileTracker copy) {
        Builder builder = new Builder();
        builder.screenshotContext = copy.getScreenshotContext();
        builder.screenshots = copy.getScreenshots();
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

    public ScreenshotContext getScreenshotContext() {
        return screenshotContext;
    }

    public ConcurrentSkipListMap<Integer, Map<Step, String>> getScreenshots() {
        return screenshots;
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

    void addScreenshot(ScreenshotContext screenshotContext, String path, int yPosition) {
        Map<Step, String> stepsToPathsMap = screenshots.get(yPosition);
        if (stepsToPathsMap == null) {
            Map<Step, String> mapToPut = new HashMap<>();
            stepsToPathsMap = screenshots.putIfAbsent(yPosition, mapToPut);
            if (stepsToPathsMap == null) {
                stepsToPathsMap = mapToPut;
            }
        }
        stepsToPathsMap.put(screenshotContext.step, path);
    }


    public static final class Builder {
        private ScreenshotContext screenshotContext;
        private ConcurrentSkipListMap<Integer, Map<Step, String>> screenshots;

        private Builder() {
        }

        public Builder withScreenshotContext(ScreenshotContext val) {
            screenshotContext = val;
            return this;
        }

        public Builder withScreenshots(ConcurrentSkipListMap<Integer, Map<Step, String>> val) {
            screenshots = val;
            return this;
        }

        public ScreenshotContextFileTracker build() {
            return new ScreenshotContextFileTracker(this);
        }
    }
}
