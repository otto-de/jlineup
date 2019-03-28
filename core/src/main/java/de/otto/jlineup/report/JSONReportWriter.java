package de.otto.jlineup.report;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.FileNotFoundException;

public interface JSONReportWriter {

    public void writeComparisonReportAsJson(Report report) throws FileNotFoundException, JsonProcessingException;
}
