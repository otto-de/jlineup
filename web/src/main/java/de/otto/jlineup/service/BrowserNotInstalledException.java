package de.otto.jlineup.service;

import de.otto.jlineup.browser.Browser;

public class BrowserNotInstalledException extends Exception {

    private final Browser.Type desiredBrowser;

    public BrowserNotInstalledException(Browser.Type desiredBrowser) {
        this.desiredBrowser = desiredBrowser;
    }

    public Browser.Type getDesiredBrowser() {
        return desiredBrowser;
    }
}
