package de.otto.jlineup;

import de.otto.jlineup.config.Parameters;

public class JLineupOptions {

    private final Parameters parameters;

    public JLineupOptions(Parameters parameters) {
        this.parameters = parameters;
    }

    public Parameters getParameters() {
        return parameters;
    }
}
