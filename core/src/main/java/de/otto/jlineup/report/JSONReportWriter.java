package de.otto.jlineup.report;


import tools.jackson.core.JacksonException;

import java.io.FileNotFoundException;

public interface JSONReportWriter {

    void writeComparisonReportAsJson(Report report) throws FileNotFoundException, JacksonException;
}
