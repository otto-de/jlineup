package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.*;
import de.otto.jlineup.file.FileService;
import org.junit.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.Logs;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static de.otto.jlineup.browser.Browser.*;
import static de.otto.jlineup.browser.Browser.Type.CHROME_HEADLESS;
import static de.otto.jlineup.browser.Browser.Type.FIREFOX;
import static de.otto.jlineup.config.DeviceConfig.*;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
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
    private RunStepConfig runStepConfig;
    @Mock
    private FileService fileService;

    private Browser testee;

    @Before
    public void setup() {
        initMocks(this);

        when(webDriverMock.manage()).thenReturn(webDriverOptionsMock);
        when(webDriverOptionsMock.timeouts()).thenReturn(webDriverTimeoutMock);
        when(webDriverOptionsMock.window()).thenReturn(webDriverWindowMock);
        when(webDriverOptionsMock.logs()).thenReturn(webDriverLogs);
        when(browserUtilsMock.getWebDriverByConfig(any(JobConfig.class), any(RunStepConfig.class))).thenReturn(webDriverMock);
        when(browserUtilsMock.getWebDriverByConfig(any(JobConfig.class), any(RunStepConfig.class), any(DeviceConfig.class))).thenReturn(webDriverMock);
        when(webDriverMock.executeScript(JS_GET_USER_AGENT)).thenReturn("Mocked Webdriver");
        JobConfig jobConfig = jobConfigBuilder().build();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);
        testee.initializeWebDriver();
    }

    @After
    public void cleanup() {
        if (testee != null) {
            testee.close();
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

        assertEquals("someName", capturedCookies.get(0).getName());
        assertEquals("someValue", capturedCookies.get(0).getValue());
        assertEquals("someDomain", capturedCookies.get(0).getDomain());
        assertEquals("somePath", capturedCookies.get(0).getPath());
        assertEquals(new Date(10000L), capturedCookies.get(0).getExpiry());
        Assert.assertTrue(capturedCookies.get(0).isSecure());

        assertEquals("someOtherName", capturedCookies.get(1).getName());
        assertEquals("someOtherValue", capturedCookies.get(1).getValue());
        assertEquals("someOtherDomain", capturedCookies.get(1).getDomain());
        assertEquals("someOtherPath", capturedCookies.get(1).getPath());
        assertEquals(new Date(10000000000L), capturedCookies.get(1).getExpiry());
        assertFalse(capturedCookies.get(1).isSecure());
    }

    @Test
    public void shouldSetCookiesThroughJavascript() {
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
        testee.scrollTo(1337);
        //then
        verify(webDriverMock).executeScript(String.format(JS_SCROLL_TO_CALL, 1337L));
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
                5,
                new HttpCheckConfig(),
                0,
                true);

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX)
                .withUrls(ImmutableMap.of("http://testurl", urlConfig))
                .withWindowHeight(100)
                .build();

        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("http://testurl", "/", deviceConfig(600,100), Step.before, urlConfig);
        ScreenshotContext screenshotContext2 = ScreenshotContext.of("http://testurl", "/", deviceConfig(800, 100), Step.before, urlConfig);

        when(webDriverMock.getCurrentUrl()).thenReturn("http://testurl");
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/http_url_root_ff3c40c_1001_02002_before.png")));
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL)).thenReturn(3L);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL)).thenReturn(false).thenReturn(true);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext, screenshotContext2));

        //then
        verify(webDriverWindowMock, times(2)).setSize(new Dimension(600, 100));
        verify(webDriverWindowMock, times(2)).setSize(new Dimension(800, 100));
        verify(webDriverMock, times(2)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(2)).executeScript("testJS();");
        verify(webDriverMock, times(2)).executeScript(String.format(JS_HIDE_IMAGES, 500));
        verify(webDriverMock, times(10)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        //Two times the cookie -> goes to url
        verify(webDriverMock, times(2)).get("http://testurl");
        //One time cache warmup, then the two calls for the two contexts with full url + subpath
        verify(webDriverMock, times(3)).get("http://testurl/");
        verify(webDriverMock, times(2)).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);
        verify(webDriverOptionsMock, times(2)).addCookie(new org.openqa.selenium.Cookie("testcookiename", "testcookievalue"));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SET_SESSION_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(1)).executeScript(JS_GET_USER_AGENT);
        verify(webDriverMock, times(3)).executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL);
        verify(webDriverMock, times(3)).executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL);
        verify(webDriverMock, times(2)).executeScript(JS_GET_DOM);
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SCROLL_TO_CALL, 500));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SCROLL_TO_CALL, 1000));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SCROLL_TO_CALL, 1500));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SCROLL_TO_CALL, 2000));
        //verifyNoMoreInteractions(webDriverMock);
    }


    @Test
    public void shouldSetCookieForDifferentDomain() throws Exception {
        //given
        final Long viewportHeight = 500L;
        final Long pageHeight = 2000L;

        //Set to something without milliseconds (selenium strips it!)
        Date cookieExpiry = new Date(100000);
        List<Cookie> expectedCookies = Arrays.asList(
                new Cookie("testcookiename", "testcookievalue", "cookieurl", "/", cookieExpiry, false),
                new Cookie("testcookiename2", "testcookievalue2", "anotherCookieurl", "/", cookieExpiry, false),
                new Cookie("testcookiename3", "testcookievalue3")
        );
        UrlConfig urlConfig = new UrlConfig(
                ImmutableList.of("/"),
                0f,
                expectedCookies,
                ImmutableMap.of(),
                ImmutableMap.of("key", "value"),
                ImmutableMap.of("key", "value"),
                ImmutableList.of(600),
                5000,
                0,
                0,
                0,
                3,
                null,
                5,
                new HttpCheckConfig(),
                0,
                false);

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX)
                .withUrls(ImmutableMap.of("http://testurl", urlConfig))
                .withWindowHeight(100)
                .build();
        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("http://testurl", "/", deviceConfig(600, 100), Step.before, urlConfig);

        when(webDriverMock.getCurrentUrl()).thenReturn("http://testurl");
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/http_url_root_ff3c40c_1001_02002_before.png")));
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL)).thenReturn(3L);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL)).thenReturn(false).thenReturn(true);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext));

        //then
        verify(webDriverWindowMock, times(3)).setSize(new Dimension(600, 100));
        verify(webDriverMock, times(1)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(5)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        verify(webDriverMock, times(1)).get("http://cookieurl");
        verify(webDriverMock, times(1)).get("http://anotherCookieurl");
        verify(webDriverMock, times(1)).get("http://testurl");
        verify(webDriverMock, times(2)).get("http://testurl/");
        verify(webDriverMock, times(1)).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);

        ArgumentCaptor<org.openqa.selenium.Cookie> cookieCaptor = ArgumentCaptor.forClass(org.openqa.selenium.Cookie.class);
        verify(webDriverOptionsMock, times(3)).addCookie(cookieCaptor.capture());
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(0), expectedCookies.get(0));
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(1), expectedCookies.get(1));
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(2), expectedCookies.get(2));

        verify(webDriverMock, times(1)).executeScript(String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(1)).executeScript(String.format(JS_SET_SESSION_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(1)).executeScript(String.format(JS_SCROLL_TO_CALL, 500));
        verify(webDriverMock, times(1)).executeScript(String.format(JS_SCROLL_TO_CALL, 1000));
        verify(webDriverMock, times(1)).executeScript(String.format(JS_SCROLL_TO_CALL, 1500));
        verify(webDriverMock, times(1)).executeScript(String.format(JS_SCROLL_TO_CALL, 2000));
    }


    @Test
    public void shouldSetCookieOnHttpsDomainIfOneCookieIsMarkedAsSecure() throws Exception {
        //given
        final Long viewportHeight = 500L;
        final Long pageHeight = 2000L;

        //Set to something without milliseconds (selenium strips it!)
        Date cookieExpiry = new Date(100000);
        List<Cookie> expectedCookies = Arrays.asList(
                new Cookie("testcookiename", "testcookievalue", "cookieurl", "/", cookieExpiry, false),
                new Cookie("testcookiename2", "testcookievalue2", "cookieurl", "/", cookieExpiry, true),
                new Cookie("testcookiename3", "testcookievalue3", "cookieurl", "/", cookieExpiry, false)
        );

        UrlConfig urlConfig = new UrlConfig(
                ImmutableList.of("/"),
                0f,
                expectedCookies,
                ImmutableMap.of(),
                ImmutableMap.of("key", "value"),
                ImmutableMap.of("key", "value"),
                ImmutableList.of(600),
                5000,
                0,
                0,
                0,
                3,
                null,
                5,
                new HttpCheckConfig(),
                0,
                false);

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX)
                .withUrls(ImmutableMap.of("http://testurl", urlConfig))
                .withWindowHeight(100)
                .build();
        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("http://testurl", "/", deviceConfig(600, 100), Step.before, urlConfig);

        when(webDriverMock.getCurrentUrl()).thenReturn("http://cookieurl");
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/http_url_root_ff3c40c_1001_02002_before.png")));
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL)).thenReturn(3L);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL)).thenReturn(false).thenReturn(true);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext));

        //then
        verify(webDriverWindowMock, times(3)).setSize(new Dimension(600, 100));
        verify(webDriverMock, times(1)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(5)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        verify(webDriverMock, times(1)).get("https://cookieurl");
        verify(webDriverMock, times(2)).get("http://testurl/");
        verify(webDriverMock, times(1)).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);

        ArgumentCaptor<org.openqa.selenium.Cookie> cookieCaptor = ArgumentCaptor.forClass(org.openqa.selenium.Cookie.class);
        verify(webDriverOptionsMock, times(3)).addCookie(cookieCaptor.capture());
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(0), expectedCookies.get(0));
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(1), expectedCookies.get(1));
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(2), expectedCookies.get(2));
    }

    private void assertThatCookieContentIsIdentical(org.openqa.selenium.Cookie cookie, Cookie expectedCookie) {
        assertThat(cookie.getName(), is(expectedCookie.name));
        assertThat(cookie.getValue(), is(expectedCookie.value));
        assertThat(cookie.getDomain(), is(expectedCookie.domain));
        assertThat(cookie.getPath(), is(expectedCookie.path != null ? expectedCookie.path : "/"));
        assertThat(cookie.getExpiry(), is(expectedCookie.expiry));
        assertThat(cookie.isSecure(), is(expectedCookie.secure));
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
                5,
                new HttpCheckConfig(),
                0,
                false);

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(CHROME_HEADLESS)
                .withUrls(ImmutableMap.of("testurl", urlConfig))
                .withWindowHeight(100)
                .build();

        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("testurl", "/", deviceConfig(600, 100), Step.before, urlConfig);
        ScreenshotContext screenshotContext2 = ScreenshotContext.of("testurl", "/", deviceConfig(800, 100), Step.before, urlConfig);

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
    @Ignore
    public void shouldGrepChromeDrivers() throws Exception {
        testee.grepChromedrivers();
    }

    private URI getFilePath(String fileName) throws URISyntaxException {
        return Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).toURI();
    }

}