package de.otto.jlineup.report;

import de.otto.jlineup.file.FileService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileNotFoundException;
import java.util.*;

public class HTMLReportGenerator {

    private FileService fileService;

    public HTMLReportGenerator(FileService fileService) {
        this.fileService = fileService;
    }

    public void writeReport(List<ScreenshotComparisonResult> screenshotComparisonResults) throws FileNotFoundException {
        fileService.writeHtmlReport(renderReport("report", screenshotComparisonResults));
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

        public int getWidth() {
            return width;
        }

        public List<ScreenshotComparisonResult> getResults() {
            return results;
        }

        public String getShortenedUrl() {
            String shortenedUrl = url;
            if (url.length() > 25) {
                shortenedUrl = "..." + shortenedUrl.substring(shortenedUrl.lastIndexOf("/"), shortenedUrl.length());
            }
            return shortenedUrl;
        }

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
