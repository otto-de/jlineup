package de.otto.jlineup.report;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.image.CompressionArtifactDetector;
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
        this.runStepConfig = RunStepConfig.copyOfBuilder(runStepConfig).withStep(RunStep.compare).build();
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
        if (LOG.isDebugEnabled()) {
            if (fileService.getFileTracker().contexts != null) {
                LOG.debug("Comparator looking for {} context(s). File tracker has context hashes: {}", contextList.size(), fileService.getFileTracker().contexts.keySet());
            }
            for (ScreenshotContext ctx : contextList) {
                LOG.debug("  Expected context hash '{}': urlKey='{}', urlSubPath='{}', deviceConfig={}, browserType={}", ctx.contextHash(), ctx.urlKey, ctx.urlSubPath, ctx.deviceConfig, ctx.browserType);
            }
        }
        //final HashMap<Integer, ScreenshotContext> contextHashMap = new HashMap<>();
        //contextList.forEach(screenshotContext -> contextHashMap.put(screenshotContext.contextHash(), screenshotContext));

        for (Map.Entry<String, UrlConfig> urlConfigEntry : jobConfig.urls.entrySet()) {
            List<ScreenshotComparisonResult> screenshotComparisonResults = new ArrayList<>();
            for (ScreenshotContext screenshotContext : contextList) {
                if (!urlConfigEntry.getKey().equals(screenshotContext.urlKey)) {
                    continue;
                }
                String fullUrlWithPath = BrowserUtils.buildUrl(screenshotContext.url, screenshotContext.urlSubPath, screenshotContext.urlConfig.envMapping);
                Map<Integer, Map<BrowserStep, String>> screenshots;
                try {
                    screenshots = fileService.getFileTracker().getScreenshotsForContext(screenshotContext.contextHash());
                } catch (IOException e) {
                    LOG.warn("No screenshots found for context {} ({} {} {}). Skipping.", screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, screenshotContext.browserType);
                    continue;
                }
                List<Integer> yPositions = new ArrayList<>(screenshots.keySet());

                for (Integer yPosition : yPositions) {
                    String beforeFileName = screenshots.get(yPosition).get(BrowserStep.before);
                    String afterFileName = screenshots.get(yPosition).get(BrowserStep.after);

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
                        screenshotComparisonResults.add(withImagelessScreenshots(ScreenshotComparisonResult.noBeforeImageComparisonResult(screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, yPosition, buildRelativePathFromReportDir(afterFileName)), screenshotContext, yPosition));
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
                        screenshotComparisonResults.add(withImagelessScreenshots(ScreenshotComparisonResult
                                .noAfterImageComparisonResult(screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, yPosition, buildRelativePathFromReportDir(beforeFileName)), screenshotContext, yPosition));
                        continue;
                    }

                    UrlConfig urlConfig = screenshotContext.urlConfig;
                    CompressionArtifactDetector compressionArtifactDetector = urlConfig.ignoreCompressionArtifacts
                            ? new CompressionArtifactDetector(urlConfig.compressionArtifactRadius, urlConfig.maxCompressionArtifactColorDistance, urlConfig.compressionArtifactTextureTolerance)
                            : null;
                    ImageService.ImageComparisonResult imageComparisonResult = imageService
                            .compareImages(imageBefore, imageAfter, screenshotContext.deviceConfig.height, urlConfig.ignoreAntiAliasing, urlConfig.maxAntiAliasColorDistance, urlConfig.strictColorComparison, urlConfig.maxColorDistance, compressionArtifactDetector);

                    ImageContentComparison imageContentComparison = ImageContentComparison.NONE;
                    if (urlConfig.ignoreImageContent) {
                        imageContentComparison = compareImageContent(screenshotContext, yPosition, imageBefore, imageAfter, imageComparisonResult, compressionArtifactDetector);
                        if (imageContentComparison.result() != null) {
                            imageComparisonResult = imageContentComparison.result();
                        }
                    }

                    String differenceImageFileName = null;
                    if (imageComparisonResult.getAcceptedDifferentPixels() > 0 && imageComparisonResult.getDifferenceImage().isEmpty()) {
                        LOG.warn("Accepted different pixels but no difference image available. This should not happen.");
                    }
                    if ((imageComparisonResult.getDifference() > 0 || imageComparisonResult.getAcceptedDifferentPixels() > 0) && imageComparisonResult.getDifferenceImage().isPresent()) {
                        differenceImageFileName = fileService.writeScreenshot(screenshotContext, imageComparisonResult.getDifferenceImage().orElse(null), yPosition);
                    }
                    screenshotComparisonResults.add(new ScreenshotComparisonResult(
                            screenshotContext.contextHash(),
                            fullUrlWithPath,
                            screenshotContext.deviceConfig,
                            yPosition,
                            imageComparisonResult.getDifference(),
                            imageComparisonResult.getMaxDetectedColorDistance(),
                            buildRelativePathFromReportDir(beforeFileName),
                            buildRelativePathFromReportDir(afterFileName),
                            buildRelativePathFromReportDir(differenceImageFileName),
                            imageComparisonResult.getAcceptedDifferentPixels(),
                            buildRelativePathFromReportDir(imageContentComparison.imagelessBeforeFileName()),
                            buildRelativePathFromReportDir(imageContentComparison.imagelessAfterFileName()),
                            buildRelativePathFromReportDir(imageContentComparison.imagelessDifferenceImageFileName()),
                            imageContentComparison.hint()));
                }
            }
            screenshotComparisonResults.sort(Comparator.<ScreenshotComparisonResult, String>
                    comparing(r -> r.fullUrlWithPath())
                    .thenComparing(r -> r.deviceConfig().width)
                    .thenComparing(r -> r.deviceConfig().height)
                    .thenComparing(r -> r.deviceConfig().pixelRatio)
                    .thenComparing(r -> r.verticalScrollPosition()));
            results.put(urlConfigEntry.getKey(), screenshotComparisonResults);
        }
        return results;
    }

    private String buildRelativePathFromReportDir(String imageFileName) {
        return imageFileName != null ? fileService.getRelativePathFromReportDirToScreenshotsDir() + imageFileName : null;
    }

    /**
     * Adds the screenshots without images (if there are any) to a result that has only a before or only an after
     * screenshot, i.e. in the report of the before step.
     */
    private ScreenshotComparisonResult withImagelessScreenshots(ScreenshotComparisonResult result, ScreenshotContext screenshotContext, int yPosition) {
        if (!screenshotContext.urlConfig.ignoreImageContent) {
            return result;
        }
        ScreenshotContextFileTracker contextFileTracker = fileService.getRecordedContextFileTracker(screenshotContext.contextHash());
        Map<BrowserStep, String> imagelessScreenshots = contextFileTracker != null && contextFileTracker.imagelessScreenshots != null ? contextFileTracker.imagelessScreenshots.get(yPosition) : null;
        if (imagelessScreenshots == null) {
            return result;
        }
        String imagelessBefore = result.screenshotBeforeFileName() != null ? imagelessScreenshots.get(BrowserStep.before) : null;
        String imagelessAfter = result.screenshotAfterFileName() != null ? imagelessScreenshots.get(BrowserStep.after) : null;
        return new ScreenshotComparisonResult(result.contextHash(), result.fullUrlWithPath(), result.deviceConfig(), result.verticalScrollPosition(),
                result.difference(), result.maxDetectedColorDifference(), result.screenshotBeforeFileName(), result.screenshotAfterFileName(),
                result.differenceImageFileName(), result.acceptedDifferentPixels(),
                buildRelativePathFromReportDir(imagelessBefore), buildRelativePathFromReportDir(imagelessAfter), null, null);
    }

    /**
     * @param result the comparison result with accepted image content, null if the regular result stays
     */
    private record ImageContentComparison(ImageService.ImageComparisonResult result, String imagelessBeforeFileName,
                                          String imagelessAfterFileName, String imagelessDifferenceImageFileName,
                                          ImageContentHint hint) {
        static final ImageContentComparison NONE = new ImageContentComparison(null, null, null, null, null);
    }

    /**
     * 'ignore-image-content': differences are accepted only where both runs paint image pixels, if the screenshots
     * without images are identical (so layout, text and overlays didn't change) and the page has the same image URLs.
     * The result can only contain fewer differences than the regular one, never more.
     */
    private ImageContentComparison compareImageContent(ScreenshotContext screenshotContext, int yPosition, BufferedImage imageBefore, BufferedImage imageAfter,
                                                       ImageService.ImageComparisonResult regularResult, CompressionArtifactDetector compressionArtifactDetector) throws IOException {
        ScreenshotContextFileTracker contextFileTracker = fileService.getRecordedContextFileTracker(screenshotContext.contextHash());
        if (contextFileTracker == null) {
            return ImageContentComparison.NONE;
        }
        Map<BrowserStep, String> imagelessScreenshots = contextFileTracker.imagelessScreenshots != null ? contextFileTracker.imagelessScreenshots.get(yPosition) : null;
        String imagelessBeforeFileName = imagelessScreenshots != null ? imagelessScreenshots.get(BrowserStep.before) : null;
        String imagelessAfterFileName = imagelessScreenshots != null ? imagelessScreenshots.get(BrowserStep.after) : null;

        if (regularResult.getDifference() == 0) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, null, null);
        }

        if (imagelessBeforeFileName == null || imagelessAfterFileName == null) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, null, ImageContentHint.NO_IMAGE_DATA);
        }

        BufferedImage imagelessBefore;
        BufferedImage imagelessAfter;
        try {
            imagelessBefore = fileService.readScreenshot(imagelessBeforeFileName);
            imagelessAfter = fileService.readScreenshot(imagelessAfterFileName);
        } catch (IIOException e) {
            LOG.warn("Could not read screenshots without images '{}' and '{}'.", imagelessBeforeFileName, imagelessAfterFileName);
            return new ImageContentComparison(null, null, null, null, ImageContentHint.NO_IMAGE_DATA);
        }
        if (imagelessBefore == null || imagelessAfter == null) {
            return new ImageContentComparison(null, null, null, null, ImageContentHint.NO_IMAGE_DATA);
        }

        Map<BrowserStep, List<int[]>> imageAreas = contextFileTracker.imageAreas != null ? contextFileTracker.imageAreas.get(yPosition) : null;
        List<int[]> imageAreasBefore = imageAreas != null ? imageAreas.get(BrowserStep.before) : null;
        List<int[]> imageAreasAfter = imageAreas != null ? imageAreas.get(BrowserStep.after) : null;

        //The screenshots without images are always compared, so the report can show what changed outside of the images,
        //also if differences inside of images can't be accepted anyway (i.e. because image URLs changed)
        UrlConfig urlConfig = screenshotContext.urlConfig;
        ImageService.ImageComparisonResult imagelessResult = imageService.compareImages(imagelessBefore, imagelessAfter, screenshotContext.deviceConfig.height,
                urlConfig.ignoreAntiAliasing, urlConfig.maxAntiAliasColorDistance, urlConfig.strictColorComparison, urlConfig.maxColorDistance, compressionArtifactDetector);
        boolean layoutChanged = imagelessResult.getDifference() > 0;
        if (!layoutChanged && imageAreasBefore != null && imageAreasAfter != null
                && !ImageService.equalInAreas(imagelessBefore, imagelessAfter, imageAreasBefore, imageAreasAfter)) {
            //Anything on top of the checkerboard has to be exactly the same, see ImageService.equalInAreas
            imagelessResult = imageService.compareImages(imagelessBefore, imagelessAfter, screenshotContext.deviceConfig.height,
                    false, urlConfig.maxAntiAliasColorDistance, true, urlConfig.maxColorDistance);
            layoutChanged = true;
        }
        String imagelessDifferenceImageFileName = layoutChanged && imagelessResult.getDifferenceImage().isPresent()
                ? fileService.writeImagelessScreenshot(screenshotContext, imagelessResult.getDifferenceImage().get(), yPosition)
                : null;

        Set<String> imageUrlsBefore = contextFileTracker.imageUrls != null ? contextFileTracker.imageUrls.get(BrowserStep.before) : null;
        Set<String> imageUrlsAfter = contextFileTracker.imageUrls != null ? contextFileTracker.imageUrls.get(BrowserStep.after) : null;
        if (imageUrlsBefore == null || imageUrlsAfter == null) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, imagelessDifferenceImageFileName, ImageContentHint.NO_IMAGE_DATA);
        }
        if (!imageUrlsBefore.equals(imageUrlsAfter)) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, imagelessDifferenceImageFileName, ImageContentHint.IMAGE_URLS_CHANGED);
        }
        if (layoutChanged) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, imagelessDifferenceImageFileName, ImageContentHint.LAYOUT_CHANGED);
        }
        if (imageAreasBefore == null || imageAreasAfter == null) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, null, ImageContentHint.NO_IMAGE_DATA);
        }

        boolean[] imageContentMask = ImageService.buildImageContentMask(imageBefore, imagelessBefore, imageAfter, imagelessAfter, imageAreasBefore, imageAreasAfter);
        if (imageContentMask == null) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, null, ImageContentHint.NO_IMAGE_DATA);
        }
        ImageService.ImageComparisonResult maskedResult = imageService.compareImages(imageBefore, imageAfter, screenshotContext.deviceConfig.height,
                urlConfig.ignoreAntiAliasing, urlConfig.maxAntiAliasColorDistance, urlConfig.strictColorComparison, urlConfig.maxColorDistance, compressionArtifactDetector, imageContentMask);
        if (maskedResult.getDifference() >= regularResult.getDifference()) {
            return new ImageContentComparison(null, imagelessBeforeFileName, imagelessAfterFileName, null, null);
        }
        return new ImageContentComparison(maskedResult, imagelessBeforeFileName, imagelessAfterFileName, null, ImageContentHint.IMAGE_CONTENT_ACCEPTED);
    }
}
