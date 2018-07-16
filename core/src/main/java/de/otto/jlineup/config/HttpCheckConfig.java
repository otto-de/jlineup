package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.http.HttpStatus.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpCheckConfig {

    public static final List<Integer> DEFAULT_ALLOWED_CODES = Arrays.asList(
            SC_OK,
            SC_ACCEPTED,
            SC_NO_CONTENT,
            SC_RESET_CONTENT,
            SC_PARTIAL_CONTENT,
            SC_MOVED_PERMANENTLY,
            SC_MOVED_TEMPORARILY,
            SC_SEE_OTHER,
            SC_NOT_MODIFIED,
            SC_TEMPORARY_REDIRECT,
            308
    );

    private final boolean enabled;

    @JsonProperty("allowed-codes")
    @JsonAlias({"allowed-codes"})
    private final List<Integer> allowedCodes;

    public HttpCheckConfig(boolean enabled, List<Integer> allowedCodes) {
        this.enabled = enabled;
        this.allowedCodes = allowedCodes == null ? DEFAULT_ALLOWED_CODES : allowedCodes;
    }

    public HttpCheckConfig() {
        this.enabled = false;
        this.allowedCodes = null;
    }

    public HttpCheckConfig(boolean enabled) {
        this.enabled = enabled;
        this.allowedCodes = DEFAULT_ALLOWED_CODES;
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
