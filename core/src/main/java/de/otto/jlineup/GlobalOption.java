package de.otto.jlineup;

public enum GlobalOption {

    JLINEUP_LAMBDA_FUNCTION_NAME,
    JLINEUP_LAMBDA_FUNCTION_NAME_BASE,
    JLINEUP_LAMBDA_FUNCTION_NAME_CHROME_HEADLESS,
    JLINEUP_LAMBDA_FUNCTION_NAME_FIREFOX_HEADLESS,
    JLINEUP_LAMBDA_FUNCTION_NAME_WEBKIT_HEADLESS,
    JLINEUP_LAMBDA_AWS_PROFILE,
    JLINEUP_LAMBDA_AWS_REGION,
    JLINEUP_LAMBDA_S3_BUCKET,
    JLINEUP_LAMBDA_S3_PREFIX,
    JLINEUP_CROP_LAST_SCREENSHOT,

    JLINEUP_CHROME_VERSION,
    JLINEUP_FIREFOX_VERSION;

    public String kebabCaseName() {
        return name().toLowerCase().replace("_", "-");
    }

    public String kebabCaseNameWithoutJLineupPrefix() {
        return kebabCaseName().replace("jlineup-", "");
    }

}
