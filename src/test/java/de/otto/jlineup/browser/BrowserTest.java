package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.browser.Browser.*;
import static de.otto.jlineup.browser.Browser.Type.*;
import static de.otto.jlineup.config.Config.configBuilder;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class BrowserTest {

    @Mock
    private TestSupportWebDriver webDriverMock;
    @Mock
    private WebDriver.Options webDriverOptionsMock;
    @Mock
    private WebDriver.Timeouts webDriverTimeoutMock;
    @Mock
    private WebDriver.Window webDriverWindowMock;
    @Mock
    private Logs webDriverLogs;
    @Mock
    private BrowserUtils browserUtilsMock;

    @Mock
    private Parameters parameters;
    @Mock
    private FileService fileService;

    private Browser testee;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        when(webDriverMock.manage()).thenReturn(webDriverOptionsMock);
        when(webDriverOptionsMock.timeouts()).thenReturn(webDriverTimeoutMock);
        when(webDriverOptionsMock.window()).thenReturn(webDriverWindowMock);
        when(webDriverOptionsMock.logs()).thenReturn(webDriverLogs);
        when(browserUtilsMock.getWebDriverByConfig(any(Config.class))).thenReturn(webDriverMock);
        when(browserUtilsMock.getWebDriverByConfig(any(Config.class), anyInt())).thenReturn(webDriverMock);
        Config config = configBuilder().build();
        testee = new Browser(parameters, config, fileService, browserUtilsMock);
        testee.initializeWebDriver();
    }

    @After
    public void cleanup() throws Exception {
        if (testee != null) {
            testee.close();
        }
    }

    @Test
    public void shouldGetFirefoxDriver() throws InterruptedException {
        final Config config = configBuilder().withBrowser(FIREFOX).build();
        assertSetDriverType(config, FirefoxDriver.class);
    }

    @Test
    public void shouldGetChromeDriver() throws InterruptedException {
        final Config config = configBuilder().withBrowser(CHROME).build();
        assertSetDriverType(config, ChromeDriver.class);
    }

    @Test
    public void shouldGetChromeDriverForHeadlessChrome() throws Exception {
        final Config config = configBuilder().withBrowser(CHROME_HEADLESS).build();
        assertSetDriverType(config, ChromeDriver.class);
    }

    @Test
    public void shouldGetPhantomJSDriver() throws InterruptedException {
        final Config config = configBuilder().withBrowser(PHANTOMJS).build();
        assertSetDriverType(config, PhantomJSDriver.class);
    }

    private void assertSetDriverType(Config config, Class<? extends WebDriver> driverClass) {
        WebDriver driver = null;
        BrowserUtils realBrowserUtils = new BrowserUtils();
        try {
            driver = realBrowserUtils.getWebDriverByConfig(config);
            assertTrue(driverClass.isInstance(driver));
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @Test
    public void shouldSetCookies() {
        //given
        ArgumentCaptor<org.openqa.selenium.Cookie> cookieCaptor = ArgumentCaptor.forClass(org.openqa.selenium.Cookie.class);

        Cookie cookieOne = new Cookie("someName", "someValue", "someDomain", "somePath", new Date(10000L), true);
        Cookie cookieTwo = new Cookie("someOtherName", "someOtherValue", "someOtherDomain", "someOtherPath", new Date(10000000000L), false);
        //when
        testee.setCookies(ImmutableList.of(cookieOne, cookieTwo));
        //then
        verify(webDriverOptionsMock, times(2)).addCookie(cookieCaptor.capture());
        List<org.openqa.selenium.Cookie> capturedCookies = cookieCaptor.getAllValues();

        Assert.assertEquals("someName", capturedCookies.get(0).getName());
        Assert.assertEquals("someValue", capturedCookies.get(0).getValue());
        Assert.assertEquals("someDomain", capturedCookies.get(0).getDomain());
        Assert.assertEquals("somePath", capturedCookies.get(0).getPath());
        Assert.assertEquals(new Date(10000L), capturedCookies.get(0).getExpiry());
        Assert.assertTrue(capturedCookies.get(0).isSecure());

        Assert.assertEquals("someOtherName", capturedCookies.get(1).getName());
        Assert.assertEquals("someOtherValue", capturedCookies.get(1).getValue());
        Assert.assertEquals("someOtherDomain", capturedCookies.get(1).getDomain());
        Assert.assertEquals("someOtherPath", capturedCookies.get(1).getPath());
        Assert.assertEquals(new Date(10000000000L), capturedCookies.get(1).getExpiry());
        Assert.assertFalse(capturedCookies.get(1).isSecure());
    }

    @Test
    public void shouldSetCookiesThroughJavascript() throws Exception {
        //given
        Cookie cookieOne = new Cookie("someName", "someValue", "someDomain", "somePath", new Date(10000L), true);
        Cookie cookieTwo = new Cookie("someOtherName", "someOtherValue", "someOtherDomain", "someOtherPath", new Date(100000067899L), false);
        //when
        testee.setCookiesPhantomJS(ImmutableList.of(cookieOne, cookieTwo));
        //then
        verify(webDriverMock).executeScript("document.cookie = 'someName=someValue;path=somePath;domain=someDomain;secure;expires=01 Jan 1970 00:00:10 GMT;'");
        verify(webDriverMock).executeScript("document.cookie = 'someOtherName=someOtherValue;path=someOtherPath;domain=someOtherDomain;expires=03 Mar 1973 09:47:47 GMT;'");
    }

    @Test
    public void shouldFillLocalStorage() {
        //given
        Map<String, String> localStorage = ImmutableMap.of("key", "value");
        //when
        testee.setLocalStorage(localStorage);
        //then
        final String localStorageCall = String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value");
        verify(webDriverMock).executeScript(localStorageCall);
    }

    @Test
    public void shouldFillLocalStorageWithDocument() {
        //given
        Map<String, String> localStorage = ImmutableMap.of("key", "{'customerServiceWidgetNotificationHidden':{'value':true,'timestamp':9467812242358}}");
        //when
        testee.setLocalStorage(localStorage);
        //then
        final String localStorageCall = String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "{\"customerServiceWidgetNotificationHidden\":{\"value\":true,\"timestamp\":9467812242358}}");
        verify(webDriverMock).executeScript(localStorageCall);
    }

    @Test
    public void shouldFillSessionStorage() {
        //given
        Map<String, String> sessionStorage = ImmutableMap.of("key", "value");
        //when
        testee.setSessionStorage(sessionStorage);
        //then
        final String sessionStorageCall = String.format(JS_SET_SESSION_STORAGE_CALL, "key", "value");
        verify(webDriverMock).executeScript(sessionStorageCall);
    }

    @Test
    public void shouldScroll() throws InterruptedException {
        //when
        testee.scrollBy(1337);
        //then
        verify(webDriverMock).executeScript(String.format(JS_SCROLL_CALL, 1337L));
    }

    @Test
    public void shouldDoAllTheScreenshotWebdriverCalls() throws Exception {
        //given
        final Long viewportHeight = 500L;
        final Long pageHeight = 2000L;

        UrlConfig urlConfig = new UrlConfig(
                ImmutableList.of("/"),
                0f,
                ImmutableList.of(new Cookie("testcookiename", "testcookievalue")),
                ImmutableMap.of(),
                ImmutableMap.of("key", "value"),
                ImmutableMap.of("key", "value"),
                ImmutableList.of(600, 800),
                5000,
                0,
                0,
                0,
                3,
                "testJS();",
                5);

        Config config = configBuilder()
                .withBrowser(FIREFOX)
                .withUrls(ImmutableMap.of("testurl", urlConfig))
                .withWindowHeight(100)
                .build();
        testee.close();
        testee = new Browser(parameters, config, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("testurl", "/", 600, true, urlConfig);
        ScreenshotContext screenshotContext2 = ScreenshotContext.of("testurl", "/", 800, true, urlConfig);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL)).thenReturn(3L);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL)).thenReturn(false).thenReturn(true);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext, screenshotContext2));

        //then
        verify(webDriverWindowMock, times(2)).setSize(new Dimension(600, 100));
        verify(webDriverWindowMock, times(2)).setSize(new Dimension(800, 100));
        verify(webDriverMock, times(2)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(2)).executeScript("testJS();");
        verify(webDriverMock, times(10)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        verify(webDriverMock, times(5)).get("testurl/");
        verify(webDriverMock, times(2)).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);
        verify(webDriverOptionsMock, times(2)).addCookie(new org.openqa.selenium.Cookie("testcookiename", "testcookievalue"));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SET_SESSION_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(8)).executeScript(String.format(JS_SCROLL_CALL, 500));
    }

    @Test
    public void shouldNotResizeWindowWhenDoingHeadless() throws Exception {
        //given
        final Long viewportHeight = 500L;
        final Long pageHeight = 2000L;

        UrlConfig urlConfig = new UrlConfig(
                ImmutableList.of("/"),
                0f,
                ImmutableList.of(),
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableList.of(600, 800),
                5000,
                0,
                0,
                0,
                3,
                null,
                5);

        Config config = configBuilder()
                .withBrowser(CHROME_HEADLESS)
                .withUrls(ImmutableMap.of("testurl", urlConfig))
                .withWindowHeight(100)
                .build();

        testee.close();
        testee = new Browser(parameters, config, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("testurl", "/", 600, true, urlConfig);
        ScreenshotContext screenshotContext2 = ScreenshotContext.of("testurl", "/", 800, true, urlConfig);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL)).thenReturn(3L);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL)).thenReturn(false).thenReturn(true);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext, screenshotContext2));

        verifyNoMoreInteractions(webDriverWindowMock);
    }

    @Test
    public void shouldGrepChromeDrivers() throws Exception {
        testee.grepChromedrivers();
    }
}