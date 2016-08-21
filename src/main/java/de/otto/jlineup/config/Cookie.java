package de.otto.jlineup.config;

import java.util.Date;

public class Cookie {
    public final String name;
    public final String value;
    public final String domain;
    public final String path;
    public final Date expiry;
    public final boolean secure;

    public Cookie(String name, String value, String domain, String path, Date expiry, boolean secure) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.expiry = expiry;
        this.secure = secure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cookie cookie = (Cookie) o;

        if (secure != cookie.secure) return false;
        if (name != null ? !name.equals(cookie.name) : cookie.name != null) return false;
        if (value != null ? !value.equals(cookie.value) : cookie.value != null) return false;
        if (domain != null ? !domain.equals(cookie.domain) : cookie.domain != null) return false;
        if (path != null ? !path.equals(cookie.path) : cookie.path != null) return false;
        return expiry != null ? expiry.equals(cookie.expiry) : cookie.expiry == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (expiry != null ? expiry.hashCode() : 0);
        result = 31 * result + (secure ? 1 : 0);
        return result;
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
                '}';
    }
}
