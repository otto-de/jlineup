package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static de.otto.jlineup.config.JobConfig.*;

public class DeviceConfig {

    static final String DESKTOP_DEVICE_NAME = "DESKTOP";
    static final String MOBILE_DEVICE_NAME = "MOBILE";
    private static final String DEFAULT_DEVICE_NAME = DESKTOP_DEVICE_NAME;
    private static final String DEFAULT_USER_AGENT = null;
    private static final boolean DEFAULT_TOUCH_OPTION = false;

    //
    // See http://chromedriver.chromium.org/mobile-emulation for information about Chrome mobile emulation
    //

    public final int width;

    public final int height;

    @JsonProperty("pixel-ratio")
    @JsonAlias("pixelRatio")
    public final float pixelRatio;

    @JsonProperty("device-name")
    @JsonAlias("deviceName")
    public final String deviceName;

    @JsonProperty("user-agent")
    @JsonAlias("userAgent")
    public final String userAgent;

    public final boolean touch;


    public DeviceConfig() {
        deviceName = DEFAULT_DEVICE_NAME;
        width = DEFAULT_WINDOW_WIDTH;
        height = DEFAULT_WINDOW_HEIGHT;
        pixelRatio = DEFAULT_PIXEL_RATIO;
        touch = DEFAULT_TOUCH_OPTION;
        userAgent = DEFAULT_USER_AGENT;
    }

    private DeviceConfig(int width, int height, float pixelRatio, String deviceName, String userAgent, boolean touch) {
        this.width = width;
        this.height = height;
        this.pixelRatio = pixelRatio;
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.touch = touch;
    }

    private DeviceConfig(Builder builder) {
        width = builder.width;
        height = builder.height;
        pixelRatio = builder.pixelRatio;
        deviceName = builder.deviceName;
        userAgent = builder.userAgent;
        touch = builder.touch;
    }

    public static DeviceConfig legacyWithWidth(int width) {
        return new DeviceConfig(width, DEFAULT_WINDOW_HEIGHT, DEFAULT_PIXEL_RATIO, DEFAULT_DEVICE_NAME, DEFAULT_USER_AGENT, DEFAULT_TOUCH_OPTION);
    }

    public static Builder deviceConfigBuilder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceConfig that = (DeviceConfig) o;
        return width == that.width &&
                height == that.height &&
                Float.compare(that.pixelRatio, pixelRatio) == 0 &&
                touch == that.touch &&
                Objects.equals(deviceName, that.deviceName) &&
                Objects.equals(userAgent, that.userAgent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, pixelRatio, deviceName, userAgent, touch);
    }

    @Override
    public String toString() {
        return "DeviceConfig{" +
                "width=" + width +
                ", height=" + height +
                ", pixelRatio=" + pixelRatio +
                ", deviceName='" + deviceName + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", touch=" + touch +
                '}';
    }

    public boolean isDesktop() {
        return DESKTOP_DEVICE_NAME.equalsIgnoreCase(deviceName.trim());
    }

    public boolean isMobile() {
        return !DESKTOP_DEVICE_NAME.equalsIgnoreCase(deviceName.trim());
    }

    public boolean isGenericMobile() {
        return MOBILE_DEVICE_NAME.equalsIgnoreCase(deviceName.trim());
    }

    public static final class Builder {
        private int width = DEFAULT_WINDOW_WIDTH;
        private int height = DEFAULT_WINDOW_HEIGHT;
        private float pixelRatio = DEFAULT_PIXEL_RATIO;
        private String deviceName = DEFAULT_DEVICE_NAME;
        private String userAgent = DEFAULT_USER_AGENT;
        private boolean touch = DEFAULT_TOUCH_OPTION;

        private Builder() {
        }

        public Builder withWidth(int val) {
            width = val;
            return this;
        }

        public Builder withHeight(int val) {
            height = val;
            return this;
        }

        public Builder withPixelRatio(float val) {
            pixelRatio = val;
            return this;
        }

        public Builder withDeviceName(String val) {
            deviceName = val;
            return this;
        }

        public Builder withUserAgent(String val) {
            userAgent = val;
            return this;
        }

        public Builder withTouch(boolean val) {
            touch = val;
            return this;
        }

        public DeviceConfig build() {
            return new DeviceConfig(this);
        }
    }
}
