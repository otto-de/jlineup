package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        final Parameters parameters = new Parameters();
        new JCommander(parameters, args);
        Config config = Config.readConfig(parameters.getWorkingDirectory() + "/" + parameters.getConfigFile());
        new Browser(parameters).browseAndTakeScreenshots(config, !parameters.isAfter());
    }
}
