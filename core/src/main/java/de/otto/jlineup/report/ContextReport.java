package de.otto.jlineup.report;

import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ContextReport {

    public final int contextHash;
    public final Summary summary;
    public final List<ScreenshotComparisonResult> results;
    public final ScreenshotContext screenshotContext;

    public ContextReport(int contextHash, ScreenshotContext screenshotContext, Summary summary, List<ScreenshotComparisonResult> results) {
        this.contextHash = contextHash;
        this.screenshotContext = screenshotContext;
        this.summary = summary;
        this.results = results;
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

    @UsedInTemplate
    public int getContextHash() {
        return contextHash;
    }

    @UsedInTemplate
    public int getWidth() {
        return screenshotContext.deviceConfig.width;
    }

    @UsedInTemplate
    public List<Cookie> getShownCookies() {
        return screenshotContext.cookies.stream().filter(cookie -> cookie.showInReport != null && cookie.showInReport).collect(Collectors.toList());
    }

    @UsedInTemplate
    public String getShownCookiesString() {
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
    public List<ScreenshotComparisonResult> getResults() {
        return results;
    }

    @UsedInTemplate
    public boolean isSuccess() {
        for (ScreenshotComparisonResult result : results) {
            if (result.difference > 0)
                return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextReport that = (ContextReport) o;
        return contextHash == that.contextHash && Objects.equals(summary, that.summary) && Objects.equals(results, that.results) && Objects.equals(screenshotContext, that.screenshotContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextHash, summary, results, screenshotContext);
    }

    @Override
    public String toString() {
        return "ContextReport{" +
                "contextHash=" + contextHash +
                ", summary=" + summary +
                ", results=" + results +
                ", screenshotContext=" + screenshotContext +
                '}';
    }
}
