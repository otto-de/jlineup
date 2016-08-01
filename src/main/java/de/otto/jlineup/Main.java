package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.Config;

import java.io.IOException;

public class Main {

    @Parameter(names={"--before", "-b"})
    boolean before = false;

    @Parameter(names={"--after", "-a"})
    boolean after = false;

    @Parameter(names={"--config", "-c"}, description = "Config file - default is 'lineup.json'")
    String configFile = "lineup.json";

    @Parameter(names={"--working-dir", "-d"})
    private static String workingDirectory = ".";

    public static String getWorkingDirectory() {
        return workingDirectory;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Main main = new Main();
        new JCommander(main, args);
        main.run();
    }

    public void run() throws IOException, InterruptedException {
        Config config = Config.readConfig(workingDirectory + "/" + configFile);
        Browser.browseAndTakeScreenshots(config, !after);
    }

}
