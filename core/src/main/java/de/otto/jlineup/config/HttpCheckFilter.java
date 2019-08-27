package de.otto.jlineup.config;

public class HttpCheckFilter {

    @Override
    public boolean equals(Object obj) {
        return obj == null || obj.equals(new HttpCheckConfig());
    }
}
