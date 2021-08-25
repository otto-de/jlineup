package de.otto.jlineup;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.JobConfigValidator;
import de.otto.jlineup.config.Step;
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
import java.util.Set;

import static de.otto.jlineup.browser.BrowserUtils.getFullPathOfReportDir;
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
        if (runStepConfig.getStep() == Step.before) {
            try {
                fileService.createWorkingDirectoryIfNotExists();
                fileService.createOrClearReportDirectory();
                fileService.createOrClearScreenshotsDirectory();
                htmlReportWriter.writeNotFinishedReport(runStepConfig, jobConfig);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        MDC.put(REPORT_LOG_NAME_KEY, getFullPathOfReportDir(runStepConfig) + "/" + LOGFILE_NAME);
        LOG.info("JLineup run started for step '{}'", runStepConfig.getStep());

        if (runStepConfig.getStep() == Step.before || runStepConfig.getStep() == Step.after) {
            BrowserUtils browserUtils = new BrowserUtils();
            try (Browser browser = new Browser(runStepConfig, jobConfig, fileService, browserUtils)) {
                browser.runSetupAndTakeScreenshots();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            if (runStepConfig.getStep() == Step.after || runStepConfig.getStep() == Step.compare) {
                ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, imageService);
                final Map<String, List<ScreenshotComparisonResult>> comparisonResults = screenshotsComparator.compare();

                final ReportGenerator reportGenerator = new ReportGenerator();
                final Report report = reportGenerator.generateReport(comparisonResults, jobConfig);

                JSONReportWriter jsonReportWriter;
                if (Utils.shouldUseLegacyReportFormat(jobConfig)) {
                    jsonReportWriter = new JSONReportWriter_V1(fileService);
                } else {
                    jsonReportWriter = new JSONReportWriter_V2(fileService);
                }
                jsonReportWriter.writeComparisonReportAsJson(report);
                htmlReportWriter.writeReport(report);

                final Set<Map.Entry<String, UrlReport>> urlReports = report.screenshotComparisonsForUrl.entrySet();
                for (Map.Entry<String, UrlReport> urlReport : urlReports) {
                    LOG.info("Sum of screenshot differences for {}: {} ({} %)", urlReport.getKey(), urlReport.getValue().summary.differenceSum, Math.round(urlReport.getValue().summary.differenceSum * 100d));
                    LOG.info("Max difference of a single screenshot for {}: {} ({} %)", urlReport.getKey(), urlReport.getValue().summary.differenceMax, Math.round(urlReport.getValue().summary.differenceMax * 100d));
                    LOG.info("Accepted different pixels for {}: {}", urlReport.getKey(), urlReport.getValue().summary.acceptedDifferentPixels);
                }

                LOG.info("Sum of overall screenshot differences: {} ({} %)", report.summary.differenceSum, Math.round(report.summary.differenceSum * 100d));
                LOG.info("Max difference of a single screenshot: {} ({} %)", report.summary.differenceMax, Math.round(report.summary.differenceMax * 100d));

                if (!Utils.shouldUseLegacyReportFormat(jobConfig)) {
                    //Exit with exit code 1 if at least one url report has a bigger difference than configured
                    if (isDetectedDifferenceGreaterThanMaxDifference(urlReports, jobConfig)) {
                        LOG.info("JLineup finished. There was a difference between before and after. Return code is 1.");
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("JLineup run finished for step '{}'", runStepConfig.getStep());
        MDC.remove(REPORT_LOG_NAME_KEY);
        return true;
    }

    static boolean isDetectedDifferenceGreaterThanMaxDifference(Set<Map.Entry<String, UrlReport>> urlReports, JobConfig jobConfig) {
        for (Map.Entry<String, UrlReport> urlReport : urlReports) {
            if (jobConfig.urls != null && urlReport.getValue().summary.differenceMax > jobConfig.urls.get(urlReport.getKey()).maxDiff) {
                return true;
            }
        }
        return false;
    }

    private void validateConfig() throws ValidationError {
        JobConfigValidator.validateJobConfig(jobConfig);
    }
}
