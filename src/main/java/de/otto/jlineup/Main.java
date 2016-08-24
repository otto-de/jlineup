package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.files.FileUtils;

import java.io.IOException;

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

        try (Browser browser = new Browser(parameters, config, BrowserUtils.getWebDriverByConfig(config))) {
            browser.justDoIt();
        }
    }

}
