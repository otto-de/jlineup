package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.files.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Parameters parameters = new Parameters();
        new JCommander(parameters, args);

        if (parameters.isBefore()) {
            prepareDirectoriesForBeforeStep(parameters);
        }

        Config config = null;
        try {
            config = Config.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        final Browser browser = new Browser(parameters, config);
        try {
            browser.browseAndTakeScreenshots();
        } finally {
            browser.close();
        }
    }

    private static void prepareDirectoriesForBeforeStep(Parameters parameters) {

        try {
            FileUtils.createDirIfNotExists(parameters.getWorkingDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open working directory.");
            System.exit(1);
        }

        try {
            final String screenshotDirectoryPath = parameters.getWorkingDirectory() + "/" + parameters.getScreenshotDirectory();
            FileUtils.createDirIfNotExists(screenshotDirectoryPath);
            FileUtils.clearDirectory(screenshotDirectoryPath);
        } catch (IOException e) {
            System.err.println("Could not create or open screenshots directory.");
            System.exit(1);
        }
    }


}
