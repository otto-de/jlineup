package de.otto.jlineup.report;

import com.google.common.annotations.VisibleForTesting;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.otto.jlineup.file.FileService.DIVIDER;
import static de.otto.jlineup.file.FileService.PNG_EXTENSION;
import static de.otto.jlineup.file.FileService.AFTER;
import static de.otto.jlineup.file.FileService.BEFORE;

public class ScreenshotsComparator {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotsComparator.class);

    private static final String BEFORE_MATCHER = DIVIDER + BEFORE + PNG_EXTENSION;
    private static final String AFTER_MATCHER = DIVIDER + AFTER + PNG_EXTENSION;

    final private Parameters parameters;
    final private Config config;
    final private FileService fileService;
    final private ImageService imageService;

    public ScreenshotsComparator(Parameters parameters, Config config, FileService fileService, ImageService imageService) {
        this.parameters = parameters;
        this.config = config;
        this.fileService = fileService;
        this.imageService = imageService;
    }

    public List<ScreenshotComparisonResult> compare() throws IOException {
        LOG.debug("Comparing images...");
        List<ScreenshotComparisonResult> screenshotComparisonResults = new ArrayList<>();

        for (Map.Entry<String, UrlConfig> urlConfigEntry : config.getUrls().entrySet()) {
            String url = urlConfigEntry.getKey();
            UrlConfig urlConfig = urlConfigEntry.getValue();
            LOG.debug("Url: {}", url);
            for (String path : urlConfig.paths) {
                LOG.debug("Path: {}", path);
                String fullUrlWithPath = BrowserUtils.buildUrl(url, path, urlConfig.envMapping);

                final List<String> beforeFileNamesList = fileService.getFilenamesForStep(parameters, path, url, BEFORE);
                final List<String> afterFileNamesList = new ArrayList<>();
                beforeFileNamesList.forEach(filename -> afterFileNamesList.add(switchAfterWithBeforeInFileName(filename)));

                final Set<String> beforeFileNamesSet = new HashSet<>(beforeFileNamesList);
                final Set<String> afterFileNamesSet = new HashSet<>(fileService.getFilenamesForStep(parameters, path, url, AFTER));

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
                        imageBefore = fileService.readScreenshot(parameters, beforeFileName);
                    } catch (IIOException e) {
                        System.err.println("Can't read screenshot of 'before' step. Did you run JLineup with --before parameter before trying to run --after or --compare?");
                        throw e;
                    }
                    BufferedImage imageAfter;
                    try {
                        imageAfter = fileService.readScreenshot(parameters, afterFileName);
                    } catch (IIOException e) {
                        screenshotComparisonResults.add(ScreenshotComparisonResult.noAfterImageComparisonResult(fullUrlWithPath, windowWidth, yPosition, beforeFileName));
                        continue;
                    }

                    ImageService.ImageComparisonResult imageComparisonResult = imageService.compareImages(imageBefore, imageAfter, config.getWindowHeight());
                    String differenceImagePath = null;
                    if (imageComparisonResult.getDifference() > 0 && imageComparisonResult.getDifferenceImage().isPresent()) {
                        differenceImagePath = fileService.writeScreenshot(imageComparisonResult.getDifferenceImage().orElse(null), parameters, url, path, windowWidth, yPosition, "DIFFERENCE");
                    }
                    screenshotComparisonResults.add(new ScreenshotComparisonResult(fullUrlWithPath, windowWidth, yPosition, imageComparisonResult.getDifference(), beforeFileName, afterFileName, differenceImagePath));
                }

                addMissingBeforeFilesToResults(screenshotComparisonResults, fullUrlWithPath, afterFileNamesWithNoBeforeFile);
            }
        }
        screenshotComparisonResults.sort(Comparator.<ScreenshotComparisonResult, String>comparing(rl -> rl.url).thenComparing(r -> r.width).thenComparing(l -> l.verticalScrollPosition));
        return screenshotComparisonResults;
    }

    private void addMissingBeforeFilesToResults(List<ScreenshotComparisonResult> screenshotComparisonResults, String fullUrlWithPath, List<String> afterFileNamesWithNoBeforeFile) {
        screenshotComparisonResults.addAll(afterFileNamesWithNoBeforeFile
                .stream()
                .map(remainingFile -> ScreenshotComparisonResult.noBeforeImageComparisonResult(
                    fullUrlWithPath,
                    extractWindowWidthFromFileName(remainingFile),
                    extractVerticalScrollPositionFromFileName(remainingFile),
                    remainingFile))
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
