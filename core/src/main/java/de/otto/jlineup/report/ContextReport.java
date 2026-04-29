package de.otto.jlineup.report;

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

public record ContextReport(String contextHash, ScreenshotContext screenshotContext, Summary summary,
                            List<ScreenshotComparisonResult> results, boolean flakyAccepted) {

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

    @UsedInTemplate
    public boolean isFlakyAccepted() {
        return flakyAccepted;
    }
}
