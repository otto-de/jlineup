package de.otto.jlineup.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

public class ImageUtils {

    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    /**
     * @param viewportHeight is needed to calculate the difference level
     * @return a double between 0 and 1 that measures the difference between the two pictures. 1 means 100% difference,
     * 0 means, that both pictures are identical
     * @throws IOException
     */
    public static BufferedImageComparisonResult generateDifferenceImage(BufferedImage imageBefore, BufferedImage imageAfter, int viewportHeight) throws IOException {
        final BufferedImageComparisonResult comparisonResult = getDifferenceImage(imageBefore, imageAfter, viewportHeight);
        return comparisonResult;
    }

    public static class BufferedImageComparisonResult {
        private final BufferedImage differenceImage;
        private final double difference;

        BufferedImageComparisonResult(BufferedImage differenceImage, double difference) {
            this.differenceImage = differenceImage;
            this.difference = difference;
        }

        public Optional<BufferedImage> getDifferenceImage() {
            return Optional.ofNullable(differenceImage);
        }

        public double getDifference() {
            return difference;
        }
    }

    private static BufferedImageComparisonResult getDifferenceImage(BufferedImage img1, BufferedImage img2, int viewportHeight) {
        // convert images to pixel arrays...
        final int w1 = img1.getWidth();
        final int h1 = img1.getHeight();
        final int w2 = img2.getWidth();
        final int h2 = img2.getHeight();

        final int maxWidth = Math.max(img1.getWidth(), img2.getWidth());
        final int maxHeight = Math.max(img1.getHeight(), img2.getHeight());

        final int highlight = Color.WHITE.getRGB();
        final int[] p1 = img1.getRGB(0, 0, w1, h1, null, 0, w1);
        final int[] p2 = img2.getRGB(0, 0, w2, h2, null, 0, w2);

        final int pixelCount1 = w1 * h1;
        final int pixelCount2 = w2 * h2;

        final int maxPixelCount = Math.max(pixelCount1, pixelCount2);

        int currentCount = 0;

        final int[] diff = new int[Math.max(pixelCount1, pixelCount2)];

        // compare img1 to img2, pixel by pixel. If different, highlight img1's pixel...
        for (int i = 0; i < p1.length; i++) {
            if (p1[i] != p2[i]) {
                diff[i] = highlight;
                currentCount++;
            } else {
                diff[i] = Color.BLACK.getRGB();
            }
        }

        double difference = 1d * currentCount / Math.min(maxPixelCount, maxWidth * viewportHeight);

        // save img1's pixels to a new BufferedImage, and return it...
        // (May require TYPE_INT_ARGB)
        final BufferedImage out = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, maxWidth, maxHeight, diff, 0, maxWidth);
        return new BufferedImageComparisonResult(out, difference);
    }
}
