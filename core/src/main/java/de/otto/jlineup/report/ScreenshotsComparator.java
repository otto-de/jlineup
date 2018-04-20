package de.otto.jlineup.report;

import com.google.common.annotations.VisibleForTesting;
import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.otto.jlineup.file.FileService.*;

public class ScreenshotsComparator {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotsComparator.class);

    private static final String BEFORE_MATCHER = DIVIDER + BEFORE + PNG_EXTENSION;
    private static final String AFTER_MATCHER = DIVIDER + AFTER + PNG_EXTENSION;

    private final RunStepConfig runStepConfig;
    private final JobConfig jobConfig;
    private final FileService fileService;
    private final ImageService imageService;

    public ScreenshotsComparator(RunStepConfig runStepConfig,
                                 JobConfig jobConfig,
                                 FileService fileService,
                                 ImageService imageService) {
        this.runStepConfig = runStepConfig;
        this.jobConfig = jobConfig;
        this.fileService = fileService;
        this.imageService = imageService;
    }

    public Map<String, List<ScreenshotComparisonResult>> compare() throws IOException {
        LOG.debug("Comparing images...");
        if (jobConfig.urls == null) {
            LOG.debug("No urls configured, so no comparison.");
            return null;
        }
        Map<String, List<ScreenshotComparisonResult>> results = new HashMap<>();
        for (Map.Entry<String, UrlConfig> urlConfigEntry : jobConfig.urls.entrySet()) {
            List<ScreenshotComparisonResult> screenshotComparisonResults = new ArrayList<>();
            String url = BrowserUtils.prepareDomain(runStepConfig, urlConfigEntry.getKey());
            UrlConfig urlConfig = urlConfigEntry.getValue();
            LOG.debug("Url: {}", url);
            for (String path : urlConfig.paths) {
                LOG.debug("Path: {}", path);
                String fullUrlWithPath = BrowserUtils.buildUrl(url, path, urlConfig.envMapping);

                final List<String> beforeFileNamesList = fileService.getFilenamesForStep(path, url, BEFORE);
                final List<String> afterFileNamesList = new ArrayList<>();
                beforeFileNamesList.forEach(filename -> afterFileNamesList.add(switchAfterWithBeforeInFileName(filename)));

                final Set<String> beforeFileNamesSet = new HashSet<>(beforeFileNamesList);
                final Set<String> afterFileNamesSet = new HashSet<>(fileService.getFilenamesForStep(path, url, AFTER));

                //we need after files that have no before file in the final report
                final List<String> afterFileNamesWithNoBeforeFile = new ArrayList<>();
                afterFileNamesSet.stream()
                        .filter(name -> !beforeFileNamesSet.contains(switchAfterWithBeforeInFileName(name)))
                        .forEach(afterFileNamesWithNoBeforeFile::add);

                for (int i = 0; i < beforeFileNamesList.size(); i++) {
                    String beforeFileName = beforeFileNamesList.get(i);
                    String afterFileName = afterFileNamesList.get(i);

                    LOG.debug("Comparing '{}' with '{}'", beforeFileName, afterFileName);

                    int yPosition = extractVerticalScrollPositionFromFileName(beforeFileName);
                    int windowWidth = extractWindowWidthFromFileName(beforeFileName);

                    BufferedImage imageBefore;
                    try {
                        imageBefore = fileService.readScreenshot(beforeFileName);
                    } catch (IIOException e) {
                        System.err.println("Can't read screenshot of 'before' step. Did you run JLineup with '--step before' parameter before trying to run '--step after' or --compare?");
                        throw e;
                    }

                    BufferedImage imageAfter;
                    try {
                        imageAfter = fileService.readScreenshot(afterFileName);
                    } catch (IIOException e) {
                        screenshotComparisonResults.add(ScreenshotComparisonResult.noAfterImageComparisonResult(fullUrlWithPath, windowWidth, yPosition, buildRelativePathFromReportDir(beforeFileName)));
                        continue;
                    }

                    ImageService.ImageComparisonResult imageComparisonResult = imageService.compareImages(imageBefore, imageAfter, jobConfig.windowHeight);
                    String differenceImageFileName = null;
                    if (imageComparisonResult.getDifference() > 0 && imageComparisonResult.getDifferenceImage().isPresent()) {
                        differenceImageFileName = Paths.get(fileService.writeScreenshot(imageComparisonResult.getDifferenceImage().orElse(null), url, path, windowWidth, yPosition, "DIFFERENCE")).getFileName().toString();
                    }
                    screenshotComparisonResults.add(new ScreenshotComparisonResult(fullUrlWithPath, windowWidth, yPosition, imageComparisonResult.getDifference(),
                            buildRelativePathFromReportDir(beforeFileName),
                            buildRelativePathFromReportDir(afterFileName),
                            buildRelativePathFromReportDir(differenceImageFileName)));
                }

                addMissingBeforeFilesToResults(screenshotComparisonResults, fullUrlWithPath, afterFileNamesWithNoBeforeFile);
            }
            screenshotComparisonResults.sort(Comparator.<ScreenshotComparisonResult, String>comparing(r -> r.url).thenComparing(r -> r.width).thenComparing(r -> r.verticalScrollPosition));
            results.put(urlConfigEntry.getKey(), screenshotComparisonResults);
        }
        return results;
    }

    private String buildRelativePathFromReportDir(String imageFileName) {
        return imageFileName != null ? fileService.getRelativePathFromReportDirToScreenshotsDir() + imageFileName : null;
    }

    private void addMissingBeforeFilesToResults(List<ScreenshotComparisonResult> screenshotComparisonResults, String fullUrlWithPath, List<String> afterFileNamesWithNoBeforeFile) {
        screenshotComparisonResults.addAll(afterFileNamesWithNoBeforeFile
                .stream()
                .map(remainingFile -> ScreenshotComparisonResult.noBeforeImageComparisonResult(
                        fullUrlWithPath,
                        extractWindowWidthFromFileName(remainingFile),
                        extractVerticalScrollPositionFromFileName(remainingFile),
                        buildRelativePathFromReportDir(remainingFile)))
                .collect(Collectors.toList()));
    }

    @VisibleForTesting
    static String switchAfterWithBeforeInFileName(String filename) {
        if (filename.contains(BEFORE_MATCHER)) {
            return filename.replace(BEFORE_MATCHER, AFTER_MATCHER);
        } else if (filename.contains(AFTER_MATCHER)) {
            return filename.replace(AFTER_MATCHER, BEFORE_MATCHER);
        }
        return filename;
    }

    final private static Pattern FIND_VERTICAL_SCROLL_POSITION_PATTERN = Pattern.compile("_([0-9]*?)_\\D*$");

    @VisibleForTesting
    static int extractVerticalScrollPositionFromFileName(String fileName) {
        Matcher matcher = FIND_VERTICAL_SCROLL_POSITION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String group = matcher.group(1);
            return Integer.parseInt(group);
        }
        return 0;
    }

    final private static Pattern FIND_WINDOW_WIDTH_PATTERN = Pattern.compile("([0-9]*?)_[0-9]*?_\\D*$");

    @VisibleForTesting
    static int extractWindowWidthFromFileName(String fileName) {
        Matcher matcher = FIND_WINDOW_WIDTH_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String group = matcher.group(1);
            return Integer.parseInt(group);
        }
        return 0;
    }
}
