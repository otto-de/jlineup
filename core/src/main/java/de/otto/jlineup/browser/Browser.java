package de.otto.jlineup.browser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.config.Cookie;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.graalvm.nativeimage.ImageInfo;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.otto.jlineup.JLineupRunner.LOGFILE_NAME;
import static de.otto.jlineup.JLineupRunner.REPORT_LOG_NAME_KEY;
import static de.otto.jlineup.browser.BrowserUtils.buildUrl;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.groupingBy;

public class Browser implements AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static final int THREAD_POOL_SUBMIT_SHUFFLE_TIME_IN_MS = 233;
    public static final int DEFAULT_SLEEP_AFTER_SCROLL_MILLIS = 50;
    public static final int DEFAULT_IMPLICIT_WAIT_TIME_IN_SECONDS = 60;
    public static final String JLINEUP_SLEEP_JS_OPENER = "jlineup.sleep";

    public enum Type {
        @JsonProperty(value = "Firefox")
        FIREFOX,
        @JsonProperty(value = "Firefox-Headless")
        FIREFOX_HEADLESS,
        @JsonProperty(value = "Chrome")
        CHROME,
        @JsonProperty(value = "Chrome-Headless")
        CHROME_HEADLESS,
        @JsonProperty(value = "PhantomJS")
        PHANTOMJS;

        public boolean isFirefox() {
            return this == FIREFOX || this == FIREFOX_HEADLESS;
        }

        public boolean isChrome() {
            return this == CHROME || this == CHROME_HEADLESS;
        }

        public boolean isPhantomJS() {
            return this == PHANTOMJS;
        }

        public boolean isHeadlessRealBrowser() {
            return this == FIREFOX_HEADLESS || this == CHROME_HEADLESS;
        }

        public boolean isHeadless() {
            return isHeadlessRealBrowser() || isPhantomJS();
        }

        @JsonCreator
        public static Type forValue(String value) {
            String browserNameEnum = value
                    .toUpperCase()
                    .replace("-", "_");
            return Browser.Type.valueOf(browserNameEnum);
        }
    }

    static final String JS_HIDE_IMAGES = "document.querySelectorAll(\"img\").forEach(img => img.style.visibility=\"hidden\");";
    static final String JS_DOCUMENT_HEIGHT_CALL = "return Math.max( document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight );";

    static final String JS_CLIENT_VIEWPORT_HEIGHT_CALL = "return window.innerHeight";
    static final String JS_SET_LOCAL_STORAGE_CALL = "localStorage.setItem('%s','%s')";
    static final String JS_SET_SESSION_STORAGE_CALL = "sessionStorage.setItem('%s','%s')";
    static final String JS_SCROLL_TO_CALL = "window.scrollTo(0,%d)";
    static final String JS_SCROLL_TO_TOP_CALL = "window.scrollTo(0, 0);";
    static final String JS_RETURN_DOCUMENT_FONTS_SIZE_CALL = "return document.fonts.size;";
    static final String JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL = "return document.fonts.status === 'loaded';";
    static final String JS_GET_USER_AGENT = "return navigator.userAgent;";

    static final String JS_GET_DOM = "return document.getElementsByTagName('body')[0].innerHTML;";

    static final String JS_REMOVE_FROM_DOM_CALL = "document.querySelectorAll('%s').forEach(el => el.remove());";
    static final String JS_CHECK_FOR_ELEMENT_CALL = "return document.querySelector('%s') !== null;";

    private final JobConfig jobConfig;
    private final FileService fileService;
    private final BrowserUtils browserUtils;
    private final RunStepConfig runStepConfig;
    private final LogErrorChecker logErrorChecker;

    /* Every thread has it's own WebDriver and cache warmup marks, this is manually managed through concurrent maps */
    private ExecutorService threadPool;
    private final ConcurrentHashMap<String, WebDriver> webDrivers = new ConcurrentHashMap<>();

    private final AtomicBoolean shutdownCalled = new AtomicBoolean(false);

    public Browser(RunStepConfig runStepConfig, JobConfig jobConfig, FileService fileService, BrowserUtils browserUtils) {
        this.runStepConfig = runStepConfig;
        this.jobConfig = jobConfig;
        this.fileService = fileService;
        this.browserUtils = browserUtils;
        this.threadPool = Utils.createThreadPool(jobConfig.threads, "BrowserThread");
        this.logErrorChecker = new LogErrorChecker();
    }

    @Override
    public void close() {
        LOG.debug("Closing webdrivers.");
        shutdownCalled.getAndSet(true);
        synchronized (webDrivers) {
            LOG.debug("Setting shutdown called to true");
            webDrivers.forEach((threadName, webDriver) -> {
                LOG.debug("Removing webdriver for thread {} ({})", threadName, webDriver.getClass().getCanonicalName());
                try {
                    webDriver.close();
                } catch (Exception e) {
                    LOG.error("Exception while closing webdriver: " + e.getMessage(), e);
                }
                try {
                    webDriver.quit();
                } catch (Exception e) {
                    LOG.error("Exception while quitting webdriver: " + e.getMessage(), e);
                }
            });
            webDrivers.clear();
            //grepChromedrivers();
        }
        LOG.debug("Closing webdrivers done.");
    }

    public void runSetupAndTakeScreenshots() throws Exception {
        List<ScreenshotContext> testSetupContexts = BrowserUtils.buildTestSetupContexts(runStepConfig, jobConfig);
        List<ScreenshotContext> testCleanupContexts = BrowserUtils.buildTestCleanupContexts(runStepConfig, jobConfig);
        List<ScreenshotContext> screenshotContextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);
        if (screenshotContextList.size() > 0) {
            try {
                if (!testSetupContexts.isEmpty()) {
                    LOG.debug("Running test setup.");
                    runTestSetupOrCleanup(testSetupContexts);
                    LOG.debug("Test setup done.");
                }
                takeScreenshots(screenshotContextList);
            } finally {
                if (!testCleanupContexts.isEmpty()) {
                    LOG.debug("Running test cleanup.");
                    runTestSetupOrCleanup(testCleanupContexts);
                    LOG.debug("Test cleanup done.");
                }
            }
        }
    }

    private void runTestSetupOrCleanup(List<ScreenshotContext> testSetupContexts) throws Exception {
        JLineupHttpClient jLineupHttpClient = new JLineupHttpClient();
        for (ScreenshotContext testSetupContext : testSetupContexts) {
            jLineupHttpClient.callUrl(testSetupContext);
        }
    }

    void takeScreenshots(final List<ScreenshotContext> screenshotContextList) throws Exception {
        Map<ScreenshotContext, Future<?>> screenshotResults = new HashMap<>();
        for (final ScreenshotContext screenshotContext : screenshotContextList) {
            final Future<?> takeScreenshotsResult = threadPool.submit(() -> {
                //This activates the sifting appender in logback.xml to have a log in the report dir.
                MDC.put(REPORT_LOG_NAME_KEY, screenshotContext.fullPathOfReportDir + "/" + LOGFILE_NAME);
                try {
                    tryToTakeScreenshotsForContextNTimes(screenshotContext, jobConfig.screenshotRetries);
                } catch (Exception e) {
                    //There was an error, prevent pool from taking more tasks and let run fail
                    LOG.error("Exception in Browser thread while working on '" + screenshotContext.url + "' with device config " + screenshotContext.deviceConfig + ".", e);
                    synchronized(webDrivers) {
                        threadPool.shutdownNow();
                    }
                    throw new WebDriverException("Exception in Browser thread", e);
                } finally {
                    MDC.remove(REPORT_LOG_NAME_KEY);
                }
            });
            screenshotResults.put(screenshotContext, takeScreenshotsResult);
            //submit screenshots to the browser with a slight delay, so not all instances open up in complete sync
            Thread.sleep(THREAD_POOL_SUBMIT_SHUFFLE_TIME_IN_MS);
        }
        LOG.debug("All tasks have been sent to browser thread pool. Queuing shutdown.");
        threadPool.shutdown();
        LOG.debug("Browser thread pool shutdown queued. Doing work and awaiting termination. The global timeout is set to {} seconds.", jobConfig.globalTimeout);
        boolean ranIntoTimeout = !threadPool.awaitTermination(jobConfig.globalTimeout, TimeUnit.SECONDS);

        if (ranIntoTimeout) {
            LOG.error("Browser thread pool ran into timeout.");
            throw new TimeoutException("Global timeout of " + jobConfig.globalTimeout + " seconds was reached. Set or increase global \"timeout\" variable in config to change default.");
        } else {
            LOG.debug("Browser thread pool terminated successfully.");
        }

        //Get and propagate possible exceptions
        for (Map.Entry<ScreenshotContext, Future<?>> screenshotResult : screenshotResults.entrySet()) {
            try {
                screenshotResult.getValue().get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.error("Timeout while getting screenshot result for {} with device {}.", screenshotResult.getKey().url, screenshotResult.getKey().deviceConfig);
                throw e;
            }
        }
    }

    private void tryToTakeScreenshotsForContextNTimes(ScreenshotContext screenshotContext, int maxRetries) throws Exception {
        int retries = 0;
        while (retries <= maxRetries) {
            try {
                takeScreenshotsForContext(screenshotContext);
                return;
            } catch (Exception e) {
                if (retries < maxRetries) {
                    LOG.warn("try '{}' to take screen failed", retries, e);
                } else {
                    throw e;
                }
            }
            retries++;
        }
    }

    private final AtomicBoolean printVersion = new AtomicBoolean(true);

    private void takeScreenshotsForContext(final ScreenshotContext screenshotContext) throws Exception {

        if (screenshotContext.urlConfig.httpCheck.isEnabled() || jobConfig.httpCheck.isEnabled()) {
            JLineupHttpClient jLineupHttpClient = new JLineupHttpClient();
            jLineupHttpClient.checkPageAccessibility(screenshotContext, jobConfig);
        }

        boolean headlessRealBrowserOrMobileEmulation = jobConfig.browser.isHeadlessRealBrowser() || screenshotContext.dontShareBrowser;
        final WebDriver localDriver;
        if (headlessRealBrowserOrMobileEmulation) {
            localDriver = initializeWebDriver(screenshotContext.deviceConfig);
        } else {
            localDriver = initializeWebDriver();
        }

        if (printVersion.getAndSet(false)) {
            LOG.info("User agent: " + getUserAgent());
            fileService.setBrowserAndVersion(screenshotContext, getBrowserAndVersion(localDriver));
        }

        //No need to move the mouse out of the way for headless browsers, but this avoids hovering links in other browsers
        if (!jobConfig.browser.isHeadless() &&
                !ImageInfo.inImageCode() //we need to check if we're in native-image, because AWT does not work there, so no mouse movement
        ) {
            moveMouseToZeroZero();
        }

        if (!headlessRealBrowserOrMobileEmulation) {
            localDriver.manage().window().setPosition(new Point(0, 0));
            resizeBrowser(localDriver, screenshotContext.deviceConfig.width, screenshotContext.deviceConfig.height);
        }

        final String url = buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping);
        final String rootUrl = buildUrl(screenshotContext.url, "", screenshotContext.urlConfig.envMapping);

        if (areThereCookies(screenshotContext)) {
            //set cookies and local storage
            setCookies(screenshotContext, localDriver);
        }

        if (isThereStorage(screenshotContext)) {
            //get root page from url to be able to set cookies afterwards
            //if you set cookies before getting the page once, it will fail
            if (!localDriver.getCurrentUrl().equals(rootUrl)) {
                LOG.info(String.format("Getting root url: %s to set local and session storage", rootUrl));
                localDriver.get(rootUrl);
                logErrorChecker.checkForErrors(localDriver, jobConfig);
            }
            setLocalStorage(screenshotContext);
            setSessionStorage(screenshotContext);
        }

        browserCacheWarmup(screenshotContext, url, localDriver);

        LOG.info(String.format("Browsing to %s with window size %dx%d", url, screenshotContext.deviceConfig.width, screenshotContext.deviceConfig.height));
        //now get the real page
        //Selenium's get() method blocks until the browser/page fires an onload event (files and images referenced in the html have been loaded,
        //but there might be JS calls that load more stuff dynamically afterwards).
        localDriver.get(url);

        waitForSelectors(screenshotContext.urlConfig.waitForSelectors,
                screenshotContext.urlConfig.waitForSelectorsTimeout,
                screenshotContext.urlConfig.failIfSelectorsNotFound);

        logErrorChecker.checkForErrors(localDriver, jobConfig);

        Long pageHeight = getPageHeight();
        final Long viewportHeight = getViewportHeight();

        if (screenshotContext.urlConfig.waitAfterPageLoad > 0) {
            try {
                LOG.debug(String.format("Waiting for %f seconds (wait-after-page-load)", screenshotContext.urlConfig.waitAfterPageLoad));
                Thread.sleep(Math.round(screenshotContext.urlConfig.waitAfterPageLoad * 1000));
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (jobConfig.globalWaitAfterPageLoad > 0) {
            LOG.debug(String.format("Waiting for %f seconds (global wait-after-page-load)", jobConfig.globalWaitAfterPageLoad));
            Thread.sleep(Math.round(jobConfig.globalWaitAfterPageLoad * 1000));
        }

        LOG.debug("Page height before scrolling: {}", pageHeight);
        LOG.debug("Viewport height of browser window: {}", viewportHeight);

        scrollToTop();

        //Execute custom javascript if existing
        executeJavaScript(screenshotContext.urlConfig.javaScript);

        if (screenshotContext.urlConfig.hideImages) {
            executeJavaScript(JS_HIDE_IMAGES);
        }

        removeNodes(screenshotContext);

        //Wait for fonts //TODO: Do we really need this any more?
        if (screenshotContext.urlConfig.waitForFontsTime > 0) {
            if (!jobConfig.browser.isPhantomJS()) {
                WebDriverWait wait = new WebDriverWait(getWebDriver(), new Double(Math.ceil(screenshotContext.urlConfig.waitForFontsTime)).longValue());
                wait.until(fontsLoaded);
            } else {
                LOG.warn("WARNING: 'wait-for-fonts-time' is ignored because PhantomJS doesn't support this feature.");
            }
        }

        //
        // Start with screenshots
        //

        for (int yPosition = 0; yPosition < pageHeight && yPosition <= screenshotContext.urlConfig.maxScrollHeight; yPosition += viewportHeight) {
            LOG.debug("Scrolling info: yPosition: {}, pageHeight: {}, maxScrollHeight: {}, viewPortHeight: {}", yPosition, pageHeight, screenshotContext.urlConfig.maxScrollHeight, viewportHeight);
            BufferedImage currentScreenshot = takeScreenshot();
            currentScreenshot = waitForNoAnimation(screenshotContext, currentScreenshot);
            fileService.writeScreenshot(screenshotContext,
                    currentScreenshot, yPosition);
            //PhantomJS (until now) always makes full page screenshots, so no scrolling and multi-screenshooting
            //This is subject to change because W3C standard wants viewport screenshots
            if (jobConfig.browser.isPhantomJS()) {
                break;
            }
            LOG.debug("topOfViewport: {}, pageHeight: {}", yPosition, pageHeight);
            scrollTo(yPosition + viewportHeight.intValue());
            LOG.debug("Scroll by {} done", viewportHeight.intValue());

            if (screenshotContext.urlConfig.waitAfterScroll > 0) {
                LOG.debug("Waiting for {} seconds (wait after scroll).", screenshotContext.urlConfig.waitAfterScroll);
                TimeUnit.MILLISECONDS.sleep(Math.round(screenshotContext.urlConfig.waitAfterScroll * 1000));
            }

            //Refresh to check if page grows during scrolling
            pageHeight = getPageHeight();
            LOG.debug("Page height is {}", pageHeight);
        }

        if (LOG.isDebugEnabled()) {
            try {
                LogEntries logEntries = localDriver.manage().logs().get(LogType.BROWSER);
                for (LogEntry logEntry : logEntries) {
                    LOG.debug("Browser console: {}", logEntry.toString());
                }
            } catch (Exception e) {
                LOG.debug("No browser console available.");
            }
        }

        //TODO what about this feature?
        //String dom = getDom();
        //fileService.writeHtml(dom, screenshotContext.step);
        fileService.writeFileTrackerData();
    }

    private String getBrowserAndVersion(WebDriver driver) {
        Capabilities capabilities = ((HasCapabilities) driver).getCapabilities();
        String browserName = capabilities.getBrowserName();
        browserName = browserName.substring(0, 1).toUpperCase() + browserName.substring(1);
        String browserVersion = capabilities.getVersion();
        return browserName + " " + browserVersion;
    }

    private void resizeBrowser(WebDriver driver, int width, int height) {
        LOG.debug("Resize browser window to {}x{}", width, height);
        driver.manage().window().setSize(new Dimension(width, height));
    }

    private boolean areThereCookies(ScreenshotContext screenshotContext) {
        return (screenshotContext.urlConfig.cookies != null && !screenshotContext.urlConfig.cookies.isEmpty());
    }

    private boolean isThereStorage(ScreenshotContext screenshotContext) {
        return ((screenshotContext.urlConfig.localStorage != null && screenshotContext.urlConfig.localStorage.size() > 0)
                || (screenshotContext.urlConfig.sessionStorage != null && screenshotContext.urlConfig.sessionStorage.size() > 0));
    }

    private void setCookies(ScreenshotContext screenshotContext, WebDriver localDriver) {
        //First: Set cookies on different domains which are explicitly set
        Map<String, List<Cookie>> cookiesByDomainDifferentFromDomainToScreenshot = screenshotContext.urlConfig.cookies
                .stream()
                .filter(cookie -> cookie.domain != null)
                .collect(groupingBy(cookie -> cookie.domain));

        cookiesByDomainDifferentFromDomainToScreenshot.forEach((domain, cookies) -> {
            setCookiesForDomain(localDriver, domain, cookies);
        });

        //Second: Set my cookies on same domain
        List<Cookie> cookiesForSameDomain = screenshotContext.urlConfig.cookies.stream().filter(cookie -> cookie.domain == null).collect(Collectors.toList());
        setCookiesForDomain(localDriver, screenshotContext.url, cookiesForSameDomain);
    }

    private void setCookiesForDomain(WebDriver driver, String domain, List<Cookie> cookies) {

        if (cookies == null || cookies.isEmpty()) {
            LOG.debug("There are no cookies for domain {}", domain);
            return;
        }

        boolean secure = cookies.stream().anyMatch(cookie -> cookie.secure);
        String urlToSetCookie = domain;
        if (!urlToSetCookie.startsWith("http")) {
            if (urlToSetCookie.startsWith(".")) {
                urlToSetCookie = urlToSetCookie.substring(1);
            }
            urlToSetCookie = (secure ? "https://" : "http://") + urlToSetCookie;
        }
        LOG.debug("Going to {} to set cookies afterwards.", urlToSetCookie);
        driver.get(urlToSetCookie);
        LOG.debug("Opened {}.", urlToSetCookie);
        logErrorChecker.checkForErrors(driver, jobConfig);

        //Set cookies
        if (jobConfig.browser.isPhantomJS()) {
            //current phantomjs driver has a bug that prevents selenium's normal way of setting cookies
            LOG.debug("Setting cookies for PhantomJS on {}", urlToSetCookie);
            setCookiesPhantomJS(cookies);
        } else {
            LOG.debug("Setting cookies on {}", urlToSetCookie);
            setCookies(cookies);
            LOG.debug("Finished setting cookies on {}", urlToSetCookie);
        }
    }

    private void browserCacheWarmup(ScreenshotContext screenshotContext, String url, WebDriver driver) throws Exception {
        float warmupTime = screenshotContext.urlConfig.warmupBrowserCacheTime;
        if (warmupTime > JobConfig.DEFAULT_WARMUP_BROWSER_CACHE_TIME) {
            LOG.info(String.format("Browsing to %s with device config %s for cache warmup", url, screenshotContext.deviceConfig.toString()));
            LOG.debug("Getting url: {}", url);
            driver.get(url);
            logErrorChecker.checkForErrors(driver, jobConfig);
            LOG.debug(String.format("First call of %s - waiting %f seconds for cache warmup", url, warmupTime));
            LOG.debug("Sleeping for {} seconds", warmupTime);
            Thread.sleep(Math.round(warmupTime * 1000));
            LOG.debug("Cache warmup time is over. Getting " + url + " again.");
        }
    }

    private BufferedImage takeScreenshot() throws IOException {
        LOG.debug("Taking screenshot.");
        File screenshot = ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.FILE);
        return ImageIO.read(screenshot);
    }

    private BufferedImage waitForNoAnimation(ScreenshotContext screenshotContext, BufferedImage currentScreenshot) throws IOException {
        File screenshot;
        float waitForNoAnimation = screenshotContext.urlConfig.waitForNoAnimationAfterScroll;
        if (waitForNoAnimation > 0f) {
            LOG.debug("Waiting for no animation.");
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
        LOG.debug("Getting page height.");
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        return (Long) (jse.executeScript(JS_DOCUMENT_HEIGHT_CALL));
    }

    private Long getViewportHeight() {
        LOG.debug("Getting viewport height.");
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        return (Long) (jse.executeScript(JS_CLIENT_VIEWPORT_HEIGHT_CALL));
    }

    private String getDom() {
        LOG.debug("Getting DOM.");
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        return (String) (jse.executeScript(JS_GET_DOM));
    }

    private void executeJavaScript(String javaScript) throws InterruptedException {
        if (javaScript == null || "".equals(javaScript)) {
            return;
        }

        if (javaScript.contains(JLINEUP_SLEEP_JS_OPENER)) {
            executeJavaScriptWithJlineupAdditions(javaScript);
            return;
        }

        LOG.debug("Executing JavaScript: {}", javaScript);
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        jse.executeScript(javaScript);
        Thread.sleep(50);
    }

    private void executeJavaScriptWithJlineupAdditions(String javaScript) throws InterruptedException {
        LOG.debug("Executing JavaScript with JLineup additions: {}", javaScript);
        String[] parts = javaScript.split(JLINEUP_SLEEP_JS_OPENER + "\\(");

        //Execute first javascript block
        executeJavaScript(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            String secondsFollowedByClosingBracketAndSemicolonAndMoreJavaScript = parts[i];
            String[] secondsAndRemainingJS = secondsFollowedByClosingBracketAndSemicolonAndMoreJavaScript.split("\\);", 2);
            int milliseconds = Integer.parseInt(secondsAndRemainingJS[0]);
            LOG.debug("Sleeping for {} milliseconds", milliseconds);
            executeJavaScript("/* sleeping " + milliseconds + " milliseconds */");
            Thread.sleep(milliseconds);
            if (secondsAndRemainingJS.length > 1) {
                String js = secondsAndRemainingJS[1];
                executeJavaScript(js);
            }
        }
    }

    private String getUserAgent() {
        LOG.debug("Getting browser user agent.");
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        return (String) jse.executeScript(JS_GET_USER_AGENT);
    }

    void scrollTo(int yPosition) throws InterruptedException {
        LOG.debug("Scroll to {}", yPosition);
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        jse.executeScript(String.format(JS_SCROLL_TO_CALL, yPosition));
        //Sleep some milliseconds to give scrolling time before the next screenshot happens
        Thread.sleep(DEFAULT_SLEEP_AFTER_SCROLL_MILLIS);
    }

    private void scrollToTop() throws InterruptedException {
        LOG.debug("Scroll to top");
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

    private void removeNodes(ScreenshotContext screenshotContext) {
        removeNodes(screenshotContext.urlConfig.removeSelectors);
    }

    void setLocalStorage(Map<String, String> localStorage) {
        if (localStorage == null) return;

        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        for (Map.Entry<String, String> localStorageEntry : localStorage.entrySet()) {

            final String entry = localStorageEntry.getValue() != null ? localStorageEntry.getValue().replace("'", "\"") : null;

            String jsCall = String.format(JS_SET_LOCAL_STORAGE_CALL, localStorageEntry.getKey(), entry);
            jse.executeScript(jsCall);
            LOG.debug("LocalStorage call: {}", jsCall);
        }
    }

    void removeNodes(Set<String> cssSelectors) {
        if (cssSelectors == null || cssSelectors.isEmpty()) return;
        LOG.debug("Removing nodes with CSS selectors.");
        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        for (String cssSelector : cssSelectors) {
            String jsCall = String.format(JS_REMOVE_FROM_DOM_CALL, cssSelector);
            jse.executeScript(jsCall);
            LOG.debug("Remove from DOM call: {}", jsCall);
        }
    }

    void waitForSelectors(Set<String> cssSelectors, float timeout, boolean failIfNotFound) throws InterruptedException {
        if (cssSelectors == null || cssSelectors.isEmpty()) return;

        JavascriptExecutor jse = (JavascriptExecutor) getWebDriver();
        int retries = Double.valueOf(Math.ceil(timeout)).intValue();
        for (String cssSelector : cssSelectors) {
            boolean found = false;
            String jsCall = String.format(JS_CHECK_FOR_ELEMENT_CALL, cssSelector);
            while (!found && retries > 0) {
                found = (Boolean) (jse.executeScript(jsCall));
                LOG.debug("Wait for CSS selector call: {}, retries left: {}", jsCall, retries);
                Thread.sleep(1000);
                retries--;
            }
            LOG.info("{} '{}' with {} retries left.", found ? "Found" : "Didn't find", cssSelector, retries);
            if (!found && failIfNotFound) {
                throw new RuntimeException("Didn't find element with selector '" + cssSelector + "'.");
            }
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

            org.openqa.selenium.Cookie seleniumCookie = cookieBuilder.build();
            LOG.debug("Setting cookie through webdriver : {}", seleniumCookie);
            getWebDriver().manage().addCookie(seleniumCookie);
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

    WebDriver initializeWebDriver(DeviceConfig device) {
        if (shutdownCalled.get()) return null;
        synchronized (webDrivers) {
            String currentThreadName = Thread.currentThread().getName();
            if (webDrivers.containsKey(currentThreadName)) {
                WebDriver oldDriver = webDrivers.get(currentThreadName);
                LOG.debug("Removing webdriver for thread {} ({})", currentThreadName, oldDriver.getClass().getCanonicalName());
                try {
                    oldDriver.close();
                } catch (Exception e) {
                    LOG.debug("Exception while closing webdriver: " + e.getMessage(), e);
                }
                try {
                    oldDriver.quit();
                } catch (Exception e) {
                    LOG.debug("Exception while quitting webdriver: " + e.getMessage(), e);
                }
            }
            WebDriver driver = createDriverWithEmulatedDevice(device);
            webDrivers.put(currentThreadName, driver);
            return driver;
        }
    }

    WebDriver initializeWebDriver() {
        if (shutdownCalled.get()) return null;
        synchronized (webDrivers) {
            if (shutdownCalled.get()) return null;
            String currentThreadName = Thread.currentThread().getName();

            WebDriver driver;
            if (webDrivers.containsKey(currentThreadName)) {
                driver = webDrivers.get(currentThreadName);
            } else {
                driver = createDriver();
                webDrivers.put(currentThreadName, driver);
            }
            return driver;
        }
    }

    private WebDriver createDriver() {
        shutdownCalled.get();
        final WebDriver driver = browserUtils.getWebDriverByConfig(jobConfig, runStepConfig);
        driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        LOG.debug("Adding webdriver for thread {} ({})", Thread.currentThread().getName(), driver.getClass().getCanonicalName());
        return driver;
    }

    private WebDriver createDriverWithEmulatedDevice(DeviceConfig device) {
        final WebDriver driver = browserUtils.getWebDriverByConfig(jobConfig, runStepConfig, device);
        driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        LOG.debug("Adding webdriver for thread {} with emulated device {} ({})", Thread.currentThread().getName(), device, driver.getClass().getCanonicalName());
        return driver;
    }

    private WebDriver getWebDriver() {
        synchronized (webDrivers) {
            return webDrivers.get(Thread.currentThread().getName());
        }
    }

    private void moveMouseToZeroZero() {
        Robot robot;
        try {
            robot = new Robot();
            robot.mouseMove(0, 0);
        } catch (AWTException e) {
            LOG.error("Can't move mouse to 0,0", e);
        }
    }

    // wait for fonts to load
    private ExpectedCondition<Boolean> fontsLoaded = driver -> {
        final JavascriptExecutor javascriptExecutor = (JavascriptExecutor) getWebDriver();
        final Long fontsLoadedCount = (Long) javascriptExecutor.executeScript(JS_RETURN_DOCUMENT_FONTS_SIZE_CALL);
        final Boolean fontsLoaded = (Boolean) javascriptExecutor.executeScript(JS_RETURN_DOCUMENT_FONTS_STATUS_LOADED_CALL);
        LOG.debug("Amount of fonts in document: {}", fontsLoadedCount);
        LOG.debug("Fonts loaded: {} ", fontsLoaded);
        return fontsLoaded;
    };

    void grepChromedrivers() throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        Process process = pb.command("/bin/sh", "-c", "ps -eaf | grep chromedriver").start();
        new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(System.err::println);
    }
}
