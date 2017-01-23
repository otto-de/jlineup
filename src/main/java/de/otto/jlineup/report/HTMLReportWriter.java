package de.otto.jlineup.report;

import de.otto.jlineup.Util;
import de.otto.jlineup.file.FileService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileNotFoundException;
import java.util.*;

public class HTMLReportWriter {

    private FileService fileService;

    public HTMLReportWriter(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeReport(Report report) throws FileNotFoundException {
        fileService.writeHtmlReport(renderReport("report", report.screenshotComparisons));
    }

    String renderReport(String template, List<ScreenshotComparisonResult> screenshotComparisonResults) throws FileNotFoundException {

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode("HTML");
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Map<String, Object> variables = prepareVariablesForReportTemplate(screenshotComparisonResults);

        return templateEngine.process(template, new Context(Locale.US, variables));
    }

    private Map<String, Object> prepareVariablesForReportTemplate(List<ScreenshotComparisonResult> screenshotComparisonResults) {
        Map<String, Object> variables = new HashMap<>();
        List<ScreenshotComparisonResultContext> screenshotComparisonResultContexts = new LinkedList<>();

        String lastContextKey = null;
        ScreenshotComparisonResultContext currentContext = null;
        for (ScreenshotComparisonResult screenshotComparisonResult : screenshotComparisonResults) {
            String context = screenshotComparisonResult.url + "|||" + screenshotComparisonResult.width;
            if (!context.equals(lastContextKey)) {
                lastContextKey = context;
                currentContext = new ScreenshotComparisonResultContext(screenshotComparisonResult.url, screenshotComparisonResult.width);
                screenshotComparisonResultContexts.add(currentContext);
            }
            currentContext.addResult(screenshotComparisonResult);
        }

        variables.put("resultContexts", screenshotComparisonResultContexts);
        variables.put("jlineup_version", Util.readVersion());
        variables.put("jlineup_commit", Util.readCommit());
        return variables;
    }

    private class ScreenshotComparisonResultContext {

        private String url;
        private int width;
        private List<ScreenshotComparisonResult> results;

        public ScreenshotComparisonResultContext(String url, int width) {
            this.url = url;
            this.width = width;
            this.results = new LinkedList<>();
        }

        public void addResult(ScreenshotComparisonResult result) {
            results.add(result);
        }

        public String getUrl() {
            return url;
        }

        @UsedInTemplate
        public int getWidth() {
            return width;
        }

        @UsedInTemplate
        public List<ScreenshotComparisonResult> getResults() {
            return results;
        }

        @UsedInTemplate
        public String getShortenedUrl() {
            String shortenedUrl = url;
            if (url.length() > 25) {
                shortenedUrl = "..." + shortenedUrl.substring(shortenedUrl.lastIndexOf("/"), shortenedUrl.length());
            }
            return shortenedUrl;
        }

        @UsedInTemplate
        public boolean isSuccess()
        {
            for(ScreenshotComparisonResult result : results) {
                if(result.difference > 0)
                    return false;
            }

            return true;
        }
    }

}
