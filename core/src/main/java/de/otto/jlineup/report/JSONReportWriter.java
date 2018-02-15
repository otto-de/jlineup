package de.otto.jlineup.report;

import java.io.FileNotFoundException;

public interface JSONReportWriter {

    public void writeComparisonReportAsJson(Report report) throws FileNotFoundException;
}
