package de.otto.jlineup.report;

import com.google.common.annotations.VisibleForTesting;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ComparisonResult;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileUtils;
import de.otto.jlineup.image.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.otto.jlineup.file.FileUtils.DIVIDER;
import static de.otto.jlineup.file.FileUtils.PNG_EXTENSION;
import static de.otto.jlineup.file.FileUtils.AFTER;
import static de.otto.jlineup.file.FileUtils.BEFORE;

public class ScreenshotsComparator {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotsComparator.class);

    private static final String BEFORE_MATCHER = DIVIDER + BEFORE + PNG_EXTENSION;
    private static final String AFTER_MATCHER = DIVIDER + AFTER + PNG_EXTENSION;

    final private Parameters parameters;
    final private Config config;
    final private DifferenceFileWriter differenceFileWriter;

    public ScreenshotsComparator(Parameters parameters, Config config) {
        this.parameters = parameters;
        this.config = config;
        this.differenceFileWriter = new DifferenceFileWriter();
    }

    @VisibleForTesting
    ScreenshotsComparator(Parameters parameters, Config config, DifferenceFileWriter differenceFileWriter) {
        this.parameters = parameters;
        this.config = config;
        this.differenceFileWriter = differenceFileWriter;
    }

    public List<ComparisonResult> compare() throws IOException {
        LOG.debug("Comparing images...");
        List<ComparisonResult> result = new ArrayList<>();
        for (Map.Entry<String, UrlConfig> urlConfigEntry : config.getUrls().entrySet()) {
            String url = urlConfigEntry.getKey();
            UrlConfig urlConfig = urlConfigEntry.getValue();
            LOG.debug("Url: {}", url);
            for (String path : urlConfig.paths) {
                LOG.debug("Path: {}", path);
                String fullUrlWithPath = BrowserUtils.buildUrl(url, path, urlConfig.envMapping);

                final List<String> beforeFiles = getFilenamesForStep(parameters, path, url, BEFORE);
                Collections.sort(beforeFiles, Comparator.naturalOrder());
                final List<String> afterFiles = new ArrayList<>();
                beforeFiles.forEach(filename -> afterFiles.add(switchAfterWithBeforeInFileName(filename)));

                final Set<String> beforeFileNames = new HashSet<>(beforeFiles);
                final Set<String> afterFileNames = new HashSet<>(getFilenamesForStep(parameters, path, url, AFTER));

                final List<String> afterFileNamesWithNoBeforeFile = new ArrayList<>();
                afterFileNames.stream().filter(name -> !beforeFileNames.contains(switchAfterWithBeforeInFileName(name))).forEach(afterFileNamesWithNoBeforeFile::add);


                for (int i = 0; i < beforeFiles.size(); i++) {
                    String beforeFileName = beforeFiles.get(i);
                    String afterFileName = afterFiles.get(i);

                    LOG.debug("Comparing '{}' with '{}'", beforeFileName, afterFileName);

                    int yPosition = extractVerticalScrollPositionFromFileName(beforeFileName);
                    int windowWidth = extractWindowWidthFromFileName(beforeFileName);

                    BufferedImage imageBefore;
                    try {
                        imageBefore = ImageIO.read(new File(FileUtils.getFullScreenshotFileNameWithPath(parameters, beforeFileName)));
                    } catch (IIOException e) {
                        System.err.println("Can't read screenshot of 'before' step. Did you run JLineup with --before parameter before trying to run --after or --compare?");
                        throw e;
                    }
                    BufferedImage imageAfter;
                    try {
                        imageAfter = ImageIO.read(new File(FileUtils.getFullScreenshotFileNameWithPath(parameters, afterFileName)));
                    } catch (IIOException e) {
                        result.add(ComparisonResult.noAfterImageComparisonResult(fullUrlWithPath, windowWidth, yPosition, beforeFileName));
                        continue;
                    }

                    final String differenceImageFileName = FileUtils.getFullScreenshotFileNameWithPath(parameters, url, path, windowWidth, yPosition, "DIFFERENCE");
                    ImageUtils.BufferedImageComparisonResult bufferedImageComparisonResult = ImageUtils.generateDifferenceImage(imageBefore, imageAfter, config.getWindowHeight());
                    if (bufferedImageComparisonResult.getDifference() > 0) {
                        differenceFileWriter.writeDifferenceFile(differenceImageFileName, bufferedImageComparisonResult);
                    }
                    result.add(new ComparisonResult(fullUrlWithPath, windowWidth, yPosition, bufferedImageComparisonResult.getDifference(), beforeFileName, afterFileName, bufferedImageComparisonResult.getDifference() > 0 ? differenceImageFileName : null));
                }

                result.addAll(afterFileNamesWithNoBeforeFile
                        .stream()
                        .map(remainingFile -> ComparisonResult.noBeforeImageComparisonResult(
                            fullUrlWithPath,
                            extractWindowWidthFromFileName(remainingFile),
                            extractVerticalScrollPositionFromFileName(remainingFile),
                            remainingFile))
                        .collect(Collectors.toList()));
            }
        }
        return result;
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

    @VisibleForTesting
    static List<String> getFilenamesForStep(Parameters parameters, String path, String url, String step) throws IOException {
        final List<String> beforeFiles = new ArrayList<>();
        final String matcherPattern = "glob:**" + FileUtils.generateScreenshotFileNamePrefix(url, path) + "*_*_" + step + ".png";

        PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher(matcherPattern);

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                FileUtils.getScreenshotDirectory(parameters), pathMatcher::matches)) {
            dirStream.forEach(filePath -> beforeFiles.add(filePath.getFileName().toString()));
        }
        return beforeFiles;
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
