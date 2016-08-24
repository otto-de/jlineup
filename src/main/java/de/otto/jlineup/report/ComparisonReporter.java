package de.otto.jlineup.report;

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
            for (String path : urlConfigEntry.getValue().paths) {
                String url = BrowserUtils.buildUrl(urlConfigEntry.getKey(), path, urlConfigEntry.getValue().envMapping);

                final List<String> beforeFiles = getBeforeFileNames(parameters, path, url);
                final List<String> afterFiles = new ArrayList<>();
                beforeFiles.forEach(filename -> afterFiles.add(filename.replace("_" + BEFORE + ".png", "_" + AFTER + ".png")));

                for (int i=0; i<beforeFiles.size(); i++) {
                    String beforeFileName = beforeFiles.get(i);
                    String afterFileName = afterFiles.get(i);
                    BufferedImage imageBefore = ImageIO.read(new File(beforeFileName));
                    BufferedImage imageAfter = ImageIO.read(new File(afterFileName));

                    int yPosition = extractVerticalScrollPositionFromFileName(beforeFileName);
                    int windowWidth = extractWindowWidthFromFileName(beforeFileName);

                    final String differenceImageFileName = BrowserUtils.getFullScreenshotFileNameWithPath(parameters, url, path, windowWidth, yPosition, "DIFFERENCE");
                    ImageUtils.BufferedImageComparisonResult bufferedImageComparisonResult = ImageUtils.generateDifferenceImage(imageBefore, imageAfter, 1000);
                    if (bufferedImageComparisonResult.getDifference() > 0) {
                        ImageIO.write(bufferedImageComparisonResult.getDifferenceImage().orElse(null), "png", new File(differenceImageFileName));
                    }
                    result.add(new ComparisonResult(url, windowWidth, yPosition, bufferedImageComparisonResult.getDifference(), beforeFileName, afterFileName, bufferedImageComparisonResult.getDifference() > 0 ? differenceImageFileName : null));
                }
            }
        }
        return result;
    }

    private static List<String> getBeforeFileNames(Parameters parameters, String path, String url) throws IOException {
        final List<String> beforeFiles = new ArrayList<>();
        PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:*"+ BrowserUtils.generateScreenshotFileNamePrefix(url, path)+"*_" + BEFORE + ".png");

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                new File(FileUtils.getScreenshotDirectory(parameters).toString()).toPath(), pathMatcher::matches)) {
            dirStream.forEach(filePath -> beforeFiles.add(filePath.getFileName().toString()));
        }
        return beforeFiles;
    }

    final private static Pattern FIND_VERTICAL_SCROLL_POSITION_PATTERN = Pattern.compile("_([0-9]*?)_\\D*$");

    static int extractVerticalScrollPositionFromFileName(String fileName) {
        Matcher matcher = FIND_VERTICAL_SCROLL_POSITION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String group = matcher.group(1);
            return Integer.parseInt(group);
        }
        return 0;
    }

    final private static Pattern FIND_WINDOW_WIDTH_PATTERN = Pattern.compile("([0-9]*?)_[0-9]*?_\\D*$");
    static int extractWindowWidthFromFileName(String fileName) {
        Matcher matcher = FIND_WINDOW_WIDTH_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String group = matcher.group(1);
            return Integer.parseInt(group);
        }
        return 0;
    }
}
