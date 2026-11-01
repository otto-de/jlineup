package de.otto.jlineup.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.annotation.JsonDeserialize;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.ScreenshotContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@JsonDeserialize(builder = ScreenshotContextFileTracker.Builder.class)
public class ScreenshotContextFileTracker {

    public final ScreenshotContext screenshotContext;
    public final ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> screenshots;

    // Only recorded with 'ignore-image-content': screenshots taken with all images hidden, and the image URLs of the page
    public final ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> imagelessScreenshots;
    public final ConcurrentHashMap<BrowserStep, Set<String>> imageUrls;
    // Areas of image elements per screenshot as [x1, y1, x2, y2] in screenshot pixels (x2 and y2 exclusive)
    public final ConcurrentSkipListMap<Integer, Map<BrowserStep, List<int[]>>> imageAreas;

    // Measured while taking the screenshots: window.devicePixelRatio and the screenshot width in image pixels
    private volatile Double devicePixelRatio;
    private volatile Integer screenshotWidth;

    ScreenshotContextFileTracker(ScreenshotContext screenshotContext) {
        this.screenshotContext = screenshotContext;
        this.screenshots = new ConcurrentSkipListMap<>();
        this.imagelessScreenshots = new ConcurrentSkipListMap<>();
        this.imageUrls = new ConcurrentHashMap<>();
        this.imageAreas = new ConcurrentSkipListMap<>();
    }

    private ScreenshotContextFileTracker(Builder builder) {
        screenshotContext = builder.screenshotContext;
        screenshots = builder.screenshots;
        imagelessScreenshots = builder.imagelessScreenshots != null ? builder.imagelessScreenshots : new ConcurrentSkipListMap<>();
        imageUrls = builder.imageUrls != null ? builder.imageUrls : new ConcurrentHashMap<>();
        imageAreas = builder.imageAreas != null ? builder.imageAreas : new ConcurrentSkipListMap<>();
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
        builder.imagelessScreenshots = copy.getImagelessScreenshots();
        builder.imageUrls = copy.getImageUrls();
        builder.imageAreas = copy.getImageAreas();
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> getImagelessScreenshots() {
        return imagelessScreenshots;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ConcurrentHashMap<BrowserStep, Set<String>> getImageUrls() {
        return imageUrls;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public ConcurrentSkipListMap<Integer, Map<BrowserStep, List<int[]>>> getImageAreas() {
        return imageAreas;
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
        addToPositionMap(screenshots, screenshotContext.step, path, yPosition);
    }

    void addImagelessScreenshot(ScreenshotContext screenshotContext, String path, int yPosition) {
        addToPositionMap(imagelessScreenshots, screenshotContext.step, path, yPosition);
    }

    void setImageAreas(BrowserStep step, int yPosition, List<int[]> areas) {
        imageAreas.computeIfAbsent(yPosition, k -> new ConcurrentHashMap<>()).put(step, areas);
    }

    void setImageUrls(BrowserStep step, Set<String> urls) {
        imageUrls.put(step, new TreeSet<>(urls));
    }

    private static void addToPositionMap(ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> positionMap, BrowserStep step, String path, int yPosition) {
        Map<BrowserStep, String> stepsToPathsMap = positionMap.get(yPosition);
        if (stepsToPathsMap == null) {
            Map<BrowserStep, String> mapToPut = new HashMap<>();
            stepsToPathsMap = positionMap.putIfAbsent(yPosition, mapToPut);
            if (stepsToPathsMap == null) {
                stepsToPathsMap = mapToPut;
            }
        }
        stepsToPathsMap.put(step, path);
    }

    void setScreenshotDimensions(double devicePixelRatio, int screenshotWidth) {
        this.devicePixelRatio = devicePixelRatio;
        this.screenshotWidth = screenshotWidth;
    }


    public static final class Builder {
        private ScreenshotContext screenshotContext;
        private ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> screenshots;
        private ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> imagelessScreenshots;
        private ConcurrentHashMap<BrowserStep, Set<String>> imageUrls;
        private ConcurrentSkipListMap<Integer, Map<BrowserStep, List<int[]>>> imageAreas;
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

        public Builder withImagelessScreenshots(ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> val) {
            imagelessScreenshots = val;
            return this;
        }

        public Builder withImageUrls(ConcurrentHashMap<BrowserStep, Set<String>> val) {
            imageUrls = val;
            return this;
        }

        public Builder withImageAreas(ConcurrentSkipListMap<Integer, Map<BrowserStep, List<int[]>>> val) {
            imageAreas = val;
            return this;
        }

        public Builder addImageAreas(Map<Integer, Map<BrowserStep, List<int[]>>> val) {
            if (imageAreas == null) {
                imageAreas = new ConcurrentSkipListMap<>();
            }
            if (val != null) {
                val.forEach((position, areasPerStep) -> imageAreas.computeIfAbsent(position, k -> new ConcurrentHashMap<>()).putAll(areasPerStep));
            }
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
            mergePositionMap(screenshots, val);
            return this;
        }

        public Builder addImagelessScreenshots(Map<Integer, Map<BrowserStep, String>> val) {
            if (imagelessScreenshots == null) {
                imagelessScreenshots = new ConcurrentSkipListMap<>();
            }
            if (val != null) {
                mergePositionMap(imagelessScreenshots, val);
            }
            return this;
        }

        public Builder addImageUrls(Map<BrowserStep, Set<String>> val) {
            if (imageUrls == null) {
                imageUrls = new ConcurrentHashMap<>();
            }
            if (val != null) {
                imageUrls.putAll(val);
            }
            return this;
        }

        private static void mergePositionMap(ConcurrentSkipListMap<Integer, Map<BrowserStep, String>> target, Map<Integer, Map<BrowserStep, String>> val) {
            for (Map.Entry<Integer, Map<BrowserStep, String>> integerMapEntry : val.entrySet()) {
                if (target.containsKey(integerMapEntry.getKey())) {
                    target.get(integerMapEntry.getKey()).putAll(integerMapEntry.getValue());
                } else {
                    target.put(integerMapEntry.getKey(), integerMapEntry.getValue());
                }
            }
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
                ", imagelessScreenshots=" + imagelessScreenshots +
                ", imageUrls=" + imageUrls +
                ", imageAreas=" + imageAreas +
                ", devicePixelRatio=" + devicePixelRatio +
                ", screenshotWidth=" + screenshotWidth +
                '}';
    }
}
