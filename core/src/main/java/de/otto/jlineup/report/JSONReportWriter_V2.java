package de.otto.jlineup.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.file.FileService;

import java.io.FileNotFoundException;

import static de.otto.jlineup.config.Cookie.COOKIE_TIME_FORMAT;

public class JSONReportWriter_V2 implements JSONReportWriter {

    private final FileService fileService;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat(COOKIE_TIME_FORMAT).create();

    public JSONReportWriter_V2(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeComparisonReportAsJson(Report report) throws FileNotFoundException {
        final String reportJson = gson.toJson(report);
        fileService.writeJsonReport(reportJson);
    }
}
