package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.*;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws Exception {

        final Parameters parameters = new Parameters();
        final JCommander jCommander = new JCommander(parameters);
        jCommander.parse(args);
        jCommander.setProgramName("JLineup");
        if (parameters.isHelp()) {
            jCommander.usage();
            System.out.printf("Version: %s%n", Util.getVersion());
            return;
        }

        if (parameters.isVersion()) {
            System.out.printf("JLineup version %s", Util.getVersion());
            return;
        }

        if (parameters.isDebug()) {
            Util.setLogLevelToDebug();
        }

        FileService fileService = new FileService(parameters);
        ImageService imageService = new ImageService();

        //Make sure the working dir exists
        if (parameters.isBefore()) {
            fileService.createWorkingDirectoryIfNotExists();
        }

        Config config;
        boolean exit = false;
        if (parameters.getUrl() != null) {
            String url = BrowserUtils.prependHTTPIfNotThereAndToLowerCase(parameters.getUrl());
            config = Config.defaultConfig(url);
            if (!parameters.isPrintConfig()) {
                System.out.printf("You specified an explicit URL parameter (%s), any given config file is ignored! This should only be done for testing purpose.%n", url);
                System.out.printf("Using generated config:%n%s%n", Util.createPrettyConfigJson(config));
                System.out.println("You can take this generated config as base and save it as a text file named 'lineup.json'.");
                System.out.println("Just add --print-config parameter to let JLineup print an example config");
            }
        } else {
            try {
                config = Config.readConfig(parameters);
            } catch (FileNotFoundException e) {
                if (!parameters.isPrintConfig()) {
                    System.err.println(e.getMessage());
                    System.err.println("Use --help to see the JLineup quick help.");
                }
                config = Config.exampleConfig();
                exit = true;
            }
        }

        if (parameters.isPrintConfig()) {
            System.out.println(Util.createPrettyConfigJson(config));
            System.exit(0);
        }

        if (exit) {
            System.exit(1);
        }

        if (config.debug) {
            Util.setLogLevelToDebug();
        }

        if (config.logToFile) {
            Util.logToFile(parameters);
        }

        //Only create screenshots and report dirs if config was found
        if (parameters.isBefore()) {
            fileService.createOrClearReportDirectory();
            fileService.createOrClearScreenshotsDirectory();
        }

        System.out.printf("Running JLineup [%s] with step '%s'.%n%n", Util.getVersion(), parameters.getStep());

        exit = false;
        if (!parameters.isJustCompare()) {
            BrowserUtils browserUtils = new BrowserUtils();
            try (Browser browser = new Browser(parameters, config, fileService, browserUtils)) {
                browser.takeScreenshots();
            } catch (Exception e) {
                System.err.println("JLineup Exception: " + e);
                exit = true;
            }
        }

        if (exit) {
            System.exit(1);
        }

        if (parameters.isAfter() || parameters.isJustCompare()) {
            ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(parameters, config, fileService, imageService);
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

        System.out.printf("JLineup run finished for step '%s'%n", parameters.getStep());
    }

}
