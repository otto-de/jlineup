package de.otto.jlineup;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JLineupRunner {

    private static final Logger LOG = LoggerFactory.getLogger(JLineupRunner.class);

    private final JobConfig jobConfig;
    private final RunStepConfig runStepConfig;

    public JLineupRunner(JobConfig jobConfig, RunStepConfig runStepConfig) {
        this.jobConfig = jobConfig;
        this.runStepConfig = runStepConfig;
    }

    public boolean run() throws IOException {

        if (jobConfig.urls == null) {
            LOG.error("No urls are configured in the config.");
            return false;
        }

        FileService fileService = new FileService(runStepConfig);
        ImageService imageService = new ImageService();

        //Make sure the working dir exists
        if (runStepConfig.getStep() == Step.before) {
            fileService.createWorkingDirectoryIfNotExists();
            fileService.createOrClearReportDirectory();
            fileService.createOrClearScreenshotsDirectory();
        }

        if (runStepConfig.getStep() == Step.before || runStepConfig.getStep() == Step.after) {
            BrowserUtils browserUtils = new BrowserUtils();
            try (Browser browser = new Browser(runStepConfig, jobConfig, fileService, browserUtils)) {
                browser.takeScreenshots();
            } catch (Exception e) {
                System.err.println("JLineupRunner Exception: " + e);
                return false;
            }
        }

        if (runStepConfig.getStep() == Step.after || runStepConfig.getStep() == Step.compare) {
            ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(runStepConfig, jobConfig, fileService, imageService);
            final Map<String, List<ScreenshotComparisonResult>> comparisonResults = screenshotsComparator.compare();

            final ReportGenerator reportGenerator = new ReportGenerator();
            final Report report = reportGenerator.generateReport(comparisonResults);

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
                System.out.println("Sum of screenshot differences for " + entry.getKey() + ":\n" + entry.getValue().summary.differenceSum + " (" + Math.round(entry.getValue().summary.differenceSum * 100d) + " %)");
                System.out.println("Max difference of a single screenshot for " + entry.getKey() + ":\n" + entry.getValue().summary.differenceMax + " (" + Math.round(entry.getValue().summary.differenceMax * 100d) + " %)");
                System.out.println("");
            }

            System.out.println("Sum of overall screenshot differences:\n" + report.summary.differenceSum + " (" + Math.round(report.summary.differenceSum * 100d) + " %)");
            System.out.println("Max difference of a single screenshot:\n" + report.summary.differenceMax + " (" + Math.round(report.summary.differenceMax * 100d) + " %)");

            if (!Utils.shouldUseLegacyReportFormat(jobConfig)) {
                for (Map.Entry<String, UrlReport> entry : entries) {
                    //Exit with exit code 1 if at least one url report has a bigger difference than configured
                    if (jobConfig.urls != null && entry.getValue().summary.differenceMax > jobConfig.urls.get(entry.getKey()).maxDiff) {
                        System.out.println("JLineupRunner finished. There was a difference between before and after. Return code is 1.");
                        System.exit(1);
                    }
                }
            }
        }

        System.out.printf("JLineupRunner run finished for step '%s'%n", runStepConfig.getStep());
        return true;
    }
}
