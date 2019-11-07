package de.otto.jlineup.report;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.file.FileService;

import java.io.FileNotFoundException;

public class JSONReportWriter_V2 implements JSONReportWriter {

    private final static PropertyNamingStrategy NAMING_STRATEGY = PropertyNamingStrategy.KEBAB_CASE;

    private final FileService fileService;

    public JSONReportWriter_V2(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeComparisonReportAsJson(Report report) throws FileNotFoundException {
        final String reportJson = JacksonWrapper.serializeObjectWithPropertyNamingStrategy(report, NAMING_STRATEGY);
        fileService.writeJsonReport(reportJson);
    }
}
