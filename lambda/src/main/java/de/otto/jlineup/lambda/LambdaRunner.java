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

import static de.otto.jlineup.browser.BrowserUtils.getFullPathOfReportDir;
import static java.lang.invoke.MethodHandles.lookup;

public class LambdaRunner {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static final String REPORT_LOG_NAME_KEY = "reportlogname";
    public static final String LOGFILE_NAME = "jlineup.log";

    private final JobConfig jobConfig;
    private final RunStepConfig runStepConfig;
    private final ScreenshotContext screenshotContext;

    public LambdaRunner(JobConfig jobConfig, RunStepConfig runStepConfig, ScreenshotContext screenshotContext) throws ValidationError {
        this.jobConfig = jobConfig;
        this.runStepConfig = runStepConfig;
        this.screenshotContext = screenshotContext;
        validateConfig();
    }

    public boolean run() {
        final FileService fileService = new FileService(runStepConfig, jobConfig);
        try {
            fileService.createWorkingDirectoryIfNotExists();
            fileService.createOrClearReportDirectory();
            fileService.createOrClearScreenshotsDirectory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MDC.put(REPORT_LOG_NAME_KEY, getFullPathOfReportDir(runStepConfig) + "/" + LOGFILE_NAME);
        LOG.info("JLineup run started for context '{}'", screenshotContext);

        BrowserUtils browserUtils = new BrowserUtils();
        try (Browser browser = new Browser(runStepConfig, jobConfig, fileService, browserUtils)) {
            browser.runForScreenshotContext(screenshotContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOG.info("JLineup run finished for context '{}'", screenshotContext);
        MDC.remove(REPORT_LOG_NAME_KEY);
        return true;
    }

    private void validateConfig() throws ValidationError {
        JobConfigValidator.validateJobConfig(jobConfig);
    }
}
