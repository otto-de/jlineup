package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.image.ImageService;
import de.otto.jlineup.report.ReportGenerator;
import de.otto.jlineup.report.ScreenshotComparisonResult;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.report.ScreenshotsComparator;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Parameters parameters = new Parameters();
        new JCommander(parameters, args);

        FileService fileService = new FileService(parameters);
        ImageService imageService = new ImageService();

        //Make sure the working dir exists
        if (parameters.isBefore()) {
            fileService.createWorkingDirectoryIfNotExists();
        }
        Config config = Config.readConfig(parameters);
        //Only create screenshots and report dirs if config was found
        if (parameters.isBefore()) {
            fileService.createOrClearScreenshotsDirectory();
            fileService.createOrClearReportDirectory();
        }

        if (!parameters.isJustCompare()) {
            try (Browser browser = new Browser(parameters, config, BrowserUtils.getWebDriverByConfig(config), fileService)) {
                browser.takeScreenshots();
            }
        }

        if (parameters.isAfter() || parameters.isJustCompare()) {
            ScreenshotsComparator screenshotsComparator = new ScreenshotsComparator(parameters, config, fileService, imageService);
            List<ScreenshotComparisonResult> comparisonResults = screenshotsComparator.compare();
            ReportGenerator reportGenerator = new ReportGenerator(fileService);
            reportGenerator.writeComparisonReportAsJson(comparisonResults);
        }
    }

}
