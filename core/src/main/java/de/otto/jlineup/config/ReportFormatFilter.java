package de.otto.jlineup.config;

import static de.otto.jlineup.config.JobConfig.DEFAULT_REPORT_FORMAT;

public class ReportFormatFilter {

    @Override
    public boolean equals(Object obj) {
        return obj == null || obj.equals(DEFAULT_REPORT_FORMAT);
    }

}
