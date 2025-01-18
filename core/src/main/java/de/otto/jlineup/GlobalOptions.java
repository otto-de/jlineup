package de.otto.jlineup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static de.otto.jlineup.GlobalOption.*;

public class GlobalOptions {

    private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GlobalOptions.class);

    private final static String DEFAULT_LAMBDA_FUNCTION_NAME = "jlineup-lambda";
    private final static String DEFAULT_LAMBDA_AWS_PROFILE = "default";
    private final static String DEFAULT_LAMBDA_S3_BUCKET = "jlineup-lambda";

    private final static String DEFAULT_CROP_LAST_SCREENSHOT = "false";

    static private final Map<GlobalOption, String> options;

    static {
        options = new HashMap<>();

        Properties appProps = new Properties();
        try {
            appProps.load(GlobalOptions.class.getResourceAsStream("/settings.properties"));
        } catch (Exception e) {
            LOG.debug("No settings found");
        }

        loadOption(appProps, "JLINEUP_LAMBDA_FUNCTION_NAME", "jlineup.lambda.function-name", DEFAULT_LAMBDA_FUNCTION_NAME, JLINEUP_LAMBDA_FUNCTION_NAME);
        loadOption(appProps, "JLINEUP_AWS_PROFILE", "jlineup.lambda.aws-profile", DEFAULT_LAMBDA_AWS_PROFILE, JLINEUP_LAMBDA_AWS_PROFILE);
        loadOption(appProps, "JLINEUP_LAMBDA_S3_BUCKET", "jlineup.lambda.s3-bucket", DEFAULT_LAMBDA_S3_BUCKET, JLINEUP_LAMBDA_S3_BUCKET);
        loadOption(appProps, "JLINEUP_CROP_LAST_SCREENSHOT", "jlineup.crop-last-screenshot", DEFAULT_CROP_LAST_SCREENSHOT, JLINEUP_CROP_LAST_SCREENSHOT);

        loadOption(appProps, "JLINEUP_CHROME_VERSION", "jlineup.chrome-version", null, JLINEUP_CHROME_VERSION);
        loadOption(appProps, "JLINEUP_FIREFOX_VERSION", "jlineup.firefox-version", null, JLINEUP_FIREFOX_VERSION);
    }

    private static void loadOption(Properties appProps, String key, String property, String defaultValue, GlobalOption option) {
        if (System.getenv(key) != null) {
            options.put(option, System.getenv(key));
        } else if (appProps.getProperty(property) != null) {
            options.put(option, appProps.getProperty(property));
        } else {
            options.put(option, defaultValue);
        }
    }

    public static void setOption(GlobalOption option, String value) {
        options.put(option, value);
    }

    public static String getOption(GlobalOption option) {
        return options.get(option);
    }

    public static String asString() {
        return "GlobalOptions{" +
                "options=" + String.join(", ", options.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList()) +
                '}';
    }
}
