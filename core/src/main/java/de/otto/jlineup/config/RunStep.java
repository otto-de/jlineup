package de.otto.jlineup.config;

import de.otto.jlineup.browser.BrowserStep;

public enum RunStep {
    before,
    after,
    after_only,
    compare;

    public BrowserStep toBrowserStep() {
        if (this == before) return BrowserStep.before;
        else return BrowserStep.after;
    }
}
