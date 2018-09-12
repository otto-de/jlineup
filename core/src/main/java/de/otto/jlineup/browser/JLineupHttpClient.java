package de.otto.jlineup.browser;

import com.google.common.net.InternetDomainName;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.HttpCheckConfig;
import de.otto.jlineup.config.JobConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URL;
import java.util.List;

import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static de.otto.jlineup.config.HttpCheckConfig.DEFAULT_ALLOWED_CODES;
import static java.lang.invoke.MethodHandles.lookup;
import static java.net.InetAddress.getByName;

class JLineupHttpClient {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    void checkPageAccessibility(ScreenshotContext screenshotContext, JobConfig jobConfig) throws Exception {

        List<Cookie> cookies = screenshotContext.urlConfig.cookies;
        HttpCheckConfig httpCheck = screenshotContext.urlConfig.httpCheck.isEnabled() ? screenshotContext.urlConfig.httpCheck : jobConfig.httpCheck;
        CookieStore cookieStore = new BasicCookieStore();

        //If a cookie is added without a domain, Apache HTTP Client 4.5.5 throws a NullPointerException, so we extract the domain from the URL here
        String domain;
        if (isUrlLocalHost(screenshotContext.url)) {
            domain = ".localhost";
        } else {
            domain = ".".concat(InternetDomainName.from(new URL(screenshotContext.url).getHost()).topPrivateDomain().toString());
        }
        if (cookies != null) addCookiesToStore(cookies, cookieStore, domain);

        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .setDefaultCookieStore(cookieStore)
                .build()) {

            final HttpGet request = new HttpGet(buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping));

            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            List<Integer> allowedCodes = httpCheck.getAllowedCodes();
            if (allowedCodes == null) {
                allowedCodes = DEFAULT_ALLOWED_CODES;
            }

            if (!allowedCodes.contains(statusCode)) {
                throw new JLineupException("Accessibility check of " + request.getURI() + " returned status code " + statusCode);
            } else {
                LOG.debug("Accessibility of {} checked and considered good! Return code was: {}", request.getURI(), statusCode);
            }
        }
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
            apacheCookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
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
