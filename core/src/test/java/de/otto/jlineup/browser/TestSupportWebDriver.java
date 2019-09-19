package de.otto.jlineup.browser;

import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

public interface TestSupportWebDriver extends WebDriver, JavascriptExecutor, TakesScreenshot, HasCapabilities {
    //Just an interface for mocking
}
