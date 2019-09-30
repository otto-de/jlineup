package de.otto.jlineup.report;

import de.otto.jlineup.Utils;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
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
        fileService.writeHtmlReport(renderReport("report", report.config, report.getFlatResultList()));
        //fileService.writeHtmlReport(renderReport("report_wip", report.getFlatResultList()), "report_new.html");
    }

    String renderReport(String template, JobConfig config, List<ScreenshotComparisonResult> screenshotComparisonResults) {

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode("HTML");
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        final Map<String, Object> variables = prepareVariablesForReportTemplate(screenshotComparisonResults);
        variables.put("config", config);

        return templateEngine.process(template, new Context(Locale.US, variables));
    }

    private Map<String, Object> prepareVariablesForReportTemplate(final List<ScreenshotComparisonResult> screenshotComparisonResults) {
        Map<String, Object> variables = new HashMap<>();
        List<ScreenshotComparisonResultContext> screenshotComparisonResultContexts = new LinkedList<>();

        int lastContextKey = 0;
        ScreenshotComparisonResultContext currentContext = null;
        for (ScreenshotComparisonResult screenshotComparisonResult : screenshotComparisonResults) {
            int context = getContextHash(screenshotComparisonResult);
            if (context != lastContextKey) {
                lastContextKey = context;
                currentContext = new ScreenshotComparisonResultContext(screenshotComparisonResult.contextHash);
                screenshotComparisonResultContexts.add(currentContext);
            }
            currentContext.addResult(screenshotComparisonResult);
        }

        variables.put("resultContexts", screenshotComparisonResultContexts);
        variables.put("jlineup_version", Utils.readVersion());
        variables.put("jlineup_commit", Utils.readCommit());

        variables.put("legend_same_rgb", "#" + Integer.toHexString(ImageService.SAME_COLOR).substring(2));
        variables.put("legend_look_same_rgb", "#" + Integer.toHexString(ImageService.LOOK_SAME_COLOR).substring(2));
        variables.put("legend_anti_alias_rgb", "#" + Integer.toHexString(ImageService.ANTI_ALIAS_DETECTED_COLOR).substring(2));
        variables.put("legend_different_rgb", "#" + Integer.toHexString(ImageService.HIGHLIGHT_COLOR).substring(2));
        variables.put("legend_different_size_rgb", "#" + Integer.toHexString(ImageService.DIFFERENT_SIZE_COLOR).substring(2));

        return variables;
    }

    private int getContextHash(final ScreenshotComparisonResult screenshotComparisonResult) {
        return screenshotComparisonResult.contextHash;
    }

    private class ScreenshotComparisonResultContext {

        private final List<ScreenshotComparisonResult> results;
        private final ScreenshotContext screenshotContext;
        private final int contextHash;
        private final Map<Step, String> browsers;

        ScreenshotComparisonResultContext(final int contextHash) {
            this.results = new LinkedList<>();
            this.screenshotContext = fileService.getRecordedContext(contextHash);
            this.contextHash = contextHash;
            this.browsers = fileService.getBrowsers();
        }

        void addResult(ScreenshotComparisonResult result) {
            results.add(result);
        }

        @UsedInTemplate
        public int getContextHash() {
            return contextHash;
        }

        @UsedInTemplate
        public String getUrl() {
            return BrowserUtils.buildUrl(screenshotContext.url, screenshotContext.urlSubPath, Collections.emptyMap());
        }

        @UsedInTemplate
        public int getWidth() {
            return screenshotContext.deviceConfig.width;
        }

        @UsedInTemplate
        public String getBrowser(String step) {
            return browsers.get(Step.valueOf(step));
        }

        @UsedInTemplate
        public String getDeviceInfo() {
            StringBuilder sb = new StringBuilder();
            DeviceConfig dc = screenshotContext.deviceConfig;
            if (dc.isMobile()) {
                sb.append(dc.deviceName);
            }
            if (dc.isGenericMobile()) {
                sb.append("\n");
            }
            if (dc.isGenericMobile() || dc.isDesktop()) {
                sb.append("Size: ");
                sb.append(dc.width);
                sb.append("x");
                sb.append(dc.height);
                if (dc.pixelRatio != 1.0f) {
                    sb.append("\nPixel ratio: ");
                    sb.append(dc.pixelRatio);
                }
                if (dc.userAgent != null) {
                    sb.append("\n");
                    sb.append(dc.userAgent);
                }
                if (dc.isDesktop() && dc.touch) {
                    sb.append("\n");
                    sb.append("Touch enabled");
                }
            }
            return sb.toString();
        }

        @UsedInTemplate
        public List<ScreenshotComparisonResult> getResults() {
            return results;
        }

        @UsedInTemplate
        public String getShortenedUrl() {
            String shortenedUrl = getUrl();
            if (shortenedUrl.length() > 25) {
                shortenedUrl = "..." + shortenedUrl.substring(shortenedUrl.lastIndexOf("/"), shortenedUrl.length());
            }
            return shortenedUrl;
        }

        @UsedInTemplate
        public boolean isSuccess() {
            for (ScreenshotComparisonResult result : results) {
                if (result.difference > 0)
                    return false;
            }

            return true;
        }
    }

}
