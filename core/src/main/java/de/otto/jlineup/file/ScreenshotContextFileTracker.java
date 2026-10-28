package de.otto.jlineup.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.annotation.JsonDeserialize;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@JsonDeserialize(builder = ScreenshotContextFileTracker.Builder.class)
public class ScreenshotContextFileTracker {

    public final ScreenshotContext screenshotContext;
    public final ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> screenshots;

    // Measured while taking the screenshots: window.devicePixelRatio and the screenshot width in image pixels
    private volatile Double devicePixelRatio;
    private volatile Integer screenshotWidth;

    ScreenshotContextFileTracker(ScreenshotContext screenshotContext) {
        this.screenshotContext = screenshotContext;
        this.screenshots = new ConcurrentSkipListMap<>();
    }

    private ScreenshotContextFileTracker(Builder builder) {
        screenshotContext = builder.screenshotContext;
        screenshots = builder.screenshots;
        devicePixelRatio = builder.devicePixelRatio;
        screenshotWidth = builder.screenshotWidth;
    }

    public static Builder screenshotContextFileTrackerBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(ScreenshotContextFileTracker copy) {
        Builder builder = new Builder();
        builder.screenshotContext = copy.getScreenshotContext();
        builder.screenshots = copy.getScreenshots();
        builder.devicePixelRatio = copy.getDevicePixelRatio();
        builder.screenshotWidth = copy.getScreenshotWidth();
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

    public ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> getScreenshots() {
        return screenshots;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getDevicePixelRatio() {
        return devicePixelRatio;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getScreenshotWidth() {
        return screenshotWidth;
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
        Map<BrowserStep, String> stepsToPathsMap = screenshots.get(yPosition);
        if (stepsToPathsMap == null) {
            Map<BrowserStep, String> mapToPut = new HashMap<>();
            stepsToPathsMap = screenshots.putIfAbsent(yPosition, mapToPut);
            if (stepsToPathsMap == null) {
                stepsToPathsMap = mapToPut;
            }
        }
        stepsToPathsMap.put(screenshotContext.step, path);
    }

    void setScreenshotDimensions(double devicePixelRatio, int screenshotWidth) {
        this.devicePixelRatio = devicePixelRatio;
        this.screenshotWidth = screenshotWidth;
    }


    public static final class Builder {
        private ScreenshotContext screenshotContext;
        private ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> screenshots;
        private Double devicePixelRatio;
        private Integer screenshotWidth;

        private Builder() {
        }

        public Builder withScreenshotContext(ScreenshotContext val) {
            screenshotContext = val;
            return this;
        }

        public Builder withScreenshots(ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> val) {
            screenshots = val;
            return this;
        }

        public Builder withDevicePixelRatio(Double val) {
            devicePixelRatio = val;
            return this;
        }

        public Builder withScreenshotWidth(Integer val) {
            screenshotWidth = val;
            return this;
        }

        public Builder addScreenshots(Map<Integer, Map<BrowserStep, String>> val) {
            if (screenshots == null) {
                screenshots = new ConcurrentSkipListMap<>();
            }
            for (Map.Entry<Integer, Map<BrowserStep, String>> integerMapEntry : val.entrySet()) {
                if (screenshots.containsKey(integerMapEntry.getKey())) {
                    screenshots.get(integerMapEntry.getKey()).putAll(integerMapEntry.getValue());
                } else {
                    screenshots.put(integerMapEntry.getKey(), integerMapEntry.getValue());
                }
            }
            return this;
        }

        public ScreenshotContextFileTracker build() {
            return new ScreenshotContextFileTracker(this);
        }
    }

    @Override
    public String toString() {
        return "ScreenshotContextFileTracker{" +
                "screenshotContext=" + screenshotContext +
                ", screenshots=" + screenshots +
                ", devicePixelRatio=" + devicePixelRatio +
                ", screenshotWidth=" + screenshotWidth +
                '}';
    }
}
