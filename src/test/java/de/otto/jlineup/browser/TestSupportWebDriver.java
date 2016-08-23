package de.otto.jlineup.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public interface TestSupportWebDriver extends WebDriver, JavascriptExecutor, TakesScreenshot {
    //Just an interface for mocking
}
