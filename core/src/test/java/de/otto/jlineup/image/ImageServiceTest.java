package de.otto.jlineup.image;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static de.otto.jlineup.config.JobConfig.DEFAULT_MAX_COLOR_DISTANCE;
import static de.otto.jlineup.image.ImageService.bufferedImagesEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class ImageServiceTest {

    ImageService testee;

    @Before
    public void setup() {
        testee = new ImageService();
    }

    @Test
    public void shouldGenerateDifferenceImage() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/http_url_root_ff3c40c_1001_02002_DIFFERENCE_reference.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight);

        //then
        assertThat(result.getDifference(), is(0.07005));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    public void shouldNotIgnoreSlightDifferencesInColorSpaceInStrictMode() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_DIFFERENCE.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, true, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getAcceptedDifferentPixels(), is(0));
        assertThat(result.getDifference(), greaterThan(0d));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    public void shouldIgnoreSlightDifferencesInColorSpaceInDefaultNonStrictMode() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.0));
        assertThat(result.getAcceptedDifferentPixels(), is(5));
    }

    @Test
    public void shouldIgnoreRoundedEdgeAntialiasDifference() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/shoppromo_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/shoppromo_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, false, DEFAULT_MAX_COLOR_DISTANCE);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    @Ignore("TODO: Fix this issue somehow :)")
    public void shouldIgnoreIdenticalLookingHeartIcon() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_like_heart_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, false, DEFAULT_MAX_COLOR_DISTANCE);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    public void shouldIgnoreSlightDifferenceInNordicStyleImagesWithAcceptedColorDistanceOf3() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/nordic_style_image_blur_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/nordic_style_image_blur_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, false, 3f);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    public void shouldIgnoreSlightDifferenceInComboImagesWithAcceptedColorDistanceOf3_6() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/testAdditionalCombos_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/testAdditionalCombos_after.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, false, 3.6f);
        //then
        assertThat(result.getDifference(), is(0.0));
    }

    @Test
    public void shouldIgnoreChangeInOttoLogoWhenAntiAliasingIsIgnored() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_diff2.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, true, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), is(0.0));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    public void shouldNotIgnoreChangeInOttoLogoWhenAntiAliasingIsNotIgnored() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/otto_logo_diff.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight, false, false, DEFAULT_MAX_COLOR_DISTANCE);

        //then
        assertThat(result.getDifference(), greaterThan(0d));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    public void shouldGenerateDifferenceImageFromScreenshotsWithDifferentSizes() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/ideaWide.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/ideaVertical.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/ideaDifferenceReference.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight);
        //ImageIO.write(result.getDifferenceImage().orElse(null), "png", new File("src/test/resources/screenshots/ideaDifferenceReferenceNew.png"));

        //then
        assertThat(result.getDifference(), is(0.5366469443663049));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    @Test
    public void shouldNotCrashBecauseOfDifferentHeights() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/less_height.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/more_height.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight);

        //then
        assertThat(result.getDifference(), is(0.475));
    }

    @Test
    public void shouldSeeIdenticalByteImageBuffersAsEqualWithQuickCompare() {
        final BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);
        final BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);

        boolean result = ImageService.bufferedImagesEqualQuick(image1, image2);

        assertThat(result, is(true));
    }

    @Test
    public void shouldSeeIdenticalIntImageBuffersAsEqualWithQuickCompare() {
        final BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        boolean result = ImageService.bufferedImagesEqualQuick(image1, image2);

        assertThat(result, is(true));
    }

    @Test
    public void shouldSeeDifferentImageBuffersAsDifferentWithQuickCompare() {
        final BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_INDEXED);
        final BufferedImage image2 = new BufferedImage(100, 101, BufferedImage.TYPE_BYTE_INDEXED);

        boolean result = ImageService.bufferedImagesEqualQuick(image1, image2);

        assertThat(result, is(false));
    }


}