package de.otto.jlineup.report;

import de.otto.jlineup.RunStepConfig;
import de.otto.jlineup.browser.BrowserStep;
import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.browser.ScreenshotContext;
import de.otto.jlineup.Utils;
import de.otto.jlineup.config.DeviceConfig;
import de.otto.jlineup.config.JobConfig;
import de.otto.jlineup.config.RunStep;
import de.otto.jlineup.config.UrlConfig;
import de.otto.jlineup.file.FileService;
import de.otto.jlineup.file.ScreenshotContextFileTracker;
import de.otto.jlineup.image.CompressionArtifactDetector;
import de.otto.jlineup.image.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import static java.lang.invoke.MethodHandles.lookup;

public class ScreenshotsComparator {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final int BYTES_PER_PIXEL_AND_TASK = 40;
    private static final ImageService.ImageComparisonResult IDENTICAL_IMAGES_RESULT = new ImageService.ImageComparisonResult(null, 0, 0, 0d);

    private final RunStepConfig runStepConfig;
    private final JobConfig jobConfig;
    private final FileService fileService;
    private final ImageService imageService;
    private final Semaphore compareSlots;

    public ScreenshotsComparator(RunStepConfig runStepConfig,
                                 JobConfig jobConfig,
                                 FileService fileService,
                                 ImageService imageService) {
        this(runStepConfig, jobConfig, fileService, imageService, null);
    }

    /**
     * @param compareSlots optional limit of screenshots that are compared at the same time, shared with other
     *                     comparators (i.e. runs in a web server). Null means no limit besides the thread count.
     */
    public ScreenshotsComparator(RunStepConfig runStepConfig,
                                 JobConfig jobConfig,
                                 FileService fileService,
                                 ImageService imageService,
                                 Semaphore compareSlots) {
        this.runStepConfig = RunStepConfig.copyOfBuilder(runStepConfig).withStep(RunStep.compare).build();
        this.jobConfig = jobConfig;
        this.fileService = fileService;
        this.imageService = imageService;
        this.compareSlots = compareSlots;
    }


    public Map<String, List<ScreenshotComparisonResult>> compare() throws IOException {
        LOG.debug("Comparing images...");
        if (jobConfig.urls == null) {
            LOG.debug("No urls configured, so no comparison.");
            return null;
        }
        List<ScreenshotContext> contextList = BrowserUtils.buildScreenshotContextListFromConfigAndState(runStepConfig, jobConfig);
        if (LOG.isDebugEnabled()) {
            if (fileService.getFileTracker().contexts != null) {
                LOG.debug("Comparator looking for {} context(s). File tracker has context hashes: {}", contextList.size(), fileService.getFileTracker().contexts.keySet());
            }
            for (ScreenshotContext ctx : contextList) {
                LOG.debug("  Expected context hash '{}': urlKey='{}', urlSubPath='{}', deviceConfig={}, browserType={}", ctx.contextHash(), ctx.urlKey, ctx.urlSubPath, ctx.deviceConfig, ctx.browserType);
            }
        }

        //The work items are collected before the first one is started, so nothing reads the file tracker while
        //the tasks add their results to it. Order: url key -> context -> y position.
        Map<String, List<ComparisonTask>> tasksByUrlKey = new LinkedHashMap<>();
        for (Map.Entry<String, UrlConfig> urlConfigEntry : jobConfig.urls.entrySet()) {
            List<ComparisonTask> tasks = new ArrayList<>();
            tasksByUrlKey.put(urlConfigEntry.getKey(), tasks);
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
                    Map<BrowserStep, String> screenshotFileNames = screenshots.get(yPosition);
                    tasks.add(new ComparisonTask(screenshotContext, fullUrlWithPath, yPosition,
                            screenshotFileNames.get(BrowserStep.before), screenshotFileNames.get(BrowserStep.after)));
                }
            }
        }

        List<ComparisonTask> allTasks = new ArrayList<>();
        tasksByUrlKey.values().forEach(allTasks::addAll);
        Map<ComparisonTask, ScreenshotComparisonResult> resultsByTask = runTasks(allTasks);

        Map<String, List<ScreenshotComparisonResult>> results = new HashMap<>();
        for (Map.Entry<String, List<ComparisonTask>> entry : tasksByUrlKey.entrySet()) {
            List<ScreenshotComparisonResult> screenshotComparisonResults = new ArrayList<>();
            for (ComparisonTask task : entry.getValue()) {
                screenshotComparisonResults.add(resultsByTask.get(task));
            }
            //The sort is stable and the results are in submission order, so results that are equal regarding the
            //sort keys (i.e. contexts that differ only in cookies) have a deterministic order
            screenshotComparisonResults.sort(Comparator.<ScreenshotComparisonResult, String>
                    comparing(r -> r.fullUrlWithPath())
                    .thenComparing(r -> r.deviceConfig().width)
                    .thenComparing(r -> r.deviceConfig().height)
                    .thenComparing(r -> r.deviceConfig().pixelRatio)
                    .thenComparing(r -> r.verticalScrollPosition()));
            results.put(entry.getKey(), screenshotComparisonResults);
        }
        return results;
    }

    /**
     * One screenshot (one context and y position) of the comparison. Never split a work item between threads:
     * writing the difference image adds an entry to the map of this y position in the file tracker, which is only safe
     * because no other task touches this map. Also, files.json must not be written while the tasks are running.
     */
    private record ComparisonTask(ScreenshotContext screenshotContext, String fullUrlWithPath, int yPosition,
                                  String beforeFileName, String afterFileName) {
    }

    private Map<ComparisonTask, ScreenshotComparisonResult> runTasks(List<ComparisonTask> tasks) throws IOException {
        Map<ComparisonTask, ScreenshotComparisonResult> results = new IdentityHashMap<>();
        if (tasks.isEmpty()) {
            return results;
        }
        int threads = calculateEffectiveThreads(jobConfig.compareThreads, Runtime.getRuntime().availableProcessors(), tasks.size(),
                estimateMaxPixelCount(tasks), Runtime.getRuntime().maxMemory());

        if (threads <= 1) {
            LOG.info("Comparing {} screenshot(s) in the current thread.", tasks.size());
            for (ComparisonTask task : tasks) {
                results.put(task, compareLimited(task));
            }
            return results;
        }

        final Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        final ExecutorService executor = Utils.createThreadPool(threads, "CompareThread", "compare " + tasks.size() + " screenshot(s)");
        List<Future<ScreenshotComparisonResult>> futures = new ArrayList<>(tasks.size());
        try {
            for (ComparisonTask task : tasks) {
                futures.add(executor.submit(() -> {
                    //Log lines of the worker threads belong to the log of this report
                    if (mdcContext != null) {
                        MDC.setContextMap(mdcContext);
                    }
                    try {
                        return compareLimited(task);
                    } finally {
                        MDC.clear();
                    }
                }));
            }
            for (int i = 0; i < tasks.size(); i++) {
                results.put(tasks.get(i), futures.get(i).get());
            }
        } catch (ExecutionException e) {
            throw unwrap(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while comparing screenshots", e);
        } finally {
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
        }
        return results;
    }

    private static IOException unwrap(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            return (IOException) cause;
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        throw new RuntimeException(cause);
    }

    private ScreenshotComparisonResult compareLimited(ComparisonTask task) throws IOException {
        if (compareSlots == null) {
            return compareScreenshot(task);
        }
        try {
            compareSlots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to compare screenshots", e);
        }
        try {
            return compareScreenshot(task);
        } finally {
            compareSlots.release();
        }
    }

    private static long estimateMaxPixelCount(List<ComparisonTask> tasks) {
        long maxPixelCount = 0;
        for (ComparisonTask task : tasks) {
            DeviceConfig deviceConfig = task.screenshotContext().deviceConfig;
            double pixelRatio = deviceConfig.pixelRatio > 0 ? deviceConfig.pixelRatio : 1;
            maxPixelCount = Math.max(maxPixelCount, (long) (deviceConfig.width * pixelRatio) * (long) (deviceConfig.height * pixelRatio));
        }
        return maxPixelCount;
    }

    /**
     * The number of threads is limited by the work items and by the memory: the comparison of one screenshot needs
     * about {@value #BYTES_PER_PIXEL_AND_TASK} bytes per pixel (two decoded images, two copies with alpha channel, two
     * pixel arrays and the difference image), so not more tasks than fit into half of the heap are run in parallel.
     *
     * @param configuredThreads '0' means one thread per CPU core
     */
    static int calculateEffectiveThreads(int configuredThreads, int availableProcessors, int taskCount, long maxPixelCount, long maxMemory) {
        int wantedThreads = configuredThreads > 0 ? configuredThreads : Math.max(1, availableProcessors);
        long bytesPerTask = Math.max(1, maxPixelCount) * BYTES_PER_PIXEL_AND_TASK;
        int threadsFittingIntoMemory = (int) Math.max(1, Math.min(Integer.MAX_VALUE, (maxMemory / 2) / bytesPerTask));
        int threads = Math.max(1, Math.min(wantedThreads, Math.min(taskCount, threadsFittingIntoMemory)));
        if (threads < wantedThreads) {
            LOG.info("Using {} thread(s) for the comparison (wanted: {}, screenshots: {}, limit by memory: {}).", threads, wantedThreads, taskCount, threadsFittingIntoMemory);
        }
        return threads;
    }

    private ScreenshotComparisonResult compareScreenshot(ComparisonTask task) throws IOException {
        ScreenshotContext screenshotContext = task.screenshotContext();
        String fullUrlWithPath = task.fullUrlWithPath();
        int yPosition = task.yPosition();
        String beforeFileName = task.beforeFileName();
        String afterFileName = task.afterFileName();

        LOG.debug("Comparing file '{}' with '{}'", beforeFileName, afterFileName);

        UrlConfig urlConfig = screenshotContext.urlConfig;

        //Identical files don't have to be decoded, which is the most expensive part for unchanged screenshots
        if (beforeFileName != null && afterFileName != null && fileService.areScreenshotFilesIdentical(beforeFileName, afterFileName)) {
            LOG.debug("Files '{}' and '{}' are identical.", beforeFileName, afterFileName);
            ImageContentComparison imageContentComparison = urlConfig.ignoreImageContent
                    ? compareImageContent(screenshotContext, yPosition, null, null, IDENTICAL_IMAGES_RESULT, null)
                    : ImageContentComparison.NONE;
            return buildResult(screenshotContext, fullUrlWithPath, yPosition, beforeFileName, afterFileName, IDENTICAL_IMAGES_RESULT, null, imageContentComparison);
        }

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
            return withImagelessScreenshots(ScreenshotComparisonResult.noBeforeImageComparisonResult(screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, yPosition, buildRelativePathFromReportDir(afterFileName)), screenshotContext, yPosition);
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
            return withImagelessScreenshots(ScreenshotComparisonResult
                    .noAfterImageComparisonResult(screenshotContext.contextHash(), fullUrlWithPath, screenshotContext.deviceConfig, yPosition, buildRelativePathFromReportDir(beforeFileName)), screenshotContext, yPosition);
        }

        CompressionArtifactDetector compressionArtifactDetector = urlConfig.ignoreCompressionArtifacts.enabled
                ? urlConfig.ignoreCompressionArtifacts.toDetector()
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
        return buildResult(screenshotContext, fullUrlWithPath, yPosition, beforeFileName, afterFileName, imageComparisonResult, differenceImageFileName, imageContentComparison);
    }

    private ScreenshotComparisonResult buildResult(ScreenshotContext screenshotContext, String fullUrlWithPath, int yPosition, String beforeFileName, String afterFileName,
                                                   ImageService.ImageComparisonResult imageComparisonResult, String differenceImageFileName, ImageContentComparison imageContentComparison) {
        return new ScreenshotComparisonResult(
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
                imageContentComparison.hint());
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
