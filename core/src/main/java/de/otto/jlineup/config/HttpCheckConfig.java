package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_TEMPORARY_REDIRECT;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpCheckConfig {

    private final boolean enabled;

    @JsonProperty("allowed-codes")
    @JsonAlias({"allowed-codes"})
    private final List<Integer> allowedCodes;

    public HttpCheckConfig(boolean enabled, List<Integer> allowedCodes) {
        this.enabled = enabled;
        this.allowedCodes = allowedCodes;
    }

    public HttpCheckConfig() {
        this.enabled = false;
        this.allowedCodes = null;
    }

    public HttpCheckConfig(boolean enabled) {
        this.enabled = enabled;
        this.allowedCodes = IntStream.range(SC_CREATED, SC_TEMPORARY_REDIRECT).boxed().collect(toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpCheckConfig that = (HttpCheckConfig) o;
        return enabled == that.enabled &&
                Objects.equals(allowedCodes, that.allowedCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, allowedCodes);
    }

    @Override
    public String toString() {
        return "HttpCheckConfig{" +
                "enabled=" + enabled +
                ", allowedCodes=" + allowedCodes +
                '}';
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Integer> getAllowedCodes() {
        return allowedCodes;
    }
}
