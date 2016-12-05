package de.otto.jlineup;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.HTMLReportGenerator;
import de.otto.jlineup.report.JSONReportGenerator;
import de.otto.jlineup.report.ScreenshotComparisonResult;
import de.otto.jlineup.report.ScreenshotsComparator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Parameters parameters = new Parameters();
        final JCommander jCommander = new JCommander(parameters, args);
        jCommander.setProgramName("JLineup");
        if (parameters.isHelp()) {
            jCommander.usage();
            System.out.printf("Version: %s%n", getVersion());
            return;
        }

        if (parameters.isVersion()) {
            System.out.printf("JLineup version %s", getVersion());
            return;
        }

        if (parameters.isDebug()) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
        }

        FileService fileService = new FileService(parameters);
        ImageService imageService = new ImageService();

        //Make sure the working dir exists
        if (parameters.isBefore()) {
            fileService.createWorkingDirectoryIfNotExists();
        }

        Config config;
        if (parameters.getUrl() != null) {
            String url = BrowserUtils.prependHTTPIfNotThereAndToLowerCase(parameters.getUrl());
            config = Config.defaultConfig(url);
            if (!parameters.isPrintConfig()) {
                System.out.printf("You specified an explicit URL parameter (%s), any given config file is ignored! This should only be done for testing purpose.%n", url);
                System.out.printf("Using generated config:%n%s%n", createPrettyConfigJson(config));
                System.out.println("You can take this generated config as base and save it as a text file named 'lineup.json'.");
                System.out.println("Just add --print-config parameter to let JLineup print this config");
            }
        } else {
            try {
                config = Config.readConfig(parameters);
            } catch (FileNotFoundException e) {
                if (!parameters.isPrintConfig()) {
                    System.err.println(e.getMessage());
                    System.err.println("Use --help to see the JLineup quick help.");
                }
                config = Config.defaultConfig();
            }
        }

        if (parameters.isPrintConfig()) {
            System.out.println(createPrettyConfigJson(config));
            System.exit(0);
        }

        if (Config.defaultConfig().equals(config)) {
            System.exit(1);
        }

        //Only create screenshots and report dirs if config was found
        if (parameters.isBefore()) {
            fileService.createOrClearScreenshotsDirectory();
            fileService.createOrClearReportDirectory();
        }

        System.out.println("Running JLineup with step '" + parameters.getStep() + "'.");

        /* Currently - firefox 49.0 is running fine
        if (config.browser == Browser.Type.FIREFOX) {
            System.out.println("You're running JLineup with Firefox - Firefox is currently not supported and the run may fail.");
        }
        */

        if (!parameters.isJustCompare()) {
            try (Browser browser = new Browser(parameters, config, BrowserUtils.getWebDriverByConfig(config), fileService)) {
                browser.takeScreenshots();
            }
        }

        if (parameters.isAfter() || parameters.isJustCompare()) {
            ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(parameters, config, fileService, imageService);
            List<ScreenshotComparisonResult> comparisonResults = screenshotsComparator.compare();

            final JSONReportGenerator jsonReportGenerator = new JSONReportGenerator(fileService);
            jsonReportGenerator.writeComparisonReportAsJson(comparisonResults);

            final HTMLReportGenerator htmlReportGenerator = new HTMLReportGenerator(fileService);
            htmlReportGenerator.writeReport(comparisonResults);

            System.out.println("Sum of screenshot differences:\n" + comparisonResults.stream().mapToDouble(scr -> scr.difference).sum());
        }
    }

    private static String getVersion() {
        return String.format("%s [%s]%n", Util.readVersion(), Util.readCommit());
    }

    private static String createPrettyConfigJson(Config config) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(config);
    }

}
