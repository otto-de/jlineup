package de.otto.jlineup.browser;

import de.otto.jlineup.config.JobConfig;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class LogErrorChecker {

    void checkForErrors(WebDriver driver, JobConfig jobConfig) {
        LogEntries logEntries;
        try {
            logEntries = driver.manage().logs().get(LogType.BROWSER);
        } catch (UnsupportedCommandException e) {
            logEntries = null;
        }
        if (logEntries != null && !logEntries.getAll().isEmpty() && logEntries.getAll().get(0).getLevel() == Level.SEVERE) {
            throw new WebDriverException(logEntries.getAll().get(0).getMessage());
        }

        if (jobConfig.browser.isChrome()) {
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
            try {
                WebElement element = driver.findElement(By.xpath("//*[@id=\"main-message\"]/div[2]"));
                if (element != null && element.getText() != null) {
                    throw new WebDriverException(element.getText());
                }
            } catch (NoSuchElementException e) {
                //ignore
            } finally {
                driver.manage().timeouts().implicitlyWait(Browser.DEFAULT_IMPLICIT_WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
            }
        }

    }
}
