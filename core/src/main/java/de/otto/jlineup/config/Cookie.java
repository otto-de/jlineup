package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Date;
import java.util.Objects;

@JsonDeserialize(builder = Cookie.Builder.class)
public class Cookie {

    public static final String COOKIE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public final String name;
    public final String value;
    public final String domain;
    public final String path;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = COOKIE_TIME_FORMAT, timezone = "UTC")
    @JsonDeserialize(using = CustomDateDeserializer.class)
    public final Date expiry;
    public final boolean secure;

    public final Boolean showInReport;

    public Cookie(String name, String value, String domain, String path, Date expiry, boolean secure, Boolean showInReport) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.expiry = expiry;
        this.secure = secure;
        this.showInReport = showInReport;
    }

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
        this.domain = null;
        this.path = null;
        this.expiry = null;
        this.secure = false;
        this.showInReport = null;
    }

    private Cookie(Builder builder) {
        name = builder.name;
        value = builder.value;
        domain = builder.domain;
        path = builder.path;
        expiry = builder.expiry;
        secure = builder.secure;
        showInReport = builder.showInReport;
    }

    public static Builder cookieBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(Cookie copy) {
        Builder builder = new Builder();
        builder.name = copy.getName();
        builder.value = copy.getValue();
        builder.domain = copy.getDomain();
        builder.path = copy.getPath();
        builder.expiry = copy.getExpiry();
        builder.secure = copy.isSecure();
        return builder;
    }

    /*
     *
     *
     *
     *  BEGIN of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }

    public Date getExpiry() {
        return expiry;
    }

    public boolean isSecure() {
        return secure;
    }

    public Boolean getShowInReport() {
        return showInReport;
    }

    /*
     *
     *
     *
     *  END of getters block
     *
     *  For GraalVM (JSON is empty if no getters are here)
     *
     *
     *
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cookie cookie = (Cookie) o;
        return secure == cookie.secure && Objects.equals(name, cookie.name) && Objects.equals(value, cookie.value) && Objects.equals(domain, cookie.domain) && Objects.equals(path, cookie.path) && Objects.equals(expiry, cookie.expiry) && Objects.equals(showInReport, cookie.showInReport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, domain, path, expiry, secure, showInReport);
    }

    @Override
    public String toString() {
        return "Cookie{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", domain='" + domain + '\'' +
                ", path='" + path + '\'' +
                ", expiry=" + expiry +
                ", secure=" + secure +
                ", showInReport=" + showInReport +
                '}';
    }


    public static final class Builder {
        private String name;
        private String value;
        private String domain;
        private String path;
        private Date expiry;
        private boolean secure;
        public Boolean showInReport;

        private Builder() {
        }

        public Builder withName(String val) {
            name = val;
            return this;
        }

        public Builder withValue(String val) {
            value = val;
            return this;
        }

        public Builder withDomain(String val) {
            domain = val;
            return this;
        }

        public Builder withPath(String val) {
            path = val;
            return this;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = COOKIE_TIME_FORMAT, timezone = "UTC")
        @JsonDeserialize(using = CustomDateDeserializer.class)
        public Builder withExpiry(Date val) {
            expiry = val;
            return this;
        }

        public Builder withSecure(boolean val) {
            secure = val;
            return this;
        }

        public Builder withShowInReport(Boolean val){
            showInReport = val;
            return this;
        }

        public Cookie build() {
            return new Cookie(this);
        }
    }
}
