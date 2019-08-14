package de.otto.jlineup;

import de.otto.jlineup.config.Step;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class RunStepConfig {

    private final String reportDirectory;
    private final String workingDirectory;
    private final String screenshotsDirectory;
    private final Step step;
    private final Map<String, String> urlReplacements;
    private final List<String> chromeParameters;
    private final List<String> firefoxParameters;

    private RunStepConfig(Builder builder) {
        reportDirectory = builder.reportDirectory;
        workingDirectory = builder.workingDirectory;
        screenshotsDirectory = builder.screenshotsDirectory;
        step = builder.step;
        urlReplacements = builder.urlReplacements;
        chromeParameters = builder.chromeParameters;
        firefoxParameters = builder.firefoxParameters;
    }

    public static Builder jLineupRunConfigurationBuilder() {
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

    public Step getStep() {
        return step;
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


    public static final class Builder {
        private String reportDirectory;
        private String workingDirectory;
        private String screenshotsDirectory;
        private List<String> chromeParameters = emptyList();
        private List<String> firefoxParameters = emptyList();
        private Map<String, String> urlReplacements = emptyMap();
        private Step step;

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

        public Builder withStep(Step val) {
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

        public RunStepConfig build() {
            return new RunStepConfig(this);
        }

        public Builder withUrlReplacements(Map<String, String> val) {
            urlReplacements = val;
            return this;
        }
    }
}
