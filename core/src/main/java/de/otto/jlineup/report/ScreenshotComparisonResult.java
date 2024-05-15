package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.jlineup.config.DeviceConfig;

import java.util.Objects;

import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;

public class ScreenshotComparisonResult {

    public final int contextHash;
    @JsonProperty("url")
    public final String fullUrlWithPath;
    public final DeviceConfig deviceConfig;
    public final int verticalScrollPosition;
    public final double difference;
    public final double maxDetectedColorDifference;
    public final String screenshotBeforeFileName;
    public final String screenshotAfterFileName;
    public final String differenceImageFileName;
    public final int acceptedDifferentPixels;

    public ScreenshotComparisonResult(int contextHash, String fullUrlWithPath, DeviceConfig deviceConfig, int verticalScrollPosition, double difference, double maxDetectedColorDifference, String screenshotBeforeFileName, String screenshotAfterFileName, String differenceImageFileName, int acceptedDifferentPixels) {
        this.contextHash = contextHash;
        this.fullUrlWithPath = fullUrlWithPath;
        this.deviceConfig = deviceConfig;
        this.verticalScrollPosition = verticalScrollPosition;
        this.difference = difference;
        this.maxDetectedColorDifference = maxDetectedColorDifference;
        this.screenshotBeforeFileName = screenshotBeforeFileName;
        this.screenshotAfterFileName = screenshotAfterFileName;
        this.differenceImageFileName = differenceImageFileName;
        this.acceptedDifferentPixels = acceptedDifferentPixels;
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

    private String convertToHtmlPath(String fileName) {
        if (fileName == null) return null;
        return fileName.replace(FILE_SEPARATOR, "/");
    }

    public static ScreenshotComparisonResult noBeforeImageComparisonResult(int contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotAfterFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, 0d, null, screenshotAfterFileName, null, 0);
    }

    public static ScreenshotComparisonResult noAfterImageComparisonResult(int contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotBeforeFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, 0d, screenshotBeforeFileName, null, null, 0);
    }

    @Override
    public String toString() {
        return "ScreenshotComparisonResult{" +
                "contextHash=" + contextHash +
                ", fullUrlWithPath='" + fullUrlWithPath + '\'' +
                ", deviceConfig=" + deviceConfig +
                ", verticalScrollPosition=" + verticalScrollPosition +
                ", difference=" + difference +
                ", maxDetectedColorDifference=" + maxDetectedColorDifference +
                ", screenshotBeforeFileName='" + screenshotBeforeFileName + '\'' +
                ", screenshotAfterFileName='" + screenshotAfterFileName + '\'' +
                ", differenceImageFileName='" + differenceImageFileName + '\'' +
                ", acceptedDifferentPixels=" + acceptedDifferentPixels +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenshotComparisonResult that = (ScreenshotComparisonResult) o;
        return contextHash == that.contextHash && verticalScrollPosition == that.verticalScrollPosition && Double.compare(difference, that.difference) == 0 && Double.compare(maxDetectedColorDifference, that.maxDetectedColorDifference) == 0 && acceptedDifferentPixels == that.acceptedDifferentPixels && Objects.equals(fullUrlWithPath, that.fullUrlWithPath) && Objects.equals(deviceConfig, that.deviceConfig) && Objects.equals(screenshotBeforeFileName, that.screenshotBeforeFileName) && Objects.equals(screenshotAfterFileName, that.screenshotAfterFileName) && Objects.equals(differenceImageFileName, that.differenceImageFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextHash, fullUrlWithPath, deviceConfig, verticalScrollPosition, difference, maxDetectedColorDifference, screenshotBeforeFileName, screenshotAfterFileName, differenceImageFileName, acceptedDifferentPixels);
    }

}
