package de.otto.jlineup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.invoke.MethodHandles.lookup;

public class JobConfigValidator {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static void validateJobConfig(JobConfig jobConfig) {
        jobConfig.urls.forEach((url, urlConfig) -> validateUrlConfig(jobConfig, url));
    }

    private static void validateUrlConfig(JobConfig jobConfig, String url) {

        UrlConfig urlConfig = jobConfig.urls.get(url);

        if (urlConfig.devices != null && urlConfig.windowWidths != null) {
            throw new ValidationError("URL: " + url + "\nDon't mix 'window-widths' (aliases are 'widths' or 'resolutions') and 'devices'.");
        }

        if (urlConfig.devices != null) {
            urlConfig.devices.forEach(deviceConfig -> validateDeviceConfig(jobConfig, url, deviceConfig));
        }

    }

    private static void validateDeviceConfig(JobConfig jobConfig, String url, DeviceConfig deviceConfig) {
        if (deviceConfig.isMobile()) {
            if (!deviceConfig.isGenericMobile()) { //A device name is specified
                if (deviceConfig.userAgent != null) {
                    throw new ValidationError("URL: " + url + "\n" + "Device: " + deviceConfig.deviceName + "\nReason: If you choose a defined device name, the user agent is chosen automatically and can't be overridden.");
                }
            }
            if (!jobConfig.browser.isChrome()) {
                if (deviceConfig.isMobile()) {
                    throw new ValidationError("Mobile emulation is only supported by Chrome/Chromium. You specified " + jobConfig.browser.name() + " as browser.");
                }
            }
        }
    }

    public static class ValidationError extends RuntimeException {
        public ValidationError(String message) {
            super("Error during job config validation:\n" + message + "\nPlease check your jlineup job config.");
        }
    }
}
