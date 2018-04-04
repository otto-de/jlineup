package de.otto.jlineup;

import de.otto.jlineup.config.Step;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class JLineupRunConfiguration {

    private final String reportDirectory;
    private final String workingDirectory;
    private final String screenshotsDirectory;
    private final Step step;
    private final Map<String, String> urlReplacements;

    private JLineupRunConfiguration(Builder builder) {
        reportDirectory = builder.reportDirectory;
        workingDirectory = builder.workingDirectory;
        screenshotsDirectory = builder.screenshotsDirectory;
        step = builder.step;
        urlReplacements = builder.urlReplacements;
    }

    public static Builder jLineupRunConfigurationBuilder() {
        return new Builder();
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


    public static final class Builder {
        private String reportDirectory;
        private String workingDirectory;
        private String screenshotsDirectory;
        private Map<String, String> urlReplacements = emptyMap();
        private Step step;

        private Builder() {
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

        public JLineupRunConfiguration build() {
            return new JLineupRunConfiguration(this);
        }

        public Builder withUrlReplacements(Map<String, String> val) {
            urlReplacements = val;
            return this;
        }
    }
}
