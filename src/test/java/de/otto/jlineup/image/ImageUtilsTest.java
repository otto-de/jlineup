package de.otto.jlineup.image;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ImageUtilsTest {

    @Test
    public void shouldGenerateDifferenceImage() throws IOException {
        //given
        final int viewportHeight = 800;
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_2002_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_2002_after.png"));
        final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_2002_DIFFERENCE_reference.png"));

        //when
        ImageUtils.BufferedImageComparisonResult result = ImageUtils.generateDifferenceImage(beforeImageBuffer, afterImageBuffer, viewportHeight);

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
        ImageUtils.BufferedImageComparisonResult result = ImageUtils.generateDifferenceImage(beforeImageBuffer, afterImageBuffer, viewportHeight);
        //new DifferenceFileWriter().writeDifferenceFile("src/test/resources/screenshots/ideaDifferenceReference.png", result);

        //then
        assertThat(result.getDifference(), is(0.8739169562370365));
        assertThat(bufferedImagesEqual(referenceImageBuffer, result.getDifferenceImage().orElse(null)), is(true));
    }

    //Helper function to compare two BufferedImage instances (BufferedImage doesn't override equals())
    private boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
            for (int x = 0; x < img1.getWidth(); x++) {
                for (int y = 0; y < img1.getHeight(); y++) {
                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

}