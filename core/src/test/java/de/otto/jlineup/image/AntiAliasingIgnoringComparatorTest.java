package de.otto.jlineup.image;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class AntiAliasingIgnoringComparatorTest {

    @Test
    public void shouldFindAntiAliasedPixel() throws IOException {
        //given
        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_after.png"));
        //final BufferedImage referenceImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/cases/chrome_rounded_edges_DIFFERENCE_reference.png"));

        assertThat(AntiAliasingIgnoringComparator.checkIsAntialiased(beforeImageBuffer, afterImageBuffer, 180, 33), is(false));
        assertThat(AntiAliasingIgnoringComparator.checkIsAntialiased(beforeImageBuffer, afterImageBuffer, 181, 33), is(true));
        assertThat(AntiAliasingIgnoringComparator.checkIsAntialiased(beforeImageBuffer, afterImageBuffer, 182, 33), is(false));
    }

}