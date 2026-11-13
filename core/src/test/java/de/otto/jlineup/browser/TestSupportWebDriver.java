package de.otto.jlineup.browser;

import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Interactive;

public interface TestSupportWebDriver extends WebDriver, JavascriptExecutor, TakesScreenshot, HasCapabilities, Interactive {
    //Just an interface for mocking
}
