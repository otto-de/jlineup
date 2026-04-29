package de.otto.jlineup.web.configuration;
public class JLineupWebLambdaProperties {

    private String functionName = "jlineup-lambda";
    private String functionNameBase;
    private String functionNameChromeHeadless;
    private String functionNameFirefoxHeadless;
    private String functionNameWebkitHeadless;

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionNameBase() {
        return functionNameBase;
    }

    public void setFunctionNameBase(String functionNameBase) {
        this.functionNameBase = functionNameBase;
    }

    public String getFunctionNameChromeHeadless() {
        return functionNameChromeHeadless;
    }

    public void setFunctionNameChromeHeadless(String functionNameChromeHeadless) {
        this.functionNameChromeHeadless = functionNameChromeHeadless;
    }

    public String getFunctionNameFirefoxHeadless() {
        return functionNameFirefoxHeadless;
    }

    public void setFunctionNameFirefoxHeadless(String functionNameFirefoxHeadless) {
        this.functionNameFirefoxHeadless = functionNameFirefoxHeadless;
    }

    public String getFunctionNameWebkitHeadless() {
        return functionNameWebkitHeadless;
    }

    public void setFunctionNameWebkitHeadless(String functionNameWebkitHeadless) {
        this.functionNameWebkitHeadless = functionNameWebkitHeadless;
    }

}
