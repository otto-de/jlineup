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

        try {
            FileUtils.createDirIfNotExists(parameters.getWorkingDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open working directory.");
            System.exit(1);
        }

        try {
            FileUtils.createDirIfNotExists(parameters.getWorkingDirectory() + "/" + parameters.getScreenshotDirectory());
        } catch (IOException e) {
            System.err.println("Could not create or open screenshots directory.");
            System.exit(1);
        }

        Config config = null;
        try {
            config = Config.readConfig(parameters.getWorkingDirectory(), parameters.getConfigFile());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        new Browser(parameters).browseAndTakeScreenshots(config, !parameters.isAfter());
    }


}
