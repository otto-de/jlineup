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

        //Make sure the working dir exists
        if (parameters.isBefore()) {
            createWorkingDirectoryIfNotExists(parameters);
        }
        Config config = readConfig(parameters);
        //Only create screenshots dir if config was found
        if (parameters.isBefore()) {
            createScreenshotDirectoryIfNotExists(parameters);
        }

        final Browser browser = new Browser(parameters, config, Browser.getWebDriverByConfig(config));
        try {
            browser.browseAndTakeScreenshots();
        } finally {
            browser.close();
        }
    }

    private static Config readConfig(Parameters parameters) {
        Config config = null;
        try {
            config = Config.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return config;
    }

    private static void createWorkingDirectoryIfNotExists(Parameters parameters) {
        try {
            FileUtils.createDirIfNotExists(parameters.getWorkingDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open working directory.");
            System.exit(1);
        }
    }

    private static void createScreenshotDirectoryIfNotExists(Parameters parameters) {
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
