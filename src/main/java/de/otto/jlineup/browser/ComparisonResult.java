package de.otto.jlineup.browser;

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
        this.screenshotBeforeFileName = screenshotBeforeFileName;
        this.screenshotAfterFileName = screenshotAfterFileName;
        this.differenceImageFileName = differenceImageFileName;
    }
}
