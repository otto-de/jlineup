package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.Logs;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

import static com.google.common.collect.ImmutableMap.of;
import static de.otto.jlineup.browser.Browser.*;
import static de.otto.jlineup.browser.Browser.Type.*;
import static de.otto.jlineup.browser.BrowserStep.before;
import static de.otto.jlineup.config.DeviceConfig.deviceConfig;
import static de.otto.jlineup.config.JobConfig.jobConfigBuilder;
import static de.otto.jlineup.config.UrlConfig.urlConfigBuilder;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
    private Capabilities capabilitiesMock;

    @Mock
    private RunStepConfig runStepConfig;
    @Mock
    private FileService fileService;

    private Browser testee;

    @Before
    public void setup() {
        when(webDriverMock.manage()).thenReturn(webDriverOptionsMock);
        when(webDriverOptionsMock.timeouts()).thenReturn(webDriverTimeoutMock);
        when(webDriverOptionsMock.window()).thenReturn(webDriverWindowMock);
        when(browserUtilsMock.getWebDriverByConfig(any(JobConfig.class), any(RunStepConfig.class))).thenReturn(webDriverMock);
        when(browserUtilsMock.getWebDriverByConfig(any(JobConfig.class), any(RunStepConfig.class), any(DeviceConfig.class))).thenReturn(webDriverMock);
        when(webDriverMock.executeScript(JS_GET_USER_AGENT_CALL)).thenReturn("Mocked Webdriver");
        when(webDriverMock.getCapabilities()).thenReturn(capabilitiesMock);
        when(capabilitiesMock.getBrowserName()).thenReturn("MockBrowser 1 2 3 4.0");
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

        Cookie cookieOne = new Cookie("someName", "someValue", "someDomain", "somePath", new Date(10000L), true, false, false);
        Cookie cookieTwo = new Cookie("someOtherName", "someOtherValue", "someOtherDomain", "someOtherPath", new Date(10000000000L), false, false, false);
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
    public void shouldFillLocalStorage() {
        //given
        Map<String, String> localStorage = of("key", "value");
        //when
        testee.setLocalStorage(localStorage);
        //then
        final String localStorageCall = String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value");
        verify(webDriverMock).executeScript(localStorageCall);
    }

    @Test
    public void shouldFillLocalStorageWithDocument() {
        //given
        Map<String, String> localStorage = of("key", "{'customerServiceWidgetNotificationHidden':{'value':true,'timestamp':9467812242358}}");
        //when
        testee.setLocalStorage(localStorage);
        //then
        final String localStorageCall = String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "{\"customerServiceWidgetNotificationHidden\":{\"value\":true,\"timestamp\":9467812242358}}");
        verify(webDriverMock).executeScript(localStorageCall);
    }

    @Test
    public void shouldFillSessionStorage() {
        //given
        Map<String, String> sessionStorage = of("key", "value");
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
    public void shouldScrollWithScrollDistanceFactor() throws Exception {
        //given
        JobConfig jobConfig = jobConfigBuilder()
                .withUrls(of("otto", urlConfigBuilder()
                        .withUrl("https://www.otto.de")
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder().build()))
                        .withPath("/")
                        .withScrollDistanceFactor(0.1f)
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder()
                                        .withWidth(1000)
                                        .withHeight(673)
                                .build()))
                        .build()))
                .withBrowser(CHROME).build().insertDefaults();

        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(673L);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(673L);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/ideaVertical.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1d);

        //when
        testee.runSetupAndTakeScreenshots();

        //then
        for (int i = 1; i <= 10; i++) {
            verify(webDriverMock, times(1)).executeScript(String.format(JS_SCROLL_TO_CALL, 67 * i));
        }

    }

    @Test
    public void shouldDeleteTheBrowserProfileDirectoryForChrome() throws Exception {
        //given
        JobConfig jobConfig = jobConfigBuilder()
                .withUrls(of("otto", urlConfigBuilder()
                        .withUrl("https://www.otto.de")
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder().build()))
                        .withPath("/")
                        .build()))
                .withBrowser(CHROME).build().insertDefaults();
        when(runStepConfig.isCleanupProfile()).thenReturn(true);
        when(runStepConfig.getChromeParameters()).thenReturn(List.of("--user-data-dir=/tmp/jlineup-test/jlineup/chrome/12345"));
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(1000L);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(1000L);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_1125x750.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1.5d);
        Files.createDirectories(new File("/tmp/jlineup-test/jlineup/chrome/12345").toPath());
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);
        testee.runSetupAndTakeScreenshots();

        //when

        testee.close();

        //then
        assertThat(Files.exists(new File("/tmp/jlineup-test/jlineup/chrome/12345").toPath()), is(false));
    }

    @Test
    public void shouldDeleteTheBrowserProfileDirectoryForFirefoxWithProfileParam() throws Exception {
        //given
        JobConfig jobConfig = jobConfigBuilder()
                .withUrls(of("otto", urlConfigBuilder()
                        .withUrl("https://www.otto.de")
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder().build()))
                        .withPath("/")
                        .build()))
                .withBrowser(FIREFOX).build().insertDefaults();
        when(runStepConfig.isCleanupProfile()).thenReturn(true);
        when(runStepConfig.getFirefoxParameters()).thenReturn(List.of("-profile /tmp/jlineup-test/jlineup/firefox/12345"));
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(1000L);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(1000L);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_1125x750.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1.5d);
        Files.createDirectories(new File("/tmp/jlineup-test/jlineup/firefox/12345").toPath());
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);
        testee.runSetupAndTakeScreenshots();

        //when
        testee.close();

        //then
        assertThat(Files.exists(new File("/tmp/jlineup-test/jlineup/firefox/12345").toPath()), is(false));
    }

    @Test
    public void shouldDeleteTheBrowserProfileDirectoryForFirefoxWithPParam() throws Exception {
        //given
        JobConfig jobConfig = jobConfigBuilder()
                .withUrls(of("otto", urlConfigBuilder()
                        .withUrl("https://www.otto.de")
                        .withDevices(List.of(DeviceConfig.deviceConfigBuilder().build()))
                        .withPath("/")
                        .build()))
                .withBrowser(FIREFOX).build().insertDefaults();
        when(runStepConfig.isCleanupProfile()).thenReturn(true);
        when(runStepConfig.getFirefoxParameters()).thenReturn(List.of("-P /tmp/jlineup-test/jlineup/firefox/12345"));
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(1000L);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(1000L);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_1125x750.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1.5d);
        Files.createDirectories(new File("/tmp/jlineup-test/jlineup/firefox/12345").toPath());
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);
        testee.runSetupAndTakeScreenshots();

        //when
        testee.close();

        //then
        assertThat(Files.exists(new File("/tmp/jlineup-test/jlineup/firefox/12345").toPath()), is(false));
    }

    @Test
    public void shouldDoAllTheScreenshotWebdriverCalls() throws Exception {
        //given
        final Long viewportHeight = 1000L; //Will be overridden by validateViewportHeight(), which uses the (screenshot height / device pixel ratio) (500) as 'truth', and should trigger a WARN message
        final Long pageHeight = 2000L;

        UrlConfig urlConfig = urlConfigBuilder()
                .withPath("/")
                .withCookies(ImmutableList.of(new Cookie("testcookiename", "testcookievalue")))
                .withAlternatingCookies(ImmutableList.of(ImmutableList.of(new Cookie("alternating", "one"))))
                .withLocalStorage(of("localStorageKey", "localStorageValue"))
                .withSessionStorage(of("sessionStorageKey", "sessionStorageValue"))
                .withWarmupBrowserCacheTime(3)
                .withWaitForFontsTime(5)
                .withJavaScript("testJS();")
                .withHideImages(true)
                .withWindowWidths(singletonList(600))
                .build();

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX)
                .withUrls(of("http://testurl", urlConfig))
                .withWindowHeight(100)
                .build();

        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("http://testurl", "/", deviceConfig(600, 100), before, urlConfig, ImmutableList.of(new Cookie("testcookiename", "testcookievalue"), new Cookie("alternating", "one")));
        ScreenshotContext screenshotContext2 = ScreenshotContext.of("http://testurl", "/", deviceConfig(800, 100), before, urlConfig, ImmutableList.of(new Cookie("testcookiename", "testcookievalue"), new Cookie("alternating", "one")));

        when(webDriverMock.getCurrentUrl()).thenReturn("http://testurl");
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_1125x750.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1.5d);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL)).thenReturn(3L);
        when(webDriverMock.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL)).thenReturn(false).thenReturn(true);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext, screenshotContext2));

        //then
        verify(webDriverWindowMock, times(1)).setSize(new Dimension(600, 100));
        verify(webDriverWindowMock, times(1)).setSize(new Dimension(800, 100));
        verify(webDriverMock, times(2)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(2)).executeScript("testJS();");
        verify(webDriverMock, times(2)).executeScript(String.format(JS_HIDE_IMAGES_CALL, 500));
        verify(webDriverMock, times(10)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        //Two times the cookie -> goes to url
        verify(webDriverMock, times(2)).get("http://testurl");
        //Two times cache warmup, then the two calls for the two contexts with full url + subpath
        verify(webDriverMock, times(4)).get("http://testurl/");
        verify(webDriverMock, times(2)).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);
        verify(webDriverOptionsMock, times(2)).addCookie(new org.openqa.selenium.Cookie("testcookiename", "testcookievalue"));
        verify(webDriverOptionsMock, times(2)).addCookie(new org.openqa.selenium.Cookie("alternating", "one"));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SET_LOCAL_STORAGE_CALL, "localStorageKey", "localStorageValue"));
        verify(webDriverMock, times(2)).executeScript(String.format(JS_SET_SESSION_STORAGE_CALL, "sessionStorageKey", "sessionStorageValue"));
        verify(webDriverMock, times(1)).executeScript(JS_GET_USER_AGENT_CALL);
        verify(webDriverMock, times(3)).executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL);
        verify(webDriverMock, times(3)).executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL);
        //TODO: re-enable if dom save feature comes back
        //verify(webDriverMock, times(2)).executeScript(JS_GET_DOM);
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
                new Cookie("testcookiename", "testcookievalue", "cookieurl", "/", cookieExpiry, false, false, false),
                new Cookie("testcookiename2", "testcookievalue2", "anotherCookieurl", "/", cookieExpiry, false, false, false),
                new Cookie("testcookiename3", "testcookievalue3")
        );

        UrlConfig urlConfig = urlConfigBuilder()
                .withPath("/")
                .withCookies(expectedCookies)
                .withLocalStorage(of("localStorageKey", "localStorageValue"))
                .withSessionStorage(of("sessionStorageKey", "sessionStorageValue"))
                .withWindowWidths(singletonList(600))
                .build();

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX)
                .withUrls(of("http://testurl", urlConfig))
                .withWindowHeight(100)
                .build();
        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("http://testurl", "/", deviceConfig(600, 100), before, urlConfig);

        when(webDriverMock.getCurrentUrl()).thenReturn("http://testurl");
        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_750x500.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1d);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext));

        //then
        verify(webDriverWindowMock, times(1)).setSize(new Dimension(600, 100));
        verify(webDriverMock, times(1)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(5)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        verify(webDriverMock, times(1)).get("http://cookieurl");
        verify(webDriverMock, times(1)).get("http://anotherCookieurl");
        verify(webDriverMock, times(1)).get("http://testurl");
        verify(webDriverMock, times(1)).get("http://testurl/");
        verify(webDriverMock, times(1)).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);

        ArgumentCaptor<org.openqa.selenium.Cookie> cookieCaptor = ArgumentCaptor.forClass(org.openqa.selenium.Cookie.class);
        verify(webDriverOptionsMock, times(3)).addCookie(cookieCaptor.capture());
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(0), expectedCookies.get(0));
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(1), expectedCookies.get(1));
        assertThatCookieContentIsIdentical(cookieCaptor.getAllValues().get(2), expectedCookies.get(2));

        verify(webDriverMock, times(1)).executeScript(String.format(JS_SET_LOCAL_STORAGE_CALL, "localStorageKey", "localStorageValue"));
        verify(webDriverMock, times(1)).executeScript(String.format(JS_SET_SESSION_STORAGE_CALL, "sessionStorageKey", "sessionStorageValue"));
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
                new Cookie("testcookiename", "testcookievalue", "cookieurl", "/", cookieExpiry, false, false, false),
                new Cookie("testcookiename2", "testcookievalue2", "cookieurl", "/", cookieExpiry, true, false, false),
                new Cookie("testcookiename3", "testcookievalue3", "cookieurl", "/", cookieExpiry, false, false, false)
        );

        UrlConfig urlConfig = urlConfigBuilder().withPath("/").withWindowWidths(singletonList(600)).withCookies(expectedCookies).build();

        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX)
                .withUrls(of("http://testurl", urlConfig))
                .withWindowHeight(100)
                .build();
        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("http://testurl", "/", deviceConfig(600, 100), before, urlConfig);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_750x500.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1d);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext));

        //then
        verify(webDriverWindowMock, times(1)).setSize(new Dimension(600, 100));
        verify(webDriverMock, times(1)).executeScript(JS_SCROLL_TO_TOP_CALL);
        verify(webDriverMock, times(5)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        verify(webDriverMock, times(1)).get("https://cookieurl");
        verify(webDriverMock, times(1)).get("http://testurl/");
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
    public void shouldNotResizeWindowWhenDoingHeadlessFirefox() throws Exception {
        //given
        final Long viewportHeight = 500L;
        final Long pageHeight = 2000L;

        UrlConfig urlConfig = urlConfigBuilder().withPath("/").withWindowWidths(ImmutableList.of(600, 800)).build();
        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX_HEADLESS)
                .withUrls(of("testurl", urlConfig))
                .withWindowHeight(100)
                .build();

        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("testurl", "/", deviceConfig(600, 100), before, urlConfig);
        ScreenshotContext screenshotContext2 = ScreenshotContext.of("testurl", "/", deviceConfig(800, 100), before, urlConfig);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_750x500.png")));
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1d);

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext, screenshotContext2));

        verifyNoMoreInteractions(webDriverWindowMock);
    }

    @Test
    public void shouldExecuteSpecialJLineupJS() throws Exception {
        //given
        final Long viewportHeight = 2500L;
        final Long pageHeight = 2000L;

        UrlConfig urlConfig = urlConfigBuilder()
                .withPath("/")
                .withWindowWidths(ImmutableList.of(600))
                .withJavaScript("console.log(1);jlineup.sleep(5);console.log(2);console.log(3);jlineup.sleep(2);console.log(4);jlineup.sleep(1)")
                .build();
        JobConfig jobConfig = jobConfigBuilder()
                .withBrowser(FIREFOX_HEADLESS)
                .withUrls(of("testurl", urlConfig))
                .withWindowHeight(2500)
                .build();

        testee.close();
        testee = new Browser(runStepConfig, jobConfig, fileService, browserUtilsMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("testurl", "/", deviceConfig(600, 100), before, urlConfig);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(pageHeight);
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(viewportHeight);
        when(webDriverMock.executeScript(JS_GET_DEVICE_PIXEL_RATIO_CALL)).thenReturn(1d);
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File(getFilePath("screenshots/test_image_750x500.png")));

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext));

        verify(webDriverMock, times(1)).executeScript("console.log(1);");
        verify(webDriverMock, times(1)).executeScript("/* sleeping 5 milliseconds */");
        verify(webDriverMock, times(1)).executeScript(";console.log(2);console.log(3);");
        verify(webDriverMock, times(1)).executeScript("/* sleeping 2 milliseconds */");
        verify(webDriverMock, times(1)).executeScript(";console.log(4);");
        verify(webDriverMock, times(1)).executeScript("/* sleeping 1 milliseconds */");

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