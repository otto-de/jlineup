package de.otto.jlineup;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.otto.jlineup.browser.Browser;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.image.ImageUtils;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.MarionetteDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.*;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {

    @Parameter(names={"--before", "-b"})
    boolean before = false;

    @Parameter(names={"--after", "-a"})
    boolean after = false;

    @Parameter(names={"--config", "-c"}, description = "Config file - default is 'lineup.json'")
    String configFile = "lineup.json";

    @Parameter(names={"--working-dir", "-d"})
    String workingDirectory = ".";

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        new JCommander(main, args);
        main.run();
    }

    public void run() throws IOException {
        Config config = Config.readConfig(workingDirectory + "/" + configFile);
        config.workingDir = workingDirectory;
        Browser.browseAndTakeScreenshots(config, !after);
    }

}
