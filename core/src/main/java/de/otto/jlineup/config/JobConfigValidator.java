package de.otto.jlineup.config;

import de.otto.jlineup.exceptions.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;

public class JobConfigValidator {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static void validateJobConfig(JobConfig jobConfig) {

        //Check urls
        if (jobConfig.urls == null || jobConfig.urls.isEmpty()) {
            throw new ValidationError("No URLs configured.");
        }

        //Check browser window height
        if (jobConfig.windowHeight != null && (jobConfig.windowHeight < 100 || jobConfig.windowHeight > 10000)) {
            throw new ValidationError(String.format("Configured window height is invalid: %d. Valid values are between 100 and 10000", jobConfig.windowHeight));
        }

        for (Map.Entry<String, UrlConfig> urlConfigEntry : jobConfig.urls.entrySet()) {

            UrlConfig urlConfig = urlConfigEntry.getValue();
            String url = urlConfig.url;

            //Check browser window widths
            for (Integer width : ( urlConfig.windowWidths != null ? urlConfig.windowWidths : urlConfig.devices.stream().map(d -> d.width).collect(Collectors.toList()))) {
                if (width < 10 || width > 10000) {
                    throw new ValidationError(String.format("One of the configured window widths for %s is invalid: %d. Valid values are between 10 and 10000", url, width));
                }
            }

            //Check timeouts
            if (urlConfig.waitAfterPageLoad > 20 || urlConfig.waitAfterPageLoad < 0) {
                throw new ValidationError(String.format("Configured wait after page load time of %f seconds for %s is invalid. Valid values are between 0 and 20.", urlConfig.waitAfterPageLoad, url));
            }
            if (urlConfig.waitAfterScroll > 20 || urlConfig.waitAfterScroll < 0) {
                throw new ValidationError(String.format("Configured wait after scroll time of %f seconds for %s is invalid. Valid values are between 0 and 20.", urlConfig.waitAfterScroll, url));
            }
            if (urlConfig.waitForFontsTime > 20 || urlConfig.waitForFontsTime < 0) {
                throw new ValidationError(String.format("Configured wait for fonts time of %f seconds for %s is invalid. Valid values are between 0 and 20.", urlConfig.waitForFontsTime, url));
            }

            //Check max scroll height
            if (urlConfig.maxScrollHeight < 0) {
                throw new ValidationError(String.format("Configured max scroll height (%d) for %s must not be negative)", urlConfig.maxScrollHeight, url));
            }
        }

        jobConfig.urls.forEach((urlKey, urlConfig) -> validateUrlConfig(jobConfig, urlKey));
    }

    private static void validateUrlConfig(JobConfig jobConfig, String urlKey) {

        UrlConfig urlConfig = jobConfig.urls.get(urlKey);

        if ( (urlConfig.devices != null && !urlConfig.devices.isEmpty() ) && urlConfig.windowWidths != null) {
            throw new ValidationError("URL: " + urlKey + "\nDon't mix 'window-widths' (aliases are 'widths' or 'resolutions') and 'devices'.");
        }

        if (urlConfig.devices != null) {
            urlConfig.devices.forEach(deviceConfig -> validateDeviceConfig(jobConfig, urlKey, deviceConfig));
        }

    }

    private static void validateDeviceConfig(JobConfig jobConfig, String urlKey, DeviceConfig deviceConfig) {
        if (deviceConfig.isMobile()) {
            if (!deviceConfig.isGenericMobile()) { //A device name is specified
                if (deviceConfig.userAgent != null) {
                    throw new ValidationError("URLConfig: " + urlKey + "\n" + "Device: " + deviceConfig.deviceName + "\nReason: If you choose a defined device name, the user agent is chosen automatically and can't be overridden.");
                }
            }
            if (!jobConfig.browser.isChrome()) {
                if (deviceConfig.isMobile()) {
                    throw new ValidationError("Mobile emulation is only supported by Chrome/Chromium. You specified " + jobConfig.browser.name() + " as browser.");
                }
            }
        }
    }

}
