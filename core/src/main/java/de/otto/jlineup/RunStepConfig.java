package de.otto.jlineup;

import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.config.RunStep;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class RunStepConfig {

    private final String reportDirectory;
    private final String workingDirectory;
    private final String screenshotsDirectory;
    private final RunStep step;
    private final Map<String, String> urlReplacements;
    private final List<String> chromeParameters;
    private final List<String> firefoxParameters;
    private final String webDriverCachePath;
    private final boolean keepExisting;
    private final String refreshUrl;

    private RunStepConfig(Builder builder) {
        reportDirectory = builder.reportDirectory;
        workingDirectory = builder.workingDirectory;
        screenshotsDirectory = builder.screenshotsDirectory;
        step = builder.step;
        urlReplacements = builder.urlReplacements;
        chromeParameters = builder.chromeParameters;
        firefoxParameters = builder.firefoxParameters;
        webDriverCachePath = builder.webDriverCachePath;
        keepExisting = builder.keepExisting;
        refreshUrl = builder.refreshUrl;
    }

    public static Builder runStepConfigBuilder() {
        return new Builder();
    }

    public static Builder copyOfBuilder(RunStepConfig config) {
        return new Builder(config);
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getScreenshotsDirectory() {
        return screenshotsDirectory;
    }

    public RunStep getStep() {
        return step;
    }

    public BrowserStep getBrowserStep() {
        return step.toBrowserStep();
    }

    public Map<String, String> getUrlReplacements() {
        return urlReplacements;
    }

    public List<String> getChromeParameters() {
        return chromeParameters;
    }

    public List<String> getFirefoxParameters() {
        return firefoxParameters;
    }

    public String getWebDriverCachePath() {
        return webDriverCachePath;
    }

    public boolean isKeepExisting() {
        return keepExisting;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public static final class Builder {
        private String reportDirectory;
        private String workingDirectory;
        private String screenshotsDirectory;
        private List<String> chromeParameters = emptyList();
        private List<String> firefoxParameters = emptyList();
        private Map<String, String> urlReplacements = emptyMap();
        private RunStep step;
        private String webDriverCachePath;
        private boolean keepExisting;
        private String refreshUrl;

        private Builder() {
        }

        private Builder(RunStepConfig copy) {
            this.reportDirectory = copy.getReportDirectory();
            this.workingDirectory = copy.getWorkingDirectory();
            this.screenshotsDirectory = copy.getScreenshotsDirectory();
            this.step = copy.getStep();
            this.urlReplacements = copy.getUrlReplacements();
            this.chromeParameters = copy.getChromeParameters();
            this.firefoxParameters = copy.getFirefoxParameters();
            this.webDriverCachePath = copy.getWebDriverCachePath();
            this.keepExisting = copy.isKeepExisting();
            this.refreshUrl = copy.getRefreshUrl();
        }

        public Builder withReportDirectory(String val) {
            reportDirectory = val;
            return this;
        }

        public Builder withWorkingDirectory(String val) {
            workingDirectory = val;
            return this;
        }

        public Builder withScreenshotsDirectory(String val) {
            screenshotsDirectory = val;
            return this;
        }

        public Builder withStep(RunStep val) {
            step = val;
            return this;
        }

        public Builder withFirefoxParameters(List<String> val) {
            firefoxParameters = val;
            return this;
        }

        public Builder withChromeParameters(List<String> val) {
            chromeParameters = val;
            return this;
        }

        public Builder withUrlReplacements(Map<String, String> val) {
            urlReplacements = val;
            return this;
        }

        public Builder withWebDriverCachePath(String val) {
            webDriverCachePath = val;
            return this;
        }

        public Builder withKeepExistingFiles(boolean val) {
            keepExisting = val;
            return this;
        }

        public Builder withRefreshUrl(String val) {
            refreshUrl = val;
            return this;
        }

        public RunStepConfig build() {
            return new RunStepConfig(this);
        }
    }
}
