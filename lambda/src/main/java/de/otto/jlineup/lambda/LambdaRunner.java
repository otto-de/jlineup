package de.otto.jlineup.lambda;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.JobConfigValidator;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static de.otto.jlineup.browser.BrowserUtils.getFullPathOfReportDir;
import static java.lang.invoke.MethodHandles.lookup;

public class LambdaRunner {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static final String REPORT_LOG_NAME_KEY = "reportlogname";
    public static final String LOGFILE_NAME = "jlineup.log";

    private final JobConfig jobConfig;

    public RunStepConfig getRunStepConfig() {
        return runStepConfig;
    }

    private final RunStepConfig runStepConfig;
    private final ScreenshotContext screenshotContext;

    private final String id;

    public LambdaRunner(String id, JobConfig jobConfig, RunStepConfig runStepConfig, ScreenshotContext screenshotContext) throws ValidationError {
        this.id = id;
        this.jobConfig = jobConfig;
        this.runStepConfig = runStepConfig;
        this.screenshotContext = screenshotContext;
        validateConfig();
    }

    public int run() {
        final FileService fileService = new FileService(runStepConfig, jobConfig, "files_" + runStepConfig.getStep() + "_" + screenshotContext.contextHash() + ".json");
        String debugLogDir = "/tmp/jlineup/debug-" + this.id;
        try {
            fileService.createWorkingDirectoryIfNotExists();
            fileService.createOrClearReportDirectory(runStepConfig.isKeepExisting());
            fileService.createOrClearScreenshotsDirectory(runStepConfig.isKeepExisting());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("jlineup.debug.logdir", debugLogDir);
        MDC.put(REPORT_LOG_NAME_KEY, getFullPathOfReportDir(runStepConfig) + "/" + LOGFILE_NAME);
        LOG.info("JLineup run started for context '{}'", screenshotContext);
        BrowserUtils browserUtils = new BrowserUtils();
        int retries = 0;
        try (Browser browser = new Browser(runStepConfig, jobConfig, fileService, browserUtils)) {
            retries = tryToTakeScreenshotsForContextNTimes(browser, screenshotContext, jobConfig.screenshotRetries);
            if (retries > 0) {
                LOG.warn("It took '{}' retries to take screenshots", retries);
            }
        } catch (Exception exception) {
            logBrowserDebugOutput(debugLogDir);
            throw new RuntimeException(exception);
        }

        LOG.info("JLineup run finished for context '{}'", screenshotContext);
        MDC.remove(REPORT_LOG_NAME_KEY);
        return retries;
    }

    private int tryToTakeScreenshotsForContextNTimes(Browser browser, ScreenshotContext screenshotContext, int maxRetries) throws Exception {
        int retries = 0;
        while (retries <= maxRetries) {
            try {
                browser.runForScreenshotContext(screenshotContext);
                return retries;
            } catch (Exception e) {
                if (retries < maxRetries) {
                    LOG.warn("try '{}' to take screen failed", retries, e);
                } else {
                    LOG.error("'{}' retries did not help, giving up. Last exception was: '{}'", retries, e.getMessage());
                    throw e;
                }
            }
            retries++;
        }
        return Integer.MAX_VALUE;
    }

    private void validateConfig() throws ValidationError {
        JobConfigValidator.validateJobConfig(jobConfig);
    }

    private void logBrowserDebugOutput(String debugLogDir) {
        Browser.Type browserType = screenshotContext.getBrowserType();
        // Determine which log files to look for based on browser type
        List<Path> logFiles = new ArrayList<>();
        if (browserType == null || browserType.isChrome()) {
            // Legacy path for Chrome debug log
            logFiles.add(Paths.get("/tmp", "jlineup", "chrome-profile-" + this.id, "chrome_debug.log"));
            logFiles.add(Paths.get(debugLogDir, "chrome_debug.log"));
        } else if (browserType.isFirefox()) {
            logFiles.add(Paths.get(debugLogDir, "firefox_debug.log"));
        } else if (browserType.isWebkit()) {
            logFiles.add(Paths.get(debugLogDir, "webkit_debug.log"));
        }

        for (Path logFile : logFiles) {
            if (Files.exists(logFile)) {
                LOG.info("--- Debug log from {} ---", logFile.getFileName());
                try (Stream<String> lines = Files.lines(logFile)) {
                    lines.forEach(LOG::info);
                } catch (IOException ioException) {
                    LOG.error("Could not read '{}'", logFile.toAbsolutePath(), ioException);
                }
                LOG.info("--- End of {} ---", logFile.getFileName());
            }
        }
    }
}
