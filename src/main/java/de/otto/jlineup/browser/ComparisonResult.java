package de.otto.jlineup.browser;

import java.nio.file.Paths;

public class ComparisonResult {

    public final String url;
    public final int width;
    public final int verticalScrollPosition;
    public final double difference;
    public final String screenshotBeforeFileName;
    public final String screenshotAfterFileName;
    public final String differenceImageFileName;

    public ComparisonResult(String url, int width, int verticalScrollPosition, double difference, String screenshotBeforeFileName, String screenshotAfterFileName, String differenceImageFileName) {
        this.url = url;
        this.width = width;
        this.verticalScrollPosition = verticalScrollPosition;
        this.difference = difference;
        this.screenshotBeforeFileName = screenshotBeforeFileName != null ? Paths.get(screenshotBeforeFileName).getFileName().toString() : null;
        this.screenshotAfterFileName = screenshotAfterFileName != null ? Paths.get(screenshotAfterFileName).getFileName().toString() : null;
        this.differenceImageFileName = differenceImageFileName != null ? Paths.get(differenceImageFileName).getFileName().toString() : null;
    }

    public static ComparisonResult noBeforeImageComparisonResult(String url, int width, int verticalScrollPosition, String screenshotAfterFileName) {
        return new ComparisonResult(url, width, verticalScrollPosition, 1d, null, screenshotAfterFileName, null);
    }

    public static ComparisonResult noAfterImageComparisonResult(String url, int width, int verticalScrollPosition, String screenshotBeforeFileName) {
        return new ComparisonResult(url, width, verticalScrollPosition, 1d, screenshotBeforeFileName, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComparisonResult that = (ComparisonResult) o;

        if (width != that.width) return false;
        if (verticalScrollPosition != that.verticalScrollPosition) return false;
        if (Double.compare(that.difference, difference) != 0) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (screenshotBeforeFileName != null ? !screenshotBeforeFileName.equals(that.screenshotBeforeFileName) : that.screenshotBeforeFileName != null)
            return false;
        if (screenshotAfterFileName != null ? !screenshotAfterFileName.equals(that.screenshotAfterFileName) : that.screenshotAfterFileName != null)
            return false;
        return differenceImageFileName != null ? differenceImageFileName.equals(that.differenceImageFileName) : that.differenceImageFileName == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = url != null ? url.hashCode() : 0;
        result = 31 * result + width;
        result = 31 * result + verticalScrollPosition;
        temp = Double.doubleToLongBits(difference);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (screenshotBeforeFileName != null ? screenshotBeforeFileName.hashCode() : 0);
        result = 31 * result + (screenshotAfterFileName != null ? screenshotAfterFileName.hashCode() : 0);
        result = 31 * result + (differenceImageFileName != null ? differenceImageFileName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ComparisonResult{" +
                "url='" + url + '\'' +
                ", width=" + width +
                ", verticalScrollPosition=" + verticalScrollPosition +
                ", difference=" + difference +
                ", screenshotBeforeFileName='" + screenshotBeforeFileName + '\'' +
                ", screenshotAfterFileName='" + screenshotAfterFileName + '\'' +
                ", differenceImageFileName='" + differenceImageFileName + '\'' +
                '}';
    }
}
