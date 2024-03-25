package de.otto.jlineup;

public enum GlobalOption {

    JLINEUP_LAMBDA_FUNCTION_NAME,
    JLINEUP_LAMBDA_AWS_PROFILE,
    JLINEUP_LAMBDA_S3_BUCKET;

    public String kebabCaseName() {
        return name().toLowerCase().replace("_", "-");
    }

}
