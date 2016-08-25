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
        final int w = img1.getWidth() > img2.getWidth() ? img2.getWidth() : img1.getWidth();
        final int h = img1.getHeight() > img2.getHeight() ? img2.getHeight() : img1.getHeight();

        final int maxWidth = Math.max(img1.getWidth(), img2.getWidth());
        final int maxHeight = Math.max(img1.getHeight(), img2.getHeight());

        final int highlight = Color.WHITE.getRGB();
        final int[] p1 = img1.getRGB(0, 0, w, h, null, 0, w);
        final int[] p2 = img2.getRGB(0, 0, w, h, null, 0, w);

        final int pixelCount = w * h;
        int currentCount = 0;

        // compare img1 to img2, pixel by pixel. If different, highlight img1's pixel...
        for (int i = 0; i < p1.length; i++) {
            if (p1[i] != p2[i]) {
                p1[i] = highlight;
                currentCount++;
            } else {
                p1[i] = Color.BLACK.getRGB();
            }
        }

        double difference = 1d * currentCount / Math.min(pixelCount, w * viewportHeight);

        // save img1's pixels to a new BufferedImage, and return it...
        // (May require TYPE_INT_ARGB)
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, p1, 0, w);
        return new BufferedImageComparisonResult(out, difference);
    }
}
