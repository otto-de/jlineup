package de.otto.jlineup.browser;

import com.google.common.net.InternetDomainName;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.HttpCheckConfig;
import de.otto.jlineup.config.JobConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static de.otto.jlineup.config.HttpCheckConfig.DEFAULT_ALLOWED_CODES;
import static java.lang.invoke.MethodHandles.lookup;
import static java.net.InetAddress.getByName;

class JLineupHttpClient {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    void callUrl(ScreenshotContext screenshotContext) throws Exception {
        CookieStore cookieStore = prepareCookieStore(screenshotContext);
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(StandardCookieSpec.RELAXED).build())
                .setDefaultCookieStore(cookieStore)
                .build()) {

            String uri = buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping);
            final HttpGet request = new HttpGet(uri);
            LOG.debug("Calling uri {} for setup or cleanup", uri);
            HttpResponse response = client.execute(request);
            int statusCode = response.getCode();
            LOG.debug("Status code was {}", statusCode);
            if (!DEFAULT_ALLOWED_CODES.contains(statusCode)) {
                throw new JLineupException("Calling setup or cleanup path " + request.getRequestUri() + " returned status code " + statusCode);
            } else {
                LOG.info("Setup or cleanup at {} done. Return code was: {}", request.getRequestUri(), statusCode);
            }
        }
    }

    void checkPageAccessibility(ScreenshotContext screenshotContext, JobConfig jobConfig) throws Exception {

        HttpCheckConfig httpCheck = screenshotContext.urlConfig.httpCheck.isEnabled() ? screenshotContext.urlConfig.httpCheck : jobConfig.httpCheck;
        CookieStore cookieStore = prepareCookieStore(screenshotContext);

        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(StandardCookieSpec.RELAXED).build())
                .setDefaultCookieStore(cookieStore)
                .build()) {

            final HttpGet request = new HttpGet(buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping));

            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getCode();

            List<Integer> allowedCodes = httpCheck.getAllowedCodes();
            if (allowedCodes == null) {
                allowedCodes = DEFAULT_ALLOWED_CODES;
            }

            if (!allowedCodes.contains(statusCode)) {
                throw new JLineupException("Accessibility check of " + request.getRequestUri() + " returned status code " + statusCode);
            }

            List<String> errorSignals = httpCheck.getErrorSignals();
            if (errorSignals != null && !errorSignals.isEmpty()) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String body = EntityUtils.toString(entity);
                    if (body != null) {
                        for (String errorSignal : errorSignals) {
                            if (body.contains(errorSignal)) {
                                throw new JLineupException("Accessibility check of " + request.getRequestUri() + " returned error signal '" + errorSignal + "' in body ");
                            }
                        }
                    }
                }
            }
            LOG.info("Accessibility of {} checked and considered good! Return code was: {}", request.getRequestUri(), statusCode);
        }
    }

    private CookieStore prepareCookieStore(ScreenshotContext screenshotContext) throws MalformedURLException {
        List<Cookie> cookies = screenshotContext.cookies;
        CookieStore cookieStore = new BasicCookieStore();

        //If a cookie is added without a domain, Apache HTTP Client 4.5.5 throws a NullPointerException, so we extract the domain from the URL here
        String domain;
        if (isUrlLocalHost(screenshotContext.url)) {
            domain = "localhost";
        } else {
            domain = ".".concat(InternetDomainName.from(new URL(screenshotContext.url).getHost()).topPrivateDomain().toString());
        }
        if (cookies != null) addCookiesToStore(cookies, cookieStore, domain);
        return cookieStore;
    }

    private boolean isUrlLocalHost(String url) {
        try {
            InetAddress address = getByName(new URL(url).getHost());
            return address.isAnyLocalAddress() || address.isLoopbackAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void addCookiesToStore(List<Cookie> cookies, CookieStore cookieStore, String domain) {
        for (Cookie cookie : cookies) {
            BasicClientCookie apacheCookie = new BasicClientCookie(cookie.name, cookie.value);
            apacheCookie.setAttribute("domain", "true");
            if (cookie.domain != null) {
                apacheCookie.setDomain(cookie.domain);
            } else {
                apacheCookie.setDomain(domain);
            }
            if (cookie.expiry != null) {
                apacheCookie.setExpiryDate(cookie.expiry);
            }
            if (cookie.path != null) {
                apacheCookie.setPath(cookie.path);
            }
            apacheCookie.setSecure(cookie.secure);
            cookieStore.addCookie(apacheCookie);
        }
    }
}
