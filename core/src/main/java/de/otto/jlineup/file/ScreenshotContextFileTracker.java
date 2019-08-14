package de.otto.jlineup.file;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.Step;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class ScreenshotContextFileTracker {

    public final ScreenshotContext screenshotContext;
    public final Map<Integer, Map<Step, String>> screenshots;

    ScreenshotContextFileTracker(ScreenshotContext screenshotContext) {
        this.screenshotContext = screenshotContext;
        this.screenshots = new ConcurrentSkipListMap<>();
    }

    //Used by Jackson
    private ScreenshotContextFileTracker() {
        this.screenshotContext = null;
        this.screenshots = null;
    }

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
}
