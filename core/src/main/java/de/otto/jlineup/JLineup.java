package de.otto.jlineup;

import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JLineup {

    private final Config config;
    private final JLineupOptions jLineupOptions;

    public JLineup(Config config, JLineupOptions jLineupOptions) {
        this.config = config;
        this.jLineupOptions = jLineupOptions;
    }

    public int run() throws IOException {
        FileService fileService = new FileService(jLineupOptions.getParameters());
        ImageService imageService = new ImageService();

        //Make sure the working dir exists
        if (jLineupOptions.getParameters().isBefore()) {
            fileService.createWorkingDirectoryIfNotExists();
        }


        if (config.debug) {
            Util.setLogLevelToDebug();
        }

        if (config.logToFile) {
            Util.logToFile(jLineupOptions.getParameters());
        }

        //Only create screenshots and report dirs if config was found
        if (jLineupOptions.getParameters().isBefore()) {
            fileService.createOrClearReportDirectory();
            fileService.createOrClearScreenshotsDirectory();
        }

        System.out.printf("Running JLineup [%s] with step '%s'.%n%n", Util.getVersion(), jLineupOptions.getParameters().getStep());

        if (!jLineupOptions.getParameters().isJustCompare()) {
            BrowserUtils browserUtils = new BrowserUtils();
            try (Browser browser = new Browser(jLineupOptions.getParameters(), config, fileService, browserUtils)) {
                browser.takeScreenshots();
            } catch (Exception e) {
                System.err.println("JLineup Exception: " + e);
                return 1;
            }
        }

        if (jLineupOptions.getParameters().isAfter() || jLineupOptions.getParameters().isJustCompare()) {
            ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(jLineupOptions.getParameters(), config, fileService, imageService);
            final Map<String, List<ScreenshotComparisonResult>> comparisonResults = screenshotsComparator.compare();

            final ReportGenerator reportGenerator = new ReportGenerator();
            final Report report = reportGenerator.generateReport(comparisonResults);

            JSONReportWriter jsonReportWriter;
            if (Util.shouldUseLegacyReportFormat(config)) {
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

            if (!Util.shouldUseLegacyReportFormat(config)) {
                for (Map.Entry<String, UrlReport> entry : entries) {
                    //Exit with exit code 1 if at least one url report has a bigger difference than configured
                    if (config.urls != null && entry.getValue().summary.differenceMax > config.urls.get(entry.getKey()).maxDiff) {
                        System.out.println("JLineup finished. There was a difference between before and after. Return code is 1.");
                        System.exit(1);
                    }
                }
            }
        }

        System.out.printf("JLineup run finished for step '%s'%n", jLineupOptions.getParameters().getStep());
        return 0;
    }
}
