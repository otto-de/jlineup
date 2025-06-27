package de.otto.jlineup.browser;

import de.otto.jlineup.config.JobConfig;
import org.openqa.selenium.*;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.lang.invoke.MethodHandles.lookup;

public class LogErrorChecker {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    void checkForErrors(WebDriver driver, JobConfig jobConfig) {
        if (jobConfig.checkForErrorsInLog) {
            LOG.debug("Checking for errors.");
            LogEntries logEntries;
            try {
                logEntries = driver.manage().logs().get(LogType.BROWSER);
            } catch (UnsupportedCommandException | JsonException e) {
                logEntries = null;
            }

            if (logEntries != null && !logEntries.getAll().isEmpty() && logEntries.getAll().get(0).getLevel().equals(Level.SEVERE)) {
                throw new WebDriverException(logEntries.getAll().get(0).getMessage());
            }

            if (jobConfig.browser.isChrome()) {
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                try {
                    WebElement element = driver.findElement(By.className("error-code"));
                    if (element != null && element.getText() != null) {
                        element.getText();
                        throw new WebDriverException(element.getText());
                    }
                } catch (NoSuchElementException e) {
                    //ignore
                } finally {
                    driver.manage().timeouts().implicitlyWait(Duration.of(Browser.DEFAULT_IMPLICIT_WAIT_TIME_IN_SECONDS, ChronoUnit.SECONDS));
                }
            }
        } else {
            LOG.debug("Not checking for errors in browser log.");
        }
    }
}
