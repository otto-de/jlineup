package de.otto.jlineup.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.otto.jlineup.file.FileService;

import java.io.FileNotFoundException;
import java.util.List;

public class ReportGenerator {

    private final FileService fileService;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ReportGenerator(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeComparisonReportAsJson(List<ScreenshotComparisonResult> screenshotComparisonResults) throws FileNotFoundException {
        final String reportJson = gson.toJson(screenshotComparisonResults);
        fileService.writeReportStringIntoFile(reportJson);
    }

}
