package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.jlineup.config.DeviceConfig;

import java.util.Objects;

import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;

public record ScreenshotComparisonResult(String contextHash, @JsonProperty("url") String fullUrlWithPath,
                                         DeviceConfig deviceConfig, int verticalScrollPosition, double difference,
                                         double maxDetectedColorDifference, String screenshotBeforeFileName,
                                         String screenshotAfterFileName, String differenceImageFileName,
                                         int acceptedDifferentPixels) {

    @UsedInTemplate
    @JsonIgnore
    public String getScreenshotBeforeFileNameForHTML() {
        return convertToHtmlPath(screenshotBeforeFileName);
    }

    @UsedInTemplate
    @JsonIgnore
    public String getScreenshotAfterFileNameForHTML() {
        return convertToHtmlPath(screenshotAfterFileName);
    }

    @UsedInTemplate
    @JsonIgnore
    public String getDifferenceImageFileNameForHtml() {
        return convertToHtmlPath(differenceImageFileName);
    }

    private String convertToHtmlPath(String fileName) {
        if (fileName == null) return null;
        return fileName.replace(FILE_SEPARATOR, "/");
    }

    public static ScreenshotComparisonResult noBeforeImageComparisonResult(String contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotAfterFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, 0d, null, screenshotAfterFileName, null, 0);
    }

    public static ScreenshotComparisonResult noAfterImageComparisonResult(String contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotBeforeFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, 0d, screenshotBeforeFileName, null, null, 0);
    }

}
