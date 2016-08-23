package de.otto.jlineup.browser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.files.FileUtilsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.MarionetteDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Files.equal;
import static de.otto.jlineup.browser.Browser.*;
import static de.otto.jlineup.browser.Browser.Type.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
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
    private Parameters parameters;

    private Browser testee;

    private String tempDirPath;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDirPath = tempDir.getPath() + "/lineupbrowsertest";

        Files.createDirectories(Paths.get(tempDirPath));
        Files.createDirectories(Paths.get(tempDirPath + "/screenshots"));

        when(webDriverMock.manage()).thenReturn(webDriverOptionsMock);
        when(webDriverOptionsMock.timeouts()).thenReturn(webDriverTimeoutMock);
        when(webDriverOptionsMock.window()).thenReturn(webDriverWindowMock);
        Config config = new Config(null, Browser.Type.PHANTOMJS, 0f, 100);
        testee = new Browser(parameters, config, webDriverMock);
        when(parameters.getWorkingDirectory()).thenReturn(tempDirPath);
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");
    }

    @After
    public void cleanup() throws IOException {
        FileUtilsTest.deleteIfExists(Paths.get(tempDirPath + "/screenshots"));
        FileUtilsTest.deleteIfExists(Paths.get(tempDirPath));
        if (testee != null) {
            testee.close();
        }
    }

    @Test
    public void shouldGenerateFilename() throws Exception {
        String outputString = Browser.generateScreenshotFileName("https://www.otto.de/", "multimedia", 1000, 2000, "after");
        assertThat(outputString, is("www_otto_de_multimedia_1000_2000_after.png"));
    }

    @Test
    public void shouldConvertRoot() throws Exception {
        String outputString = Browser.generateScreenshotFileName("https://www.otto.de/", "/", 1000, 2000, "before");
        assertThat(outputString, is("www_otto_de_root_1000_2000_before.png"));
    }

    @Test
    @Ignore //TODO: Find out why this doesn't work in conjunction with other tests
    public void shouldGetFirefoxDriver() throws InterruptedException {
        final Config config = new Config(null, FIREFOX, 5f, 800);
        assertSetDriverType(config, MarionetteDriver.class);
    }

    @Test
    public void shouldGetChromeDriver() throws InterruptedException {
        final Config config = new Config(null, CHROME, 5f, 800);
        assertSetDriverType(config, ChromeDriver.class);
    }

    @Test
    public void shouldGetPhantomJSDriver() throws InterruptedException {
        final Config config = new Config(null, PHANTOMJS, 5f, 800);
        assertSetDriverType(config, PhantomJSDriver.class);
    }

    private void assertSetDriverType(Config config, Class<? extends WebDriver> driverClass) {
        WebDriver driver = null;
        try {
            driver = Browser.getWebDriverByConfig(config);
            assertTrue(driverClass.isInstance(driver));
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    @Test
    public void shouldGenerateFullPathToPngFile() {
        Parameters parameters = mock(Parameters.class);
        Config config = new Config(null, Browser.Type.PHANTOMJS, 0f, 100);
        when(parameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");
        Browser browser = new Browser(parameters, config, webDriverMock);
        final String fullFileNameWithPath = browser.getFullScreenshotFileNameWithPath("testurl", "/", 1001, 2002, "step");
        assertThat(fullFileNameWithPath, is("some/working/dir/screenshots/testurl_root_1001_2002_step.png"));
        browser.close();
    }

    @Test
    public void shouldGenerateScreenshotsParameters() throws FileNotFoundException {
        //given
        Parameters parameters = mock(Parameters.class);
        Config config = Config.readConfig(".", "src/test/resources/lineup_test.json");
        when(parameters.getWorkingDirectory()).thenReturn("some/working/dir");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");

        final List<ScreenshotContext> expectedScreenshotContextList = ImmutableList.of(
                ScreenshotContext.of("https://www.otto.de", "/", 600, true),
                ScreenshotContext.of("https://www.otto.de", "/", 800, true),
                ScreenshotContext.of("https://www.otto.de", "/", 1200, true),
                ScreenshotContext.of("https://www.otto.de", "multimedia", 600, true),
                ScreenshotContext.of("https://www.otto.de", "multimedia", 800, true),
                ScreenshotContext.of("https://www.otto.de", "multimedia", 1200, true),
                ScreenshotContext.of("http://www.google.de", "/", 1200, true)
        );

        final List<ScreenshotContext> screenshotContextList = Browser.generateScreenshotsParametersFromConfig(config, true);

        assertThat(screenshotContextList, containsInAnyOrder(expectedScreenshotContextList.toArray()));
    }

    @Test
    public void shouldGenerateDifferenceImage() throws IOException {

        //given
        when(parameters.getWorkingDirectory()).thenReturn("src/test/resources");
        //when
        double difference = testee.generateDifferenceImage("url", "/", 1001, 2002, 800);

        //then
        final String generatedDifferenceImagePath = testee.getFullScreenshotFileNameWithPath("url", "/", 1001, 2002, "DIFFERENCE");
        final String referenceDifferenceImagePath = testee.getFullScreenshotFileNameWithPath("url", "/", 1001, 2002, "DIFFERENCE_reference");
        assertThat(equal(new File(generatedDifferenceImagePath), new File(referenceDifferenceImagePath)), is(true));
        assertThat(difference, is(0.07005));

        Files.delete(Paths.get(generatedDifferenceImagePath));
        testee.close();
    }

    @Test
    public void shouldSetCookies() {
        //given
        Cookie cookieOne = new Cookie("someName", "someValue", "someDomain", "somePath", new Date(0L), true);
        Cookie cookieTwo = new Cookie("someOtherName", "someOtherValue", "someOtherDomain", "someOtherPath", new Date(1L), false);
        //when
        testee.setCookies(ImmutableList.of(cookieOne, cookieTwo));
        //then
        verify(webDriverOptionsMock).addCookie(new org.openqa.selenium.Cookie("someName", "someValue", "someDomain", "somePath", new Date(0L), true));
        verify(webDriverOptionsMock).addCookie(new org.openqa.selenium.Cookie("someOtherName", "someOtherValue", "someOtherDomain", "someOtherPath", new Date(1L)));
    }

    @Test
    public void shouldFillLocalStorage() {
        //given
        Map<String, String> localStorage = ImmutableMap.of("key", "value");
        //when
        testee.setLocalStorage(webDriverMock, localStorage);
        //then
        final String localStorageCall = String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value");
        verify(webDriverMock).executeScript(localStorageCall);
    }

    @Test
    public void shouldFillLocalStorageWithDocument() {
        //given
        Map<String, String> localStorage = ImmutableMap.of("key", "{'customerServiceWidgetNotificationHidden':{'value':true,'timestamp':9467812242358}}");
        //when
        testee.setLocalStorage(webDriverMock, localStorage);
        //then
        final String localStorageCall = String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "{\"customerServiceWidgetNotificationHidden\":{\"value\":true,\"timestamp\":9467812242358}}");
        verify(webDriverMock).executeScript(localStorageCall);
    }

    @Test
    public void shouldScroll() {
        //when
        testee.scrollBy(webDriverMock, 1337);
        //then
        verify(webDriverMock).executeScript(String.format(JS_SCROLL_CALL, 1337L));
    }

    @Test
    public void shouldDoAllTheScreenshotWebdriverCalls() throws Exception {
        //given
        final int viewportHeight = 500;
        final int pageHeight = 2000;

        UrlConfig urlConfig = new UrlConfig(
                ImmutableList.of("/"),
                0f,
                ImmutableList.of(new Cookie("testcookiename", "testcookievalue")),
                ImmutableMap.of(), ImmutableMap.of("key", "value"),
                ImmutableList.of(600), 5000, 0);

        Config config = new Config(ImmutableMap.of("testurl", urlConfig), Browser.Type.FIREFOX, 0f, 100);
        testee = new Browser(parameters, config, webDriverMock);

        ScreenshotContext screenshotContext = ScreenshotContext.of("testurl", "/", 600, true);

        when(webDriverMock.executeScript(JS_DOCUMENT_HEIGHT_CALL)).thenReturn(new Long(pageHeight));
        when(webDriverMock.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL)).thenReturn(new Long(viewportHeight));
        when(webDriverMock.getScreenshotAs(OutputType.FILE)).thenReturn(new File("src/test/resources/screenshots/url_root_1001_2002_before.png"));

        //when
        testee.takeScreenshots(ImmutableList.of(screenshotContext));

        //then
        verify(webDriverMock, times(5)).executeScript(JS_DOCUMENT_HEIGHT_CALL);
        verify(webDriverMock).executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL);
        verify(webDriverOptionsMock).addCookie(new org.openqa.selenium.Cookie("testcookiename", "testcookievalue"));
        verify(webDriverMock).executeScript(String.format(JS_SET_LOCAL_STORAGE_CALL, "key", "value"));
        verify(webDriverMock, times(4)).executeScript(String.format(JS_SCROLL_CALL, 500));
    }

    @Test
    public void shouldBuildUrl() {
        final String url = buildUrl("url", "path");
        assertThat(url, is("url/path"));
    }

    @Test
    public void shouldStripUnnecessarySlashesFromUrl() {
        final String url = buildUrl("url/", "/path");
        assertThat(url, is("url/path"));
    }

    @Test
    public void shouldReplaceEnvMappingsCorrectly() {

        Map<String, String> envMapping = ImmutableMap.of("originalOne", "replacementOne", "originalTwo", "replacementTwo");

        final String urlOne = buildUrl("https://originalOne.otto.de", "/", envMapping);
        final String urlTwo = buildUrl("http://originalTwo.otto.de", "/", envMapping);
        final String urlThree = buildUrl("http://mega.originalOne.otto.de", "/", envMapping);

        assertThat(urlOne, is("https://replacementOne.otto.de/"));
        assertThat(urlTwo, is("http://replacementTwo.otto.de/"));
        assertThat(urlThree, is("http://mega.replacementOne.otto.de/"));

    }
}