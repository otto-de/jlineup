package de.otto.jlineup.report;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.file.FileService;

import java.io.FileNotFoundException;

public class JSONReportWriter_V1 implements JSONReportWriter {

    private final static PropertyNamingStrategy NAMING_STRATEGY = PropertyNamingStrategy.LOWER_CAMEL_CASE;

    private final FileService fileService;

    public JSONReportWriter_V1(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeComparisonReportAsJson(Report report) throws FileNotFoundException {
        final String reportJson = JacksonWrapper.serializeObjectWithPropertyNamingStrategy(report.getFlatResultList(), NAMING_STRATEGY);
        fileService.writeJsonReport(reportJson);
    }
}
