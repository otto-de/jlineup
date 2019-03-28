package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;

public class ScreenshotComparisonResult {

    public final String url;
    public final int width;
    public final int verticalScrollPosition;
    public final double difference;
    public final String screenshotBeforeFileName;
    public final String screenshotAfterFileName;
    public final String differenceImageFileName;
    public final int maxSingleColorDifference;

    public ScreenshotComparisonResult(String url, int width, int verticalScrollPosition, double difference, String screenshotBeforeFileName, String screenshotAfterFileName, String differenceImageFileName, int maxSingleColorDifference) {
        this.url = url;
        this.width = width;
        this.verticalScrollPosition = verticalScrollPosition;
        this.difference = difference;
        this.screenshotBeforeFileName = screenshotBeforeFileName;
        this.screenshotAfterFileName = screenshotAfterFileName;
        this.differenceImageFileName = differenceImageFileName;
        this.maxSingleColorDifference = maxSingleColorDifference;
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

    public static ScreenshotComparisonResult noBeforeImageComparisonResult(String url, int width, int verticalScrollPosition, String screenshotAfterFileName) {
        return new ScreenshotComparisonResult(url, width, verticalScrollPosition, 1d, null, screenshotAfterFileName, null, 0);
    }

    public static ScreenshotComparisonResult noAfterImageComparisonResult(String url, int width, int verticalScrollPosition, String screenshotBeforeFileName) {
        return new ScreenshotComparisonResult(url, width, verticalScrollPosition, 1d, screenshotBeforeFileName, null, null, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenshotComparisonResult that = (ScreenshotComparisonResult) o;
        return width == that.width &&
                verticalScrollPosition == that.verticalScrollPosition &&
                Double.compare(that.difference, difference) == 0 &&
                maxSingleColorDifference == that.maxSingleColorDifference &&
                Objects.equals(url, that.url) &&
                Objects.equals(screenshotBeforeFileName, that.screenshotBeforeFileName) &&
                Objects.equals(screenshotAfterFileName, that.screenshotAfterFileName) &&
                Objects.equals(differenceImageFileName, that.differenceImageFileName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(url, width, verticalScrollPosition, difference, screenshotBeforeFileName, screenshotAfterFileName, differenceImageFileName, maxSingleColorDifference);
    }

    @Override
    public String toString() {
        return "ScreenshotComparisonResult{" +
                "url='" + url + '\'' +
                ", width=" + width +
                ", verticalScrollPosition=" + verticalScrollPosition +
                ", difference=" + difference +
                ", screenshotBeforeFileName='" + screenshotBeforeFileName + '\'' +
                ", screenshotAfterFileName='" + screenshotAfterFileName + '\'' +
                ", differenceImageFileName='" + differenceImageFileName + '\'' +
                ", maxSingleColorDifference=" + maxSingleColorDifference +
                '}';
    }
}
