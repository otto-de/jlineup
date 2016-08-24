package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ComparisonResult;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileUtils;
import de.otto.jlineup.report.ComparisonReporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Parameters parameters = new Parameters();
        new JCommander(parameters, args);

        //Make sure the working dir exists
        if (parameters.isBefore()) {
            FileUtils.createWorkingDirectoryIfNotExists(parameters);
        }
        Config config = Config.readConfig(parameters);
        //Only create screenshots and report dirs if config was found
        if (parameters.isBefore()) {
            FileUtils.createOrClearScreenshotsDirectory(parameters);
            FileUtils.createOrClearReportDirectory(parameters);
        }

        if (!parameters.isJustCompare()) {
            try (Browser browser = new Browser(parameters, config, BrowserUtils.getWebDriverByConfig(config))) {
                browser.takeScreenshots();
            }
        }

        if (parameters.isAfter() || parameters.isJustCompare()) {
            ComparisonReporter comparisonReporter = new ComparisonReporter(parameters, config);
            List<ComparisonResult> compare = comparisonReporter.compare();
            writeComparisonReport(parameters, compare);
        }
    }

    private static void writeComparisonReport(Parameters parameters, List<ComparisonResult> comparisonResults) throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String reportJson = gson.toJson(comparisonResults);
        try (PrintStream out = new PrintStream(new FileOutputStream(FileUtils.getReportDirectory(parameters).toString() + "/report.json"))) {
            out.print(reportJson);
        }
    }

}
