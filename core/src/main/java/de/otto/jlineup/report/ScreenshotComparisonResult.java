package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.jlineup.config.DeviceConfig;

import java.util.Objects;

import static de.otto.jlineup.file.FileService.FILE_SEPARATOR;

/**
 * The result of comparing the before and after screenshot of one scroll position of one screenshot context.
 * <p>
 * Results are part of the {@link ContextReport}s and are written to the JSON report. All file names are relative to
 * the report directory. A missing screenshot (i.e. a new or removed page) is represented with a {@code null} file name
 * and a {@code difference} of {@code 1.0}, see {@link #noBeforeImageComparisonResult} and
 * {@link #noAfterImageComparisonResult}.
 * <p>
 * The screenshots without images and the image content hint are only set with 'ignore-image-content'.
 *
 * @param contextHash                         hash of the {@link de.otto.jlineup.browser.ScreenshotContext} this
 *                                            screenshot belongs to
 * @param fullUrlWithPath                     the URL of the page including its sub-path, serialized as {@code url}
 * @param deviceConfig                        the device (window size, pixel ratio, user agent...) the screenshot was
 *                                            taken with
 * @param verticalScrollPosition              the vertical scroll position of the page in pixels at which the
 *                                            screenshot was taken
 * @param difference                          the share of different pixels between 0.0 (identical) and 1.0 (every
 *                                            pixel differs). Pixels that were accepted (see
 *                                            {@code acceptedDifferentPixels}) are not counted. Compared against
 *                                            {@code max-diff} to decide whether the screenshot is an error.
 * @param maxDetectedColorDifference          the highest CIEDE2000 color distance found between two different pixels
 *                                            (0 for identical images or with 'strict-color-comparison')
 * @param screenshotBeforeFileName            file name of the before screenshot, null if there is none
 * @param screenshotAfterFileName             file name of the after screenshot, null if there is none
 * @param differenceImageFileName             file name of the image that highlights the differing pixels, null if the
 *                                            screenshots are identical or one of them is missing
 * @param acceptedDifferentPixels             the number of pixels that differ, but were not counted as difference
 *                                            because they look the same (see 'max-color-distance'), are anti-aliased,
 *                                            are compression artifacts or lie inside unchanged images
 * @param imagelessScreenshotBeforeFileName   the before screenshot with all images replaced by a checkerboard
 * @param imagelessScreenshotAfterFileName    the after screenshot with all images replaced by a checkerboard
 * @param imagelessDifferenceImageFileName    the difference image of the two screenshots without images
 * @param imageContentHint                    explains whether differences inside images were accepted, and if not,
 *                                            why
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

    /**
     * Creates a result without screenshots without images, i.e. if 'ignore-image-content' is not used.
     */
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

    /**
     * Creates the result for a screenshot that only exists in the after step. It counts as completely different
     * ({@code difference} 1.0).
     *
     * @param screenshotAfterFileName file name of the after screenshot
     * @return a result without before screenshot and difference image
     */
    public static ScreenshotComparisonResult noBeforeImageComparisonResult(String contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotAfterFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, 0d, null, screenshotAfterFileName, null, 0);
    }

    /**
     * Creates the result for a screenshot that only exists in the before step. It counts as completely different
     * ({@code difference} 1.0).
     *
     * @param screenshotBeforeFileName file name of the before screenshot
     * @return a result without after screenshot and difference image
     */
    public static ScreenshotComparisonResult noAfterImageComparisonResult(String contextHash, String url, DeviceConfig deviceConfig, int verticalScrollPosition, String screenshotBeforeFileName) {
        return new ScreenshotComparisonResult(contextHash, url, deviceConfig, verticalScrollPosition, 1d, 0d, screenshotBeforeFileName, null, null, 0);
    }

}
