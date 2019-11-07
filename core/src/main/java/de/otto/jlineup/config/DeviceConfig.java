package de.otto.jlineup.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

import static de.otto.jlineup.config.JobConfig.*;

@JsonDeserialize(builder = DeviceConfig.Builder.class)
//@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class DeviceConfig  {

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

    @JsonAlias("pixelRatio")
    public final float pixelRatio;

    @JsonAlias("deviceName")
    public final String deviceName;

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

    public static DeviceConfig deviceConfig(int width, int height) {
        return DeviceConfig.deviceConfigBuilder().withWidth(width).withHeight(height).build();
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getPixelRatio() {
        return pixelRatio;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }
////@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
    public boolean isTouch() {
        return touch;
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

    public static Builder deviceConfigBuilder() {
        return new Builder();
    }

    @JsonIgnore
    public boolean isDesktop() {
        return DESKTOP_DEVICE_NAME.equalsIgnoreCase(deviceName.trim());
    }

    @JsonIgnore
    public boolean isMobile() {
        return !DESKTOP_DEVICE_NAME.equalsIgnoreCase(deviceName.trim());
    }

    @JsonIgnore
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

        @JsonAlias("pixelRatio")
        public Builder withPixelRatio(float val) {
            pixelRatio = val;
            return this;
        }

        @JsonAlias("deviceName")
        public Builder withDeviceName(String val) {
            deviceName = val;
            return this;
        }

        @JsonAlias("userAgent")
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
}
