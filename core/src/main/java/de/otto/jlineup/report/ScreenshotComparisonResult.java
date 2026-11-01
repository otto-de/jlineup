package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.jlineup.config.DeviceConfig;

import java.util.Objects;

import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;

/**
 * The screenshots without images and the image content hint are only set with 'ignore-image-content'.
 */
public record ScreenshotComparisonResult(String contextHash, @JsonProperty("url") String fullUrlWithPath,
                                         DeviceConfig deviceConfig, int verticalScrollPosition, double difference,
                                         double maxDetectedColorDifference, String screenshotBeforeFileName,
                                         String screenshotAfterFileName, String differenceImageFileName,
                                         int acceptedDifferentPixels,
                                         @JsonInclude(JsonInclude.Include.NON_NULL) String imagelessScreenshotBeforeFileName,
                                         @JsonInclude(JsonInclude.Include.NON_NULL) String imagelessScreenshotAfterFileName,
                                         @JsonInclude(JsonInclude.Include.NON_NULL) String imagelessDifferenceImageFileName,
                                         @JsonInclude(JsonInclude.Include.NON_NULL) ImageContentHint imageContentHint) {

    public ScreenshotComparisonResult(String contextHash, String fullUrlWithPath,
                                      DeviceConfig deviceConfig, int verticalScrollPosition, double difference,
                                      double maxDetectedColorDifference, String screenshotBeforeFileName,
                                      String screenshotAfterFileName, String differenceImageFileName,
                                      int acceptedDifferentPixels) {
        this(contextHash, fullUrlWithPath, deviceConfig, verticalScrollPosition, difference, maxDetectedColorDifference,
                screenshotBeforeFileName, screenshotAfterFileName, differenceImageFileName, acceptedDifferentPixels,
                null, null, null, null);
    }

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

    @UsedInTemplate
    @JsonIgnore
    public String getImagelessScreenshotBeforeFileNameForHTML() {
        return convertToHtmlPath(imagelessScreenshotBeforeFileName);
    }

    @UsedInTemplate
    @JsonIgnore
    public String getImagelessScreenshotAfterFileNameForHTML() {
        return convertToHtmlPath(imagelessScreenshotAfterFileName);
    }

    @UsedInTemplate
    @JsonIgnore
    public String getImagelessDifferenceImageFileNameForHTML() {
        return convertToHtmlPath(imagelessDifferenceImageFileName);
    }

    /**
     * True if every existing screenshot of this result (before and/or after) has a counterpart without images,
     * so the report can switch to them.
     */
    @UsedInTemplate
    @JsonIgnore
    public boolean hasImagelessScreenshots() {
        boolean beforeCovered = screenshotBeforeFileName == null || imagelessScreenshotBeforeFileName != null;
        boolean afterCovered = screenshotAfterFileName == null || imagelessScreenshotAfterFileName != null;
        return beforeCovered && afterCovered && (imagelessScreenshotBeforeFileName != null || imagelessScreenshotAfterFileName != null);
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
