package de.otto.jlineup.report;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.Utils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.FileNotFoundException;
import java.util.*;

public class HTMLReportWriter {

    private final FileService fileService;

    final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    TemplateEngine templateEngine = new TemplateEngine();

    public HTMLReportWriter(FileService fileService) {
        this.fileService = fileService;

        templateResolver.setTemplateMode("HTML");
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateEngine.setTemplateResolver(templateResolver);
    }

    public void writeReport(Report report) throws FileNotFoundException {
        fileService.writeHtmlReport(renderReport("report", report), FileService.REPORT_HTML_FILENAME);
    }

    public void writeNotFinishedReport(RunStepConfig runStepConfig, JobConfig jobConfig) throws FileNotFoundException {
        fileService.writeHtmlReport(renderNotFinishedReport("report_not_finished", runStepConfig, jobConfig), FileService.REPORT_HTML_FILENAME);
    }

    private String renderReport(String templateName, Report report) {
        final Map<String, Object> variables = new HashMap<>();
        variables.put("report", report);
        enrichVariables(variables);
        return templateEngine.process(templateName, new Context(Locale.US, variables));
    }

    String renderNotFinishedReport(String template, RunStepConfig runStepConfig, JobConfig config) {
        Map<String, Object> variables = new HashMap<>();
        enrichVariables(variables);
        variables.put("config", config);
        variables.put("report_dir", runStepConfig.getReportDirectory());
        variables.put("working_dir", runStepConfig.getWorkingDirectory());
        return templateEngine.process(template, new Context(Locale.US, variables));
    }

    private void enrichVariables(Map<String, Object> variables) {
        variables.put("jlineup_version", Utils.readVersion());
        variables.put("jlineup_commit", Utils.readCommit());

        variables.put("legend_same_rgb", "#" + Integer.toHexString(ImageService.SAME_COLOR).substring(2));
        variables.put("legend_look_same_rgb", "#" + Integer.toHexString(ImageService.LOOK_SAME_COLOR).substring(2));
        variables.put("legend_anti_alias_rgb", "#" + Integer.toHexString(ImageService.ANTI_ALIAS_DETECTED_COLOR).substring(2));
        variables.put("legend_different_rgb", "#" + Integer.toHexString(ImageService.HIGHLIGHT_COLOR).substring(2));
        variables.put("legend_different_size_rgb", "#" + Integer.toHexString(ImageService.DIFFERENT_SIZE_COLOR).substring(2));
    }

    public void writeReportAfterBeforeStep(Report reportBefore) throws FileNotFoundException {
        fileService.writeHtmlReport(renderReport("report_before", reportBefore), FileService.REPORT_BEFORE_HTML_FILENAME);
    }

}

