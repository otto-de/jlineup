package de.otto.jlineup.browser;

import com.google.gson.annotations.SerializedName;
import de.otto.jlineup.Util;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.*;
import org.openqa.selenium.Point;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static de.otto.jlineup.file.FileService.AFTER;
import static de.otto.jlineup.file.FileService.BEFORE;

public class Browser implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Browser.class);
    public static final int THREADPOOL_SUBMIT_SHUFFLE_TIME_IN_MS = 233;
    public static final int DEFAULT_SLEEP_AFTER_SCROLL_MILLIS = 50;
    public static final String JS_RETURN_DOCUMENT_FONTS_SIZE_CALL = "return document.fonts.size;";
    public static final String JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL = "return document.fonts.status === 'loaded';";


    public enum Type {
        @SerializedName(value = "Firefox", alternate = {"firefox", "FIREFOX"})
        FIREFOX,
        @SerializedName(value = "Chrome", alternate = {"chrome", "CHROME"})
        CHROME,
        @SerializedName(value = "PhantomJS", alternate = {"phantomjs", "PHANTOMJS"})
        PHANTOMJS;
    }

    static final String JS_DOCUMENT_HEIGHT_CALL = "return Math.max( document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight );";

    static final String JS_CLIENT_VIEWPORT_HEIGHT_CALL = "return document.documentElement.clientHeight";
    static final String JS_SET_LOCAL_STORAGE_CALL = "localStorage.setItem('%s','%s')";
    static final String JS_SET_SESSION_STORAGE_CALL = "sessionStorage.setItem('%s','%s')";
    static final String JS_SCROLL_CALL = "window.scrollBy(0,%d)";
    static final String JS_SCROLL_TO_TOP_CALL = "window.scrollTo(0, 0);";
    final private Parameters parameters;

    final private Config config;
    final private FileService fileService;
    final private BrowserUtils browserUtils;
    /* Every thread has it's own WebDriver and cache warmup marks, this is manually managed through concurrent maps */
    private ExecutorService threadPool;

    private ConcurrentHashMap<String, WebDriver> webDrivers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> cacheWarmupMarksMap = new ConcurrentHashMap<>();

    public Browser(Parameters parameters, Config config, FileService fileService, BrowserUtils browserUtils) {
        this.parameters = parameters;
        this.config = config;
        this.fileService = fileService;
        this.browserUtils = browserUtils;
        this.threadPool = Util.createThreadPool(config.threads, "BrowserThread");
    }

    @Override
    public void close() throws Exception {
        webDrivers.values().forEach(WebDriver::close);
        webDrivers.values().forEach(WebDriver::quit);
        webDrivers.clear();
    }

    public void takeScreenshots() throws IOException, InterruptedException, ExecutionException {
        boolean before = !parameters.isAfter();
        List<ScreenshotContext> screenshotContextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(parameters, config, before);
        if (screenshotContextList.size() > 0) {
            takeScreenshots(screenshotContextList);
        }
    }

    void takeScreenshots(final List<ScreenshotContext> screenshotContextList) throws IOException, InterruptedException, ExecutionException {
        for (final ScreenshotContext screenshotContext : screenshotContextList) {
            final Future<?> takeScreenshotsResult = threadPool.submit(() -> {
                try {
                    takeScreenshotsForContext(screenshotContext);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            });
            //submit screenshots to the browser with a slight delay, so not all instances open up in complete sync
            Thread.sleep(THREADPOOL_SUBMIT_SHUFFLE_TIME_IN_MS);
        }
        threadPool.shutdown();
        threadPool.awaitTermination(15, TimeUnit.MINUTES);
    }

    private void takeScreenshotsForContext(final ScreenshotContext screenshotContext) throws InterruptedException, IOException {

        final WebDriver localDriver = getWebDriver();

        moveMouseToZeroZero();

        localDriver.manage().window().setPosition(new Point(0, 0));
        resizeBrowser(localDriver, screenshotContext.windowWidth, config.windowHeight);

        final String url = buildUrl(screenshotContext.url, screenshotContext.path, screenshotContext.urlConfig.envMapping);
        final String rootUrl = buildUrl(screenshotContext.url, "/", screenshotContext.urlConfig.envMapping);

        if (areThereCookiesOrStorage(screenshotContext)) {
            //get root page from url to be able to set cookies afterwards
            //if you set cookies before getting the page once, it will fail
            LOG.info(String.format("Getting root url: %s to set cookies, local and session storage", rootUrl));
            localDriver.get(rootUrl);

            //set cookies and local storage
            setCookies(screenshotContext);
            setLocalStorage(screenshotContext);
            setSessionStorage(screenshotContext);
        }

        checkBrowserCacheWarmup(screenshotContext, url, localDriver);

        //now get the real page
        LOG.info(String.format("Browsing to %s with window size %dx%d", url, screenshotContext.windowWidth, config.windowHeight));

        //Selenium's get() method blocks until the browser/page fires an onload event (files and images referenced in the html have been loaded,
        //but there might be JS calls that load more stuff dynamically afterwards).
        localDriver.get(url);

        Long pageHeight = getPageHeight();
        final Long viewportHeight = getViewportHeight();

        if (screenshotContext.urlConfig.waitAfterPageLoad > 0) {
            try {
                LOG.debug(String.format("Waiting for %d seconds (wait-after-page-load)", screenshotContext.urlConfig.waitAfterPageLoad));
                Thread.sleep(screenshotContext.urlConfig.waitAfterPageLoad * 1000);
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (config.globalWaitAfterPageLoad > 0) {
            LOG.debug(String.format("Waiting for %s seconds (global wait-after-page-load)", config.globalWaitAfterPageLoad));
            Thread.sleep(Math.round(config.globalWaitAfterPageLoad * 1000));
        }

        LOG.debug("Page height before scrolling: {}", pageHeight);
        LOG.debug("Viewport height of browser window: {}", viewportHeight);

        scrollToTop();

        //Execute custom javascript if existing
        executeJavaScript(screenshotContext.urlConfig.javaScript);

        //Wait for fonts
        if (screenshotContext.urlConfig.waitForFontsTime > 0) {
            WebDriverWait wait = new WebDriverWait(getWebDriver(), screenshotContext.urlConfig.waitForFontsTime);
            wait.until(fontsLoaded);
        }

        for (int yPosition = 0; yPosition < pageHeight && yPosition <= screenshotContext.urlConfig.maxScrollHeight; yPosition += viewportHeight) {
            BufferedImage currentScreenshot = takeScreenshot();
            currentScreenshot = waitForNoAnimation(screenshotContext, currentScreenshot);
            fileService.writeScreenshot(currentScreenshot, screenshotContext.url,
                    screenshotContext.path, screenshotContext.windowWidth, yPosition, screenshotContext.before ? BEFORE : AFTER);
            //PhantomJS (until now) always makes full page screenshots, so no scrolling and multi-screenshooting
            //This is subject to change because W3C standard wants viewport screenshots
            if (config.browser == Type.PHANTOMJS) {
                break;
            }
            LOG.debug("topOfViewport: {}, pageHeight: {}", yPosition, pageHeight);
            scrollBy(viewportHeight.intValue());
            if (screenshotContext.urlConfig.waitAfterScroll > 0) {
                TimeUnit.SECONDS.sleep(screenshotContext.urlConfig.waitAfterScroll);
            }

            //Refresh to check if page grows during scrolling
            pageHeight = getPageHeight();
        }
    }

    private void resizeBrowser(WebDriver localDriver, int width, int height) {
        localDriver.manage().window().setSize(new Dimension(width, height));
    }

    private WebDriver initializeWebDriver() {
        final WebDriver driver = browserUtils.getWebDriverByConfig(config);
        driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        return driver;
    }

    private Set<String> initializeCacheWarmupMarks() {
        return new HashSet<>();
    }

    private boolean areThereCookiesOrStorage(ScreenshotContext screenshotContext) {
        return (screenshotContext.urlConfig.cookies != null && screenshotContext.urlConfig.cookies.size() > 0)
                || (screenshotContext.urlConfig.localStorage != null && screenshotContext.urlConfig.localStorage.size() > 0)
                || (screenshotContext.urlConfig.sessionStorage != null && screenshotContext.urlConfig.sessionStorage.size() > 0);
    }

    private void setCookies(ScreenshotContext screenshotContext) {
        if (config.browser == Type.PHANTOMJS) {
            //current phantomjs driver has a bug that prevents selenium's normal way of setting cookies
            LOG.debug("Setting cookies for PhantomJS");
            setCookiesPhantomJS(screenshotContext.urlConfig.cookies);
        } else {
            LOG.debug("Setting cookies");
            setCookies(screenshotContext.urlConfig.cookies);
        }
    }

    private void checkBrowserCacheWarmup(ScreenshotContext screenshotContext, String url, WebDriver driver) {
        int warmupTime = screenshotContext.urlConfig.warmupBrowserCacheTime;
        if (warmupTime > Config.DEFAULT_WARMUP_BROWSER_CACHE_TIME) {
            final Set<String> browserCacheWarmupMarks = cacheWarmupMarksMap.computeIfAbsent(Thread.currentThread().getName(), k -> initializeCacheWarmupMarks());
            if (!browserCacheWarmupMarks.contains(url)) {
                final Integer maxWidth = screenshotContext.urlConfig.windowWidths.stream().max(Integer::compareTo).get();
                LOG.info(String.format("Browsing to %s with window size %dx%d for cache warmup", url, maxWidth, config.windowHeight));
                resizeBrowser(driver, maxWidth, config.windowHeight);
                driver.get(url);
                LOG.debug(String.format("First call of %s - waiting %d seconds for cache warmup", url, warmupTime));
                browserCacheWarmupMarks.add(url);
                try {
                    Thread.sleep(warmupTime * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                resizeBrowser(driver, screenshotContext.windowWidth, config.windowHeight);
                LOG.debug("Cache warmup time is over. Getting " + url + " again.");
            }
        }
    }

    private BufferedImage takeScreenshot() throws IOException {
        File screenshot = ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.FILE);
        return ImageIO.read(screenshot);
    }

    private BufferedImage waitForNoAnimation(ScreenshotContext screenshotContext, BufferedImage currentScreenshot) throws IOException {
        File screenshot;
        float waitForNoAnimation = screenshotContext.urlConfig.waitForNoAnimationAfterScroll;
        if (waitForNoAnimation > 0f) {
            final long beginTime = System.currentTimeMillis();
            int sameCounter = 0;
            while (sameCounter < 10 && !timeIsOver(beginTime, waitForNoAnimation)) {
                screenshot = ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.FILE);
                BufferedImage newScreenshot = ImageIO.read(screenshot);
                if (ImageService.bufferedImagesEqualQuick(newScreenshot, currentScreenshot)) {
                    sameCounter++;
                }
                currentScreenshot = newScreenshot;
            }
        }
        return currentScreenshot;
    }

    private boolean timeIsOver(long beginTime, float waitForNoAnimation) {
        boolean over = beginTime + (long) (waitForNoAnimation * 1000L) < System.currentTimeMillis();
        if (over) LOG.debug("Time is over");
        return over;
    }

    private Long getPageHeight() {
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        return (Long) (jse.executeScript(JS_DOCUMENT_HEIGHT_CALL));
    }

    private Long getViewportHeight() {
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        return (Long) (jse.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL));
    }

    void executeJavaScript(String javaScript) throws InterruptedException {
        if (javaScript == null) {
            return;
        }
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        jse.executeScript(javaScript);
        Thread.sleep(50);
    }

    void scrollBy(int viewportHeight) throws InterruptedException {
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        jse.executeScript(String.format(JS_SCROLL_CALL, viewportHeight));
        //Sleep some milliseconds to give scrolling time before the next screenshot happens
        Thread.sleep(DEFAULT_SLEEP_AFTER_SCROLL_MILLIS);
    }

    void scrollToTop() throws InterruptedException {
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        jse.executeScript(JS_SCROLL_TO_TOP_CALL);
        //Sleep some milliseconds to give scrolling time before the next screenshot happens
        Thread.sleep(50);
    }

    private void setLocalStorage(ScreenshotContext screenshotContext) {
        setLocalStorage(screenshotContext.urlConfig.localStorage);
    }

    private void setSessionStorage(ScreenshotContext screenshotContext) {
        setSessionStorage(screenshotContext.urlConfig.sessionStorage);
    }

    void setLocalStorage(Map<String, String> localStorage) {
        if (localStorage == null) return;

        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        for (Map.Entry<String, String> localStorageEntry : localStorage.entrySet()) {

            final String entry = localStorageEntry.getValue().replace("'", "\"");

            String jsCall = String.format(JS_SET_LOCAL_STORAGE_CALL, localStorageEntry.getKey(), entry);
            jse.executeScript(jsCall);
            LOG.debug("LocalStorage call: {}", jsCall);
        }
    }

    void setSessionStorage(Map<String, String> sessionStorage) {
        if (sessionStorage == null) return;

        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        for (Map.Entry<String, String> sessionStorageEntry : sessionStorage.entrySet()) {

            final String entry = sessionStorageEntry.getValue().replace("'", "\"");

            String jsCall = String.format(JS_SET_SESSION_STORAGE_CALL, sessionStorageEntry.getKey(), entry);
            jse.executeScript(jsCall);
            LOG.debug("SessionStorage call: {}", jsCall);
        }
    }

    void setCookies(List<Cookie> cookies) {
        if (cookies == null) return;
        for (Cookie cookie : cookies) {
            org.openqa.selenium.Cookie.Builder cookieBuilder = new org.openqa.selenium.Cookie.Builder(cookie.name, cookie.value);
            if (cookie.domain != null) cookieBuilder.domain(cookie.domain);
            if (cookie.path != null) cookieBuilder.path(cookie.path);
            if (cookie.expiry != null) cookieBuilder.expiresOn(cookie.expiry);
            cookieBuilder.isSecure(cookie.secure);
            getWebDriver().manage().addCookie(cookieBuilder.build());
        }
    }

    void setCookiesPhantomJS(List<Cookie> cookies) {
        if (cookies == null) return;
        for (Cookie cookie : cookies) {
            StringBuilder cookieCallBuilder = new StringBuilder(String.format("document.cookie = '%s=%s;", cookie.name, cookie.value));
            if (cookie.path != null) {
                cookieCallBuilder.append("path=");
                cookieCallBuilder.append(cookie.path);
                cookieCallBuilder.append(";");
            }
            if (cookie.domain != null) {
                cookieCallBuilder.append("domain=");
                cookieCallBuilder.append(cookie.domain);
                cookieCallBuilder.append(";");
            }
            if (cookie.secure) {
                cookieCallBuilder.append("secure;");
            }
            if (cookie.expiry != null) {
                cookieCallBuilder.append("expires=");

                SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                String asGmt = df.format(cookie.expiry.getTime()) + " GMT";
                cookieCallBuilder.append(asGmt);
                cookieCallBuilder.append(";");
            }
            cookieCallBuilder.append("'");

            ((JavascriptExecutor) getWebDriver()).executeScript(cookieCallBuilder.toString());
        }
    }

    private WebDriver getWebDriver() {
        return webDrivers.computeIfAbsent(Thread.currentThread().getName(), k -> initializeWebDriver());
    }

    private void moveMouseToZeroZero() {
        Robot robot = null;
        try {
            robot = new Robot();
            robot.mouseMove(0, 0);
        } catch (AWTException e) {
            LOG.error("Can't move mouse to 0,0", e);
        }
    }

    // wait for fonts to load
    private ExpectedCondition<Boolean> fontsLoaded = new ExpectedCondition<Boolean>() {
        @Override
        public Boolean apply(WebDriver driver) {
            final JavascriptExecutor javascriptExecutor = (JavascriptExecutor) getWebDriver();
            final Long fontsLoadedCount = (Long) javascriptExecutor.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL);
            final Boolean fontsLoaded = (Boolean) javascriptExecutor.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL);
            LOG.debug("Amount of fonts in document: {}", fontsLoadedCount);
            LOG.debug("Fonts loaded: {} ", fontsLoaded);
            return fontsLoaded;
        }
    };
}
