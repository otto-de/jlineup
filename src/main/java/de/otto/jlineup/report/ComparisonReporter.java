package de.otto.jlineup.report;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ComparisonResult;
import de.otto.jlineup.config.Config;
import de.otto.jlineup.config.Parameters;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileUtils;
import de.otto.jlineup.image.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.otto.jlineup.image.ImageUtils.AFTER;
import static de.otto.jlineup.image.ImageUtils.BEFORE;

public class ComparisonReporter {

    final private Parameters parameters;
    final private Config config;

    @Inject
    public ComparisonReporter(Parameters parameters, Config config) {
        this.parameters = parameters;
        this.config = config;
    }

    public List<ComparisonResult> compare() throws IOException {

        List<ComparisonResult> result = new ArrayList<>();

        for (Map.Entry<String, UrlConfig> urlConfigEntry : config.getUrls().entrySet()) {

            String url = urlConfigEntry.getKey();

            for (String path : urlConfigEntry.getValue().paths) {

                String fullUrlWithPath = BrowserUtils.buildUrl(url, path, urlConfigEntry.getValue().envMapping);

                final List<String> beforeFiles = getFilenamesForStep(parameters, path, url, BEFORE);
                final List<String> afterFiles = new ArrayList<>();
                beforeFiles.forEach(filename -> afterFiles.add(filename.replace("_" + BEFORE + ".png", "_" + AFTER + ".png")));

                for (int i=0; i<beforeFiles.size(); i++) {
                    String beforeFileName = beforeFiles.get(i);
                    String afterFileName = afterFiles.get(i);
                    BufferedImage imageBefore = ImageIO.read(new File(BrowserUtils.getFullScreenshotFileNameWithPath(parameters, beforeFileName)));
                    BufferedImage imageAfter = ImageIO.read(new File(BrowserUtils.getFullScreenshotFileNameWithPath(parameters, afterFileName)));

                    int yPosition = extractVerticalScrollPositionFromFileName(beforeFileName);
                    int windowWidth = extractWindowWidthFromFileName(beforeFileName);

                    final String differenceImageFileName = BrowserUtils.getFullScreenshotFileNameWithPath(parameters, url, path, windowWidth, yPosition, "DIFFERENCE");
                    ImageUtils.BufferedImageComparisonResult bufferedImageComparisonResult = ImageUtils.generateDifferenceImage(imageBefore, imageAfter, 1000);
                    if (bufferedImageComparisonResult.getDifference() > 0) {
                        ImageIO.write(bufferedImageComparisonResult.getDifferenceImage().orElse(null), "png", new File(differenceImageFileName));
                    }
                    result.add(new ComparisonResult(fullUrlWithPath, windowWidth, yPosition, bufferedImageComparisonResult.getDifference(), beforeFileName, afterFileName, bufferedImageComparisonResult.getDifference() > 0 ? differenceImageFileName : null));
                }
            }
        }
        return result;
    }

    @VisibleForTesting
    static List<String> getFilenamesForStep(Parameters parameters, String path, String url, String step) throws IOException {
        final List<String> beforeFiles = new ArrayList<>();
        final String matcherPattern = "glob:**" + BrowserUtils.generateScreenshotFileNamePrefix(url, path) + "*_*_" + step + ".png";

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
