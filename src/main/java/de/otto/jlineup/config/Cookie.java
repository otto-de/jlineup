package de.otto.jlineup.config;

public class Cookie {
    public final String name;
    public final String value;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Cookie{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
