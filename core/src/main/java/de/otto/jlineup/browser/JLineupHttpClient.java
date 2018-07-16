package de.otto.jlineup.browser;

import com.google.common.net.InternetDomainName;
import de.otto.jlineup.config.Cookie;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static java.lang.invoke.MethodHandles.lookup;

class JLineupHttpClient {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    void checkPageAccessibility(ScreenshotContext screenshotContext) throws Exception {

        List<Cookie> cookies = screenshotContext.urlConfig.cookies;
        CookieStore cookieStore = new BasicCookieStore();

        //If a cookie is added without a domain, Apache HTTP Client 4.5.5 throws a NullPointerException, so we extract the domain from the URL here
        String domain = ".".concat(InternetDomainName.from(new URL(screenshotContext.url).getHost()).topPrivateDomain().toString());
        addCookiesToStore(cookies, cookieStore, domain);

        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .build()) {

            final HttpGet request = new HttpGet(buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping));

            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new JLineupException("Accessibility check of " + request.getURI() + " returned status code " + statusCode);
            } else {
                LOG.debug("Accessibility of {} checked and OK!", request.getURI());
            }
        }
    }

    private void addCookiesToStore(List<Cookie> cookies, CookieStore cookieStore, String domain) {
        for (Cookie cookie : cookies) {
            BasicClientCookie apacheCookie = new BasicClientCookie(cookie.name, cookie.value);
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
