package de.otto.jlineup.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.util.Optional;

public class ImageUtils {

    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    public static final int SAME_COLOR = Color.BLACK.getRGB();
    public static final int HIGHLIGHT_COLOR = Color.WHITE.getRGB();
    public static final int DIFFERENT_SIZE_COLOR = Color.GRAY.getRGB();

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

        final int minWidth = Math.min(img1.getWidth(), img2.getWidth());
        final int minHeight = Math.min(img1.getHeight(), img2.getHeight());

        final int[] p1 = img1.getRGB(0, 0, w1, h1, null, 0, w1);
        final int[] p2 = img2.getRGB(0, 0, w2, h2, null, 0, w2);

        final int pixelCount1 = w1 * h1;
        final int pixelCount2 = w2 * h2;

        final int maxPixelCount = maxWidth * maxHeight;
        final int minPixelCount = Math.min(pixelCount1, pixelCount2);

        int currentCount = 0;

        final int[] diff = new int[maxPixelCount];

        // compare img1 to img2, pixel by pixel. If different, highlight difference image pixel
        for (int i1=0, i2=0, iD=0; iD < maxPixelCount;) {
            if (p1[i1] != p2[i2]) {
                diff[iD] = HIGHLIGHT_COLOR;
                currentCount++;
            } else {
                diff[iD] = SAME_COLOR;
            }

            i1++;
            i2++;
            iD++;

            if (w1 < w2 && i1 % minWidth == 0) {
                while(i2 % maxWidth != 0) {
                    i2++;
                    diff[iD] = DIFFERENT_SIZE_COLOR;
                    currentCount++;
                    iD++;
                }
            } else if (w2 < w1 && i2 % minWidth == 0) {
                while(i1 % maxWidth != 0) {
                    i1++;
                    diff[iD] = DIFFERENT_SIZE_COLOR;
                    currentCount++;
                    iD++;
                }
            }
            if (i1 > minPixelCount || i2 > minPixelCount) {
                while(iD < maxPixelCount) {
                    diff[iD] = DIFFERENT_SIZE_COLOR;
                    currentCount++;
                    iD++;
                }
            }
        }

        double difference = (1d * currentCount) / Math.min(maxPixelCount, maxWidth * viewportHeight);

        // save img1's pixels to a new BufferedImage, and return it...
        // (May require TYPE_INT_ARGB)
        final BufferedImage out = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, maxWidth, maxHeight, diff, 0, maxWidth);
        return new BufferedImageComparisonResult(out, difference);
    }
}
