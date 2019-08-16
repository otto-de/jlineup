package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.otto.jlineup.config.DeviceConfig;

import java.util.Objects;

import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;

public class ScreenshotComparisonResult {

    public final int contextHash;
    public final String url;
    public final DeviceConfig deviceConfig;
    public final int verticalScrollPosition;
    public final double difference;
    public final String screenshotBeforeFileName;
    public final String screenshotAfterFileName;
    public final String differenceImageFileName;
    public final int acceptedDifferentPixels;

    public ScreenshotComparisonResult(int contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, double difference, String screenshotBeforeFileName, String screenshotAfterFileName, String differenceImageFileName, int acceptedDifferentPixels) {
        this.contextHash = contextHash;
        this.url = url;
        this.deviceConfig = deviceConfig;
        this.verticalScrollPosition = verticalScrollPosition;
        this.difference = difference;
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
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, null, screenshotAfterFileName, null, 0);
    }

    public static ScreenshotComparisonResult noAfterImageComparisonResult(int contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotBeforeFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, screenshotBeforeFileName, null, null, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenshotComparisonResult that = (ScreenshotComparisonResult) o;
        return contextHash == that.contextHash &&
                verticalScrollPosition == that.verticalScrollPosition &&
                Double.compare(that.difference, difference) == 0 &&
                acceptedDifferentPixels == that.acceptedDifferentPixels &&
                Objects.equals(url, that.url) &&
                Objects.equals(deviceConfig, that.deviceConfig) &&
                Objects.equals(screenshotBeforeFileName, that.screenshotBeforeFileName) &&
                Objects.equals(screenshotAfterFileName, that.screenshotAfterFileName) &&
                Objects.equals(differenceImageFileName, that.differenceImageFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextHash, url, deviceConfig, verticalScrollPosition, difference, screenshotBeforeFileName, screenshotAfterFileName, differenceImageFileName, acceptedDifferentPixels);
    }

    @Override
    public String toString() {
        return "ScreenshotComparisonResult{" +
                "contextHash=" + contextHash +
                ", url='" + url + '\'' +
                ", deviceConfig=" + deviceConfig +
                ", verticalScrollPosition=" + verticalScrollPosition +
                ", difference=" + difference +
                ", screenshotBeforeFileName='" + screenshotBeforeFileName + '\'' +
                ", screenshotAfterFileName='" + screenshotAfterFileName + '\'' +
                ", differenceImageFileName='" + differenceImageFileName + '\'' +
                ", acceptedDifferentPixels=" + acceptedDifferentPixels +
                '}';
    }
}
