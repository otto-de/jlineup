package de.otto.jlineup.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The report of one {@link ScreenshotContext}, i.e. one combination of URL, sub-path, device, browser and cookies.
 * It contains the comparison results of all scroll positions of the page.
 *
 * @param contextHash       hash that identifies the screenshot context, also used in the file names of its screenshots
 * @param screenshotContext the context the screenshots were taken for
 * @param summary           the aggregated differences of all results of this context
 * @param results           the comparison results, one per scroll position
 * @param flakyAccepted     true if this context has differences, but was accepted because enough sibling contexts of
 *                          the same page passed (see 'flaky-tolerance')
 * @param devicePixelRatio  the window.devicePixelRatio measured while taking the screenshots (null if unknown)
 * @param screenshotWidth   the width of the screenshots in image pixels (null if unknown)
 * @param imageUrlChanges   image URLs of the page that differ between before and after (only recorded with
 *                          'ignore-image-content', null if unknown or unchanged)
 */
public record ContextReport(String contextHash, ScreenshotContext screenshotContext, Summary summary,
                            List<ScreenshotComparisonResult> results, boolean flakyAccepted,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Double devicePixelRatio,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Integer screenshotWidth,
                            @JsonInclude(JsonInclude.Include.NON_NULL) ImageUrlChanges imageUrlChanges) {

    /**
     * Convenience constructor for backwards compatibility — no image URL changes.
     */
    public ContextReport(String contextHash, ScreenshotContext screenshotContext, Summary summary,
                         List<ScreenshotComparisonResult> results, boolean flakyAccepted,
                         Double devicePixelRatio, Integer screenshotWidth) {
        this(contextHash, screenshotContext, summary, results, flakyAccepted, devicePixelRatio, screenshotWidth, null);
    }

    /**
     * Convenience constructor for backwards compatibility — no measured screenshot dimensions.
     */
    public ContextReport(String contextHash, ScreenshotContext screenshotContext, Summary summary,
                         List<ScreenshotComparisonResult> results, boolean flakyAccepted) {
        this(contextHash, screenshotContext, summary, results, flakyAccepted, null, null);
    }

    /**
     * Convenience constructor for backwards compatibility — defaults flakyAccepted to false.
     */
    public ContextReport(String contextHash, ScreenshotContext screenshotContext, Summary summary,
                         List<ScreenshotComparisonResult> results) {
        this(contextHash, screenshotContext, summary, results, false);
    }

    @UsedInTemplate
    public String getUrl() {
        return BrowserUtils.buildUrl(screenshotContext.url, screenshotContext.urlSubPath, Collections.emptyMap());
    }

    /** Returns the configured title for this sub-path, or null if none was set. */
    @UsedInTemplate
    public String getSubPathTitle() {
        return screenshotContext.urlSubPathTitle;
    }

    @UsedInTemplate
    public String getShortenedUrl() {
        String shortenedUrl = getUrl();
        if (shortenedUrl.length() > 25) {
            shortenedUrl = "..." + shortenedUrl.substring(shortenedUrl.lastIndexOf("/"), shortenedUrl.length());
        }
        return shortenedUrl;
    }

    @Override
    @UsedInTemplate
    public String contextHash() {
        return contextHash;
    }

    @UsedInTemplate
    public int getWidth() {
        return screenshotContext.deviceConfig.width;
    }

    /**
     * The width of the screenshots in CSS pixels, i.e. the width of the page as it was rendered in the browser.
     * Calculated from the measured screenshot width and device pixel ratio, falls back to the configured width.
     * Null if unknown (specific mobile devices without measured values).
     */
    @UsedInTemplate
    @JsonIgnore
    public Integer getScreenshotDisplayWidth() {
        if (screenshotWidth != null && screenshotWidth > 0 && devicePixelRatio != null && devicePixelRatio > 0) {
            return (int) Math.round(screenshotWidth / devicePixelRatio);
        }
        DeviceConfig dc = screenshotContext.deviceConfig;
        if (dc.isSpecificMobile() || dc.width <= 0) {
            return null;
        }
        return dc.width;
    }

    /**
     * CSS style that caps the displayed screenshot width at the width the page was rendered with, so that
     * screenshots (e.g. mobile ones, or ones with a high device pixel ratio) are not upscaled to the full
     * column width on large screens.
     */
    @UsedInTemplate
    @JsonIgnore
    public String getScreenshotMaxWidthStyle() {
        Integer displayWidth = getScreenshotDisplayWidth();
        return displayWidth != null ? "--screenshot-max-width: " + displayWidth + "px" : null;
    }

    @UsedInTemplate
    public List<Cookie> getShownCookies() {
        if (screenshotContext.cookies == null) {
            return null;
        }
        return screenshotContext.cookies.stream().filter(cookie -> cookie.showInReport != null && cookie.showInReport).collect(Collectors.toList());
    }

    @UsedInTemplate
    public String getShownCookiesString() {
        if (getShownCookies() == null) {
            return null;
        }
        String cookiesString = getShownCookies().stream().filter(Objects::nonNull).map(cookie -> cookie.name + ": " + cookie.value).collect(Collectors.joining(", "));
        try {
            cookiesString = URLDecoder.decode(cookiesString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            //URLDecode failed, go on without it
        }
        return cookiesString;
    }

    @UsedInTemplate
    public String getDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        DeviceConfig dc = screenshotContext.deviceConfig;
        if (dc.isMobile()) {
            sb.append(dc.deviceName);
        }
        if (dc.isGenericMobile()) {
            sb.append("\n");
        }
        if (dc.isGenericMobile() || dc.isDesktop()) {
            sb.append(dc.width);
            sb.append("x");
            sb.append(dc.height);
            if (dc.pixelRatio != 1.0f) {
                sb.append("\nPixel ratio: ");
                sb.append(dc.pixelRatio);
            }
            if (dc.userAgent != null) {
                sb.append("\n");
                sb.append(dc.userAgent);
            }
            if (dc.isDesktop() && dc.touch) {
                sb.append("\n");
                sb.append("Touch enabled");
            }
        }
        return sb.toString();
    }

    @UsedInTemplate
    public String getBrowserInfo() {
        Browser.Type bt = screenshotContext.browserType;
        return bt != null ? bt.name().toLowerCase().replace('_', '-') : null;
    }

    @Override
    @UsedInTemplate
    public List<ScreenshotComparisonResult> results() {
        return results;
    }

    @UsedInTemplate
    public boolean isSuccess() {
        if (flakyAccepted) {
            return true;
        }
        for (ScreenshotComparisonResult result : results) {
            if (result.difference() > 0)
                return false;
        }

        return true;
    }

    /**
     * True if at least one screenshot of this context has a counterpart without images, so the report can switch.
     */
    @UsedInTemplate
    @JsonIgnore
    public boolean hasImagelessScreenshots() {
        return results != null && results.stream().anyMatch(ScreenshotComparisonResult::hasImagelessScreenshots);
    }

    @UsedInTemplate
    public boolean isFlakyAccepted() {
        return flakyAccepted;
    }
}
