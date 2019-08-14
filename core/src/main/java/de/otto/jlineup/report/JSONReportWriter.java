package de.otto.jlineup.report;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.FileNotFoundException;

public interface JSONReportWriter {

    void writeComparisonReportAsJson(Report report) throws FileNotFoundException, JsonProcessingException;
}
