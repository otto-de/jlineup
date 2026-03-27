package de.otto.jlineup.report;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import de.otto.jlineup.JacksonWrapper;
import de.otto.jlineup.file.FileService;

import java.io.FileNotFoundException;

public class JSONReportWriter {

    private final static PropertyNamingStrategy NAMING_STRATEGY = PropertyNamingStrategies.KEBAB_CASE;

    private final FileService fileService;

    public JSONReportWriter(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeComparisonReportAsJson(Report report) throws FileNotFoundException {
        final String reportJson = JacksonWrapper.serializeObjectWithPropertyNamingStrategy(report, NAMING_STRATEGY);
        fileService.writeJsonReport(reportJson);
    }
}
