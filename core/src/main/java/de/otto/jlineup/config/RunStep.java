package de.otto.jlineup.config;

import de.otto.jlineup.browser.BrowserStep;

public enum RunStep {
    before,
    after,
    after_only,
    compare;

    public BrowserStep toBrowserStep() {
        return switch (this) {
            case before -> BrowserStep.before;
            case compare -> BrowserStep.compare;
            case after, after_only -> BrowserStep.after;
        };
    }
}
