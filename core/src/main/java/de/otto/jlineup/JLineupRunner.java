package de.otto.jlineup;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.exceptions.ConfigValidationException;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;

public class JLineupRunner {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final JobConfig jobConfig;
    private final RunStepConfig runStepConfig;

    public JLineupRunner(JobConfig jobConfig, RunStepConfig runStepConfig) throws ConfigValidationException {
        this.jobConfig = jobConfig;
        this.runStepConfig = runStepConfig;

        validateConfig();
    }

    public boolean run() {
        FileService fileService = new FileService(runStepConfig);
        ImageService imageService = new ImageService();

        //Make sure the working dir exists
        if (runStepConfig.getStep() == Step.before) {
            try {
                fileService.createWorkingDirectoryIfNotExists();
                fileService.createOrClearReportDirectory();
                fileService.createOrClearScreenshotsDirectory();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (runStepConfig.getStep() == Step.before || runStepConfig.getStep() == Step.after) {
            BrowserUtils browserUtils = new BrowserUtils();
            try (Browser browser = new Browser(runStepConfig, jobConfig, fileService, browserUtils)) {
                browser.takeScreenshots();
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

                final HTMLReportWriter htmlReportWriter = new HTMLReportWriter(fileService);
                htmlReportWriter.writeReport(report);

                final Set<Map.Entry<String, UrlReport>> entries = report.screenshotComparisonsForUrl.entrySet();
                for (Map.Entry<String, UrlReport> entry : entries) {
                    LOG.info("Sum of screenshot differences for " + entry.getKey() + ": " + entry.getValue().summary.differenceSum + " (" + Math.round(entry.getValue().summary.differenceSum * 100d) + " %)");
                    LOG.info("Max difference of a single screenshot for " + entry.getKey() + ": " + entry.getValue().summary.differenceMax + " (" + Math.round(entry.getValue().summary.differenceMax * 100d) + " %)");
                }

                LOG.info("Sum of overall screenshot differences: " + report.summary.differenceSum + " (" + Math.round(report.summary.differenceSum * 100d) + " %)");
                LOG.info("Max difference of a single screenshot: " + report.summary.differenceMax + " (" + Math.round(report.summary.differenceMax * 100d) + " %)");

                if (!Utils.shouldUseLegacyReportFormat(jobConfig)) {
                    for (Map.Entry<String, UrlReport> entry : entries) {
                        //Exit with exit code 1 if at least one url report has a bigger difference than configured
                        if (jobConfig.urls != null && entry.getValue().summary.differenceMax > jobConfig.urls.get(entry.getKey()).maxDiff) {
                            LOG.info("JLineup finished. There was a difference between before and after. Return code is 1.");
                            return false;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("JLineup run finished for step '{}'\n", runStepConfig.getStep());
        return true;
    }

    private void validateConfig() throws ConfigValidationException {
        //Check urls
        if (jobConfig.urls == null || jobConfig.urls.isEmpty()) {
            throw new ConfigValidationException("No URLs configured.");
        }

        //Check browser window height
        if (jobConfig.windowHeight != null && (jobConfig.windowHeight < 100 && jobConfig.windowHeight > 10000)) {
            throw new ConfigValidationException(String.format("Configured window height is invalid: %d. Valid values are between 100 and 10000", jobConfig.windowHeight));
        }

        for (Map.Entry<String, UrlConfig> urlConfigEntry : jobConfig.urls.entrySet()) {

            String url = urlConfigEntry.getKey();
            UrlConfig urlConfig = urlConfigEntry.getValue();

            //Check browser window widths
            for (Integer width : urlConfig.windowWidths) {
                if (width < 10 || width > 10000) {
                    throw new ConfigValidationException(String.format("Configured window width for %s is invalid: %d. Valid values are between 10 and 10000", url, width));
                }
            }

            //Check timeouts
            if (urlConfig.waitAfterPageLoad > 20 || urlConfig.waitAfterPageLoad < 0) {
                throw new ConfigValidationException(String.format("Configured wait after page load time of %d seconds for %s is invalid. Valid values are between 0 and 20.", urlConfig.waitAfterPageLoad, url));
            }
            if (urlConfig.waitAfterScroll > 20 || urlConfig.waitAfterScroll < 0) {
                throw new ConfigValidationException(String.format("Configured wait after scroll time of %d seconds for %s is invalid. Valid values are between 0 and 20.", urlConfig.waitAfterScroll, url));
            }
            if (urlConfig.waitForFontsTime > 20 || urlConfig.waitForFontsTime < 0) {
                throw new ConfigValidationException(String.format("Configured wait for fonts time of %d seconds for %s is invalid. Valid values are between 0 and 20.", urlConfig.waitForFontsTime, url));
            }

            //Check max scroll height
            if (urlConfig.maxScrollHeight <= 0) {
                throw new ConfigValidationException(String.format("Configured max scroll height (%d) for %s must be negative)", urlConfig.maxScrollHeight, url));
            }
        }
    }
}
