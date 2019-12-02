package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.http.HttpStatus.*;

@JsonDeserialize(builder = HttpCheckConfig.Builder.class)
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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
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

    private HttpCheckConfig(Builder builder) {
        enabled = builder.enabled;
        allowedCodes = builder.allowedCodes;
    }

    public static Builder copyOfBuilder() {
        return new Builder();
    }

    public static Builder httpCheckConfigBuilder(HttpCheckConfig copy) {
        Builder builder = new Builder();
        builder.enabled = copy.isEnabled();
        builder.allowedCodes = copy.getAllowedCodes();
        return builder;
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


    public static final class Builder {
        private boolean enabled;
        private List<Integer> allowedCodes;

        private Builder() {
        }

        public Builder withEnabled(boolean val) {
            enabled = val;
            return this;
        }

        public Builder withAllowedCodes(List<Integer> val) {
            allowedCodes = val;
            return this;
        }

        public HttpCheckConfig build() {
            return new HttpCheckConfig(this);
        }
    }
}
