package de.otto.jlineup;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.JobConfigValidator;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.exceptions.ValidationError;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static de.otto.jlineup.browser.BrowserUtils.getFullPathToLogFile;
import static java.lang.invoke.MethodHandles.lookup;

public class JLineupRunner {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static final String REPORT_LOG_NAME_KEY = "reportlogname";
    public static final String LOGFILE_NAME = "jlineup.log";

    private final JobConfig jobConfig;
    private final RunStepConfig runStepConfig;

    public JLineupRunner(JobConfig jobConfig, RunStepConfig runStepConfig) throws ValidationError {
        this.jobConfig = jobConfig;
        this.runStepConfig = runStepConfig;
        validateConfig();
    }

    public boolean run() {

        final FileService fileService = new FileService(runStepConfig, jobConfig);
        final ImageService imageService = new ImageService();
        final HTMLReportWriter htmlReportWriter = new HTMLReportWriter(fileService);

        //Make sure the working dir exists
        if (runStepConfig.getStep() == RunStep.before || runStepConfig.getStep() == RunStep.after_only) {
            try {
                fileService.createWorkingDirectoryIfNotExists();
                fileService.createOrClearReportDirectory(runStepConfig.isKeepExisting());
                fileService.createOrClearScreenshotsDirectory(runStepConfig.isKeepExisting());
                htmlReportWriter.writeNotFinishedReport(runStepConfig, jobConfig);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        MDC.put(REPORT_LOG_NAME_KEY, getFullPathToLogFile(runStepConfig));
        LOG.info("JLineup run started for step '{}'", runStepConfig.getStep());

        if (runStepConfig.getStep() == RunStep.before || runStepConfig.getStep() == RunStep.after|| runStepConfig.getStep() == RunStep.after_only) {
            BrowserUtils browserUtils = new BrowserUtils();
            try (Browser browser = new Browser(runStepConfig, jobConfig, fileService, browserUtils)) {
                browser.runSetupAndTakeScreenshots();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (runStepConfig.getStep() == RunStep.before) {
                ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, imageService);
                try {
                    Map<String, List<ScreenshotComparisonResult>> onlyBeforeScreenshotsInThisComparisonResult = screenshotsComparator.compare();
                    final Report reportAfterBeforeStep = new ReportGenerator(fileService).generateReport(onlyBeforeScreenshotsInThisComparisonResult, jobConfig);
                    htmlReportWriter.writeReportAfterBeforeStep(reportAfterBeforeStep);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            if (runStepConfig.getStep() == RunStep.after || runStepConfig.getStep() == RunStep.compare) {
                ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, imageService);
                final Map<String, List<ScreenshotComparisonResult>> comparisonResults = screenshotsComparator.compare();

                final ReportGenerator reportGenerator = new ReportGenerator(fileService);
                final Report report = reportGenerator.generateReport(comparisonResults, jobConfig);

                new JSONReportWriter(fileService).writeComparisonReportAsJson(report);
                htmlReportWriter.writeReport(report);

                for (UrlReport urlReport : report.urlReports()) {
                    LOG.info("Sum of screenshot differences for {}: {} ({} %)", urlReport.urlKey(), urlReport.summary().differenceSum(), Math.round(urlReport.summary().differenceSum() * 100d));
                    LOG.info("Max difference of a single screenshot for {}: {} ({} %)", urlReport.urlKey(), urlReport.summary().differenceMax(), Math.round(urlReport.summary().differenceMax() * 100d));
                    LOG.info("Accepted different pixels for {}: {}", urlReport.urlKey(), urlReport.summary().acceptedDifferentPixels());
                }

                LOG.info("Sum of overall screenshot differences: {} ({} %)", report.summary().differenceSum(), Math.round(report.summary().differenceSum() * 100d));
                LOG.info("Max difference of a single screenshot: {} ({} %)", report.summary().differenceMax(), Math.round(report.summary().differenceMax() * 100d));

                //Exit with exit code 1 if at least one url report has a bigger difference than configured
                if (isDetectedDifferenceGreaterThanMaxDifference(report, jobConfig)) {
                    LOG.info("JLineup finished. There was a difference between before and after. Return code is 1.");
                    return false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("JLineup run finished for step '{}'", runStepConfig.getStep());
        MDC.remove(REPORT_LOG_NAME_KEY);
        return true;
    }

    static boolean isDetectedDifferenceGreaterThanMaxDifference(Report report, JobConfig jobConfig) {
        for (UrlReport urlReport : report.urlReports()) {
            if (jobConfig.urls != null && urlReport.summary().differenceMax() > jobConfig.urls.get(urlReport.urlKey()).maxDiff) {
                return true;
            }
        }
        return false;
    }

    private void validateConfig() throws ValidationError {
        JobConfigValidator.validateJobConfig(jobConfig);
    }
}
