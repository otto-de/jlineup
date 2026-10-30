package de.otto.jlineup.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE;
import static de.otto.jlineup.config.JobConfig.DEFAULT_COMPRESSION_ARTIFACT_RADIUS;
import static de.otto.jlineup.config.JobConfig.DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE;
import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_COLOR_DISTANCE;
import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE;
import static de.otto.jlineup.image.ImageService.bufferedImagesEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

class ImageServiceTest {

    ImageService testee;

    @BeforeEach
    void setup() {
        testee = new ImageService();
    }

    @Test
    void shouldGenerateDifferenceImage() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_DIFFERENCE_reference.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, true, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.07005));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    void shouldNotIgnoreSlightDifferencesInColorSpaceInStrictMode() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_DIFFERENCE.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, true, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getAcceptedDifferentPixels(), is(0));
        assertThat(result.getDifference(), greaterThan(0d));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    void shouldIgnoreSlightDifferencesInColorSpaceInDefaultNonStrictMode() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.0));
        assertThat(result.getAcceptedDifferentPixels(), is(5));
    }

    @Test
    void shouldIgnoreSlightDifferenceInBenefitImageThroughAntiAliasDetection() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/benefit_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/benefit_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, 2.3d, true, 0);

        //then
        assertThat(result.getDifference(), is(0.0));
        assertThat(result.getAcceptedDifferentPixels(), is(1));
    }

    @Test
    void shouldIgnoreRoundedEdgeAntialiasDifference() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/shoppromo_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/shoppromo_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldIgnoreIdenticalLookingHeartIconWhenIncreasedMaxColorDistanceIsUsed() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, 4.68f);

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldIgnoreIdenticalLookingHeartIconWhenIncreasedMaxAntiAliasColorDistanceIsUsed() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, 20f, true, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldNotIgnoreIdenticalLookingHeartIconWhenDefaultMaxColorDistanceIsUsed() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(7.8125E-6));
    }

    @Test
    void shouldIgnoreSlightDifferenceInNordicStyleImagesWithAcceptedColorDistanceOf3() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/nordic_style_image_blur_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/nordic_style_image_blur_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, 3f);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldIgnoreSlightDifferenceInComboImagesWithAcceptedColorDistanceOf3_6() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/testAdditionalCombos_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/testAdditionalCombos_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, 3.6f);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldIgnoreChangeInOttoLogoWhenAntiAliasingIsIgnored() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_diff3.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.0));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    void shouldNotIgnoreChangeInOttoLogoWhenAntiAliasingIsNotIgnored() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_diff2.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    void shouldGenerateDifferenceImageFromScreenshotsWithDifferentSizes() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/ideaWide.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/ideaVertical.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/ideaDifferenceReference.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, true, DEFAULT_MAX_COLOR_DISTANCE);
        //ImageIO.write(result.getDifferenceImage().orElse(null), "png", new File("src/test/resources/screenshots/ideaDifferenceReferenceNew.png"));

        //then
        assertThat(result.getDifference(), is(0.5366469443663049));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    void shouldNotCrashBecauseOfDifferentHeights() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/less_height.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/more_height.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, true, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.475));
    }

    @Test
    void shouldSeeIdenticalByteImageBuffersAsEqualWithQuickCompare() {
        final BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);
        final BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);

        boolean result = ImageService.bufferedImagesEqualQuick(image1, image2);

        assertThat(result, is(true));
    }

    @Test
    void shouldSeeIdenticalIntImageBuffersAsEqualWithQuickCompare() {
        final BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        boolean result = ImageService.bufferedImagesEqualQuick(image1, image2);

        assertThat(result, is(true));
    }

    @Test
    void shouldSeeDifferentImageBuffersAsDifferentWithQuickCompare() {
        final BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);
        final BufferedImage image2 = new BufferedImage(100, 101, BufferedImage.TYPE_BYTE_INDEXED);

        boolean result = ImageService.bufferedImagesEqualQuick(image1, image2);

        assertThat(result, is(false));
    }

    //The same app promo teaser image, encoded once by the Cloudflare and once by the Akamai CDN of the image provider,
    //scaled to the rendered size of 347x260 pixels
    private static final String CDN_COMPRESSION_BEFORE = "src/test/resources/screenshots/cases/app_promo_cdn_compression_before.png";
    private static final String CDN_COMPRESSION_AFTER = "src/test/resources/screenshots/cases/app_promo_cdn_compression_after.png";

    private static final CompressionArtifactDetector DEFAULT_COMPRESSION_ARTIFACT_DETECTOR = new CompressionArtifactDetector(DEFAULT_COMPRESSION_ARTIFACT_RADIUS, DEFAULT_MAX_COMPRESSION_ARTIFACT_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_TEXTURE_TOLERANCE);

    @Test
    void shouldNotIgnoreCdnCompressionArtifactsByDefault() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifactsWhenEnabled() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), is(0.0));
        assertThat(result.getAcceptedDifferentPixels(), greaterThan(0));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifactsWhenEnabledInStrictModeTogetherWithAntiAliasDetection() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, true, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldDetectAddedTextDespiteIgnoredCompressionArtifacts() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_AFTER));
        Graphics2D graphics = afterImageBuffer.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.drawString("neu", 60, 220);
        graphics.dispose();

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
    }

    @Test
    void shouldDetectShiftedContentDespiteIgnoredCompressionArtifacts() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_AFTER));
        final BufferedImage shiftedAfterImageBuffer = new BufferedImage(afterImageBuffer.getWidth(), afterImageBuffer.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = shiftedAfterImageBuffer.createGraphics();
        graphics.drawImage(afterImageBuffer, 0, 0, null);
        graphics.drawImage(afterImageBuffer, 2, 0, null);
        graphics.dispose();

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, shiftedAfterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
    }

    @Test
    void shouldDetectSlightColorChangeDespiteIgnoredCompressionArtifacts() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_AFTER));
        for (int y = 0; y < afterImageBuffer.getHeight(); y++) {
            for (int x = 0; x < afterImageBuffer.getWidth(); x++) {
                int rgb = afterImageBuffer.getRGB(x, y);
                afterImageBuffer.setRGB(x, y, (rgb & 0xFFFFFF00) | Math.min(255, (rgb & 0xFF) + 12));
            }
        }

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), greaterThan(0.5d));
    }

    //Photo teaser with text, also encoded once by Cloudflare and once by Akamai (708x176, rendered unscaled at 48em-62em)
    private static final String CDN_COMPRESSION_PHOTO_BEFORE = "src/test/resources/screenshots/cases/kamp_20_cdn_compression_before.png";
    private static final String CDN_COMPRESSION_PHOTO_AFTER = "src/test/resources/screenshots/cases/kamp_20_cdn_compression_after.png";

    @Test
    void shouldNotIgnoreCdnCompressionArtifactsInPhotoByDefault() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), greaterThan(0.05d));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifactsInPhotoWithDefaultSettings() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldNotIgnoreAllCdnCompressionArtifactsInPhotoWithSmallRadius() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_AFTER));

        //when
        //compression noise in textured photo areas doesn't average out completely in a small window
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, new CompressionArtifactDetector(3, 10, 0));

        //then
        assertThat(result.getDifference(), greaterThan(0d));
        assertThat(result.getDifference(), lessThan(0.01d));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifactsInPhotoWithSmallerRadiusAndTextureTolerance() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, new CompressionArtifactDetector(7, 12, 0.15));

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldDetectAddedTextInPhotoTeaserDespiteIgnoredCompressionArtifacts() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_AFTER));
        Graphics2D graphics = afterImageBuffer.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.drawString("neu", 60, 100);
        graphics.dispose();

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
    }

    @Test
    void shouldDetectChangedDigitInPhotoTeaserDespiteIgnoredCompressionArtifacts() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_PHOTO_AFTER));
        //paint over a part of the white "2" in "20%" with the teaser's red
        Graphics2D graphics = afterImageBuffer.createGraphics();
        graphics.setColor(new Color(225, 0, 25));
        graphics.fillRect(490, 30, 41, 31);
        graphics.dispose();

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), greaterThan(0.005d));
    }

    //Crop of a real JLineup report (mobile device with pixel ratio 2): the app promo teaser, delivered by different CDNs
    private static final String CDN_COMPRESSION_REPORT_BEFORE = "src/test/resources/screenshots/cases/app_promo_mobile_report_before.png";
    private static final String CDN_COMPRESSION_REPORT_AFTER = "src/test/resources/screenshots/cases/app_promo_mobile_report_after.png";

    @Test
    void shouldNotIgnoreCdnCompressionArtifactsInReportScreenshotByDefault() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_REPORT_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_REPORT_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifactsInReportScreenshotWithDefaultSettings() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_REPORT_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_REPORT_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    //Crop of a real JLineup report: a very busy photo, which needs a bigger window to average out the compression noise
    private static final String CDN_COMPRESSION_BUSY_PHOTO_BEFORE = "src/test/resources/screenshots/cases/flower_photo_report_before.png";
    private static final String CDN_COMPRESSION_BUSY_PHOTO_AFTER = "src/test/resources/screenshots/cases/flower_photo_report_after.png";

    @Test
    void shouldIgnoreCdnCompressionArtifactsInBusyPhotoWithDefaultSettings() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BUSY_PHOTO_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BUSY_PHOTO_AFTER));

        //when
        ImageService.ImageComparisonResult defaultResult = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE);
        ImageService.ImageComparisonResult smallWindowResult = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, new CompressionArtifactDetector(12, 12, 0));
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(defaultResult.getDifference(), greaterThan(0d));
        assertThat(smallWindowResult.getDifference(), greaterThan(0d));
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    void shouldIgnoreCdnCompressionArtifactsAtTheImageBorder() throws IOException {
        //given
        //remove the 40px margins, so the noisy photo touches the image borders, like at the edge of a screenshot
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BUSY_PHOTO_BEFORE)).getSubimage(40, 40, 400, 400);
        final BufferedImage afterImageBuffer = ImageIO.read(new File(CDN_COMPRESSION_BUSY_PHOTO_AFTER)).getSubimage(40, 40, 400, 400);

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), is(0.0));
    }

    //Crop of a real JLineup report: the close icon of a sheet is missing in the before screenshot (a real difference)
    private static final String MISSING_CLOSE_ICON_BEFORE = "src/test/resources/screenshots/cases/missing_close_icon_report_before.png";
    private static final String MISSING_CLOSE_ICON_AFTER = "src/test/resources/screenshots/cases/missing_close_icon_report_after.png";

    @Test
    void shouldDetectMissingIconDespiteIgnoredCompressionArtifacts() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File(MISSING_CLOSE_ICON_BEFORE));
        final BufferedImage afterImageBuffer = ImageIO.read(new File(MISSING_CLOSE_ICON_AFTER));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, false, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);
        ImageService.ImageComparisonResult resultWithAntiAliasDetection = testee.compareImages(beforeImageBuffer, afterImageBuffer, 800, true, DEFAULT_MAX_ANTI_ALIAS_COLOR_DISTANCE, false, DEFAULT_MAX_COLOR_DISTANCE, DEFAULT_COMPRESSION_ARTIFACT_DETECTOR);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
        assertThat(resultWithAntiAliasDetection.getDifference(), greaterThan(0d));
    }

}