package de.otto.jlineup.report;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.Step;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.image.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import static java.lang.invoke.MethodHandles.lookup;

public class ScreenshotsComparator {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final RunStepConfig runStepConfig;
    private final JobConfig jobConfig;
    private final FileService fileService;
    private final ImageService imageService;

    public ScreenshotsComparator(RunStepConfig runStepConfig,
                                 JobConfig jobConfig,
                                 FileService fileService,
                                 ImageService imageService) {
        this.runStepConfig = RunStepConfig.copyOfBuilder(runStepConfig).withStep(Step.compare).build();
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
        List<ScreenshotContext> contextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);
        //final HashMap<Integer, ScreenshotContext> contextHashMap = new HashMap<>();
        //contextList.forEach(screenshotContext -> contextHashMap.put(screenshotContext.contextHash(), screenshotContext));

        for (Map.Entry<String, UrlConfig> urlConfigEntry : jobConfig.urls.entrySet()) {
            List<ScreenshotComparisonResult> screenshotComparisonResults = new ArrayList<>();
            for (ScreenshotContext screenshotContext : contextList) {
                if (!urlConfigEntry.getKey().equals(screenshotContext.url)) {
                    continue;
                }
                String fullUrlWithPath = BrowserUtils.buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping);
                Map<Integer, Map<Step, String>> screenshots = fileService.getFileTracker().getScreenshotsForContext(screenshotContext.contextHash());
                List<Integer> yPositions = new ArrayList<>(screenshots.keySet());

                for (Integer yPosition : yPositions) {
                    String beforeFileName = screenshots.get(yPosition).get(Step.before);
                    String afterFileName = screenshots.get(yPosition).get(Step.after);

                    LOG.debug("Comparing file '{}' with '{}'", beforeFileName, afterFileName);

                    boolean error = false;
                    BufferedImage imageBefore = null;
                    if (beforeFileName == null) {
                        error = true;
                    } else {
                        try {
                            imageBefore = fileService.readScreenshot(beforeFileName);
                        } catch (IIOException e) {
                            error = true;
                        }
                    }
                    if (error) {
                        screenshotComparisonResults.add(ScreenshotComparisonResult.noBeforeImageComparisonResult(screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, yPosition, buildRelativePathFromReportDir(afterFileName)));
                        continue;
                    }

                    BufferedImage imageAfter = null;
                    if (afterFileName == null) {
                        error = true;
                    } else {
                        try {
                            imageAfter = fileService.readScreenshot(afterFileName);
                        } catch (IIOException e) {
                            error = true;
                        }
                    }

                    if (error) {
                        screenshotComparisonResults.add(ScreenshotComparisonResult.noAfterImageComparisonResult(screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, yPosition, buildRelativePathFromReportDir(beforeFileName)));
                        continue;
                    }

                    ImageService.ImageComparisonResult imageComparisonResult = imageService.compareImages(imageBefore, imageAfter, jobConfig.windowHeight, screenshotContext.urlConfig.ignoreAntiAliasing, screenshotContext.urlConfig.strictColorComparison, screenshotContext.urlConfig.maxColorDistance);
                    String differenceImageFileName = null;
                    if ((imageComparisonResult.getDifference() > 0 || imageComparisonResult.getAcceptedDifferentPixels() > 0) && imageComparisonResult.getDifferenceImage().isPresent()) {
                        differenceImageFileName = fileService.writeScreenshot(screenshotContext, imageComparisonResult.getDifferenceImage().orElse(null), yPosition);
                    }
                    screenshotComparisonResults.add(new ScreenshotComparisonResult(
                            screenshotContext.contextHash(),
                            fullUrlWithPath,
                            screenshotContext.deviceConfig,
                            yPosition,
                            imageComparisonResult.getDifference(),
                            buildRelativePathFromReportDir(beforeFileName),
                            buildRelativePathFromReportDir(afterFileName),
                            buildRelativePathFromReportDir(differenceImageFileName),
                            imageComparisonResult.getAcceptedDifferentPixels()));
                }
            }
            screenshotComparisonResults.sort(Comparator.<ScreenshotComparisonResult, String>
                    comparing(r -> r.url)
                    .thenComparing(r -> r.deviceConfig.width)
                    .thenComparing(r -> r.deviceConfig.height)
                    .thenComparing(r -> r.deviceConfig.pixelRatio)
                    .thenComparing(r -> r.verticalScrollPosition));
            results.put(urlConfigEntry.getKey(), screenshotComparisonResults);
        }
        return results;
    }

    private String buildRelativePathFromReportDir(String imageFileName) {
        return imageFileName != null ? fileService.getRelativePathFromReportDirToScreenshotsDir() + imageFileName : null;
    }
}
