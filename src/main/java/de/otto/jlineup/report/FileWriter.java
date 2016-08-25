package de.otto.jlineup.report;

import de.otto.jlineup.image.ImageUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class FileWriter {

    protected void writeDifferenceFile(String differenceImageFileName, ImageUtils.BufferedImageComparisonResult bufferedImageComparisonResult) throws IOException {
        ImageIO.write(bufferedImageComparisonResult.getDifferenceImage().orElse(null), "png", new File(differenceImageFileName));
    }

}
