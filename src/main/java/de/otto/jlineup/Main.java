package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.report.ScreenshotComparisonResult;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.report.ScreenshotsComparator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Parameters parameters = new Parameters();
        new JCommander(parameters, args);

        FileService fileService = new FileService();

        //Make sure the working dir exists
        if (parameters.isBefore()) {
            fileService.createWorkingDirectoryIfNotExists(parameters);
        }
        Config config = Config.readConfig(parameters);
        //Only create screenshots and report dirs if config was found
        if (parameters.isBefore()) {
            fileService.createOrClearScreenshotsDirectory(parameters);
            fileService.createOrClearReportDirectory(parameters);
        }

        if (!parameters.isJustCompare()) {
            try (Browser browser = new Browser(parameters, config, BrowserUtils.getWebDriverByConfig(config), fileService)) {
                browser.takeScreenshots();
            }
        }

        if (parameters.isAfter() || parameters.isJustCompare()) {
            ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(parameters, config, fileService);
            List<ScreenshotComparisonResult> compare = screenshotsComparator.compare();
            writeComparisonReport(parameters, compare);
        }
    }

    private static void writeComparisonReport(Parameters parameters, List<ScreenshotComparisonResult> screenshotComparisonResults) throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String reportJson = gson.toJson(screenshotComparisonResults);
        try (PrintStream out = new PrintStream(new FileOutputStream(FileService.getReportDirectory(parameters).toString() + "/report.json"))) {
            out.print(reportJson);
        }
    }

}
