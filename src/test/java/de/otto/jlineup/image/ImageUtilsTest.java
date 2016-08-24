package de.otto.jlineup.image;

import de.otto.jlineup.browser.BrowserUtils;
import de.otto.jlineup.config.Parameters;
import org.junit.Test;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.io.Files.equal;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ImageUtilsTest {

    @Test
    public void shouldGenerateDifferenceImage() throws IOException {

        //given
        Parameters parameters = Mockito.mock(Parameters.class);
        when(parameters.getWorkingDirectory()).thenReturn("src/test/resources");
        when(parameters.getScreenshotDirectory()).thenReturn("screenshots");

        final String generatedDifferenceImagePath = BrowserUtils.getFullScreenshotFileNameWithPath(parameters, "url", "/", 1001, 2002, "DIFFERENCE");
        final String referenceDifferenceImagePath = BrowserUtils.getFullScreenshotFileNameWithPath(parameters, "url", "/", 1001, 2002, "DIFFERENCE_reference");

        final BufferedImage beforeImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_2002_before.png"));
        final BufferedImage afterImageBuffer = ImageIO.read(new File("src/test/resources/screenshots/url_root_1001_2002_after.png"));
        //when
        ImageUtils.BufferedImageComparisonResult result = ImageUtils.generateDifferenceImage(beforeImageBuffer, afterImageBuffer, 800);
        ImageIO.write(result.getDifferenceImage().orElse(null), "png", new File(generatedDifferenceImagePath));

        //then
        assertThat(equal(new File(generatedDifferenceImagePath), new File(referenceDifferenceImagePath)), is(true));
        assertThat(result.getDifference(), is(0.07005));

        Files.delete(Paths.get(generatedDifferenceImagePath));
    }



}