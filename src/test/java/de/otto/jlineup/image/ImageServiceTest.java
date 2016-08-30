package de.otto.jlineup.image;

import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static de.otto.jlineup.image.ImageService.bufferedImagesEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_02002_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_02002_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_02002_DIFFERENCE_reference.png"));

        //when
        ImageService.ImageComparisonResult result = testee.compareImages(beforeImageBuffer, afterImageBuffer, viewportHeight);

        //then
        assertThat(result.getDifference(), is(0.07005));
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
        //new DifferenceFileWriter().writeDifferenceFile("src/test/resources/screenshots/ideaDifferenceReferenceNew.png", result);

        //then
        assertThat(result.getDifference(), is(0.5366469443663049));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

}