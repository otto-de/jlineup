package de.otto.jlineup.image;

import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Lossy image codecs (JPEG, WebP, AVIF) produce small, noisy pixel differences when the same source image is encoded
 * twice, i.e. by different CDN nodes. This noise averages out in a neighborhood, while real changes (text, shifts,
 * color changes) don't.
 * <p>
 * A different pixel is accepted as compression artifact if
 * <ul>
 *     <li>its own color distance is at most {@code maxPixelColorDistance} and</li>
 *     <li>either the pixel itself or the average colors of the surrounding (2 * radius + 1)² window are closer than
 *     maxColorDistance. With a {@code textureTolerance} greater than 0, the accepted distance of the averages grows
 *     with the local lightness standard deviation (busy image areas hide stronger noise).</li>
 * </ul>
 * The window statistics are only calculated if a different pixel actually needs them. They are kept as column sums
 * over the rows of the current window, which slide along with the compared pixels, so the memory needed is
 * proportional to the image width only. This state belongs to one pair of images, see {@link #forImages}.
 */
public class CompressionArtifactDetector {

    private static final double[] LINEAR_SRGB = new double[256];

    //Lightness values are rounded to this fixed-point scale, so the window sums stay exact (and cheap to update)
    private static final double LIGHTNESS_SCALE = 1000d;

    static {
        for (int i = 0; i < 256; i++) {
            double c = i / 255d;
            LINEAR_SRGB[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    private final int radius;
    private final double maxPixelColorDistance;
    private final double textureTolerance;

    /**
     * Creates a detector for one set of 'ignore-compression-artifacts' settings.
     *
     * @param radius                the radius of the square around a pixel whose average color is compared; a radius
     *                              of 20 means a 41x41 pixel square ('compression-artifact-radius'), at least 1
     * @param maxPixelColorDistance different pixels with a higher CIEDE2000 color distance are never accepted
     *                              ('max-compression-artifact-color-distance')
     * @param textureTolerance      how much the accepted color distance of the averages grows with the local lightness
     *                              standard deviation, 0 disables it ('compression-artifact-texture-tolerance')
     * @throws IllegalArgumentException if the radius is smaller than 1
     */
    public CompressionArtifactDetector(int radius, double maxPixelColorDistance, double textureTolerance) {
        if (radius < 1) throw new IllegalArgumentException("Compression artifact radius has to be at least 1, but was " + radius);
        this.radius = radius;
        this.maxPixelColorDistance = maxPixelColorDistance;
        this.textureTolerance = textureTolerance;
    }

    /**
     * Creates the state for the comparison of one pair of pixel arrays. The detector itself has no state, so it can
     * be shared, but a {@link Comparison} must only be used by one thread for exactly this pair of images.
     * Pixels should be asked in raster order, other orders work but are slower.
     */
    Comparison forImages(int[] image1Pixels, int[] image2Pixels, int width, int height) {
        return new Comparison(image1Pixels, image2Pixels, width, height);
    }

    final class Comparison {
        private final WindowSums windowSums1;
        private final WindowSums windowSums2;
        private final int width;
        private final int height;
        private final int windowSize = 2 * radius + 1;

        private Comparison(int[] image1Pixels, int[] image2Pixels, int width, int height) {
            this.width = width;
            this.height = height;
            this.windowSums1 = new WindowSums(image1Pixels, width, height);
            this.windowSums2 = new WindowSums(image2Pixels, width, height);
        }

        boolean isCompressionArtifact(int x, int y, double pixelColorDistance, double maxColorDistance) {
            if (pixelColorDistance > maxPixelColorDistance) {
                return false;
            }
            if (pixelColorDistance < maxColorDistance) {
                return true;
            }
            //At the image borders, the window is shifted inward instead of being cut off, so it always has the full
            //size (as long as the image is big enough). A cut off window averages too few pixels to cancel out the noise.
            int minX = max(0, min(x - radius, width - windowSize)), maxX = min(width - 1, minX + windowSize - 1);
            int minY = max(0, min(y - radius, height - windowSize)), maxY = min(height - 1, minY + windowSize - 1);
            windowSums1.calculate(minX, maxX, minY, maxY);
            windowSums2.calculate(minX, maxX, minY, maxY);
            double acceptedDistance = maxColorDistance + textureTolerance * min(windowSums1.lightnessStandardDeviation, windowSums2.lightnessStandardDeviation);
            return Math.abs(LAB.ciede2000(windowSums1.averageColor, windowSums2.averageColor)) < acceptedDistance;
        }
    }

    /**
     * Statistics of rectangular windows of one image. For the rows of the current window ("band"), there are sums per
     * column. Moving the band updates only the rows that enter or leave it, a window is the sum of its columns.
     */
    private final class WindowSums {
        private final int[] pixels;
        private final int width;
        private final int height;

        //Allocated with the first window
        private long[] red, green, blue, lightness, lightnessSquare;
        private int bandMinY = 0, bandMaxY = -1;

        //Results of the last calculation
        private LAB averageColor;
        private double lightnessStandardDeviation;

        private WindowSums(int[] pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }

        private void calculate(int minX, int maxX, int minY, int maxY) {
            moveBand(minY, maxY);
            long redSum = 0, greenSum = 0, blueSum = 0, lightnessSum = 0, lightnessSquareSum = 0;
            for (int xx = minX; xx <= maxX; xx++) {
                redSum += red[xx];
                greenSum += green[xx];
                blueSum += blue[xx];
                if (textureTolerance > 0) {
                    lightnessSum += lightness[xx];
                    lightnessSquareSum += lightnessSquare[xx];
                }
            }
            long count = (long) (maxX - minX + 1) * (maxY - minY + 1);
            //averages are not rounded to integer RGB values, rounding can push the distance over the threshold
            averageColor = LAB.fromRGB((double) redSum / count, (double) greenSum / count, (double) blueSum / count);
            lightnessStandardDeviation = textureTolerance > 0 ? standardDeviation(lightnessSum, lightnessSquareSum, count) : 0;
        }

        private double standardDeviation(long sum, long squareSum, long count) {
            //variance = (count * squareSum - sum²) / count², the numerator is exact as long as it fits into a long
            double numerator;
            if (count < 20000) {
                numerator = count * squareSum - sum * sum;
            } else {
                numerator = (double) count * squareSum - (double) sum * sum;
            }
            return Math.sqrt(max(0, numerator)) / count / LIGHTNESS_SCALE;
        }

        private void moveBand(int minY, int maxY) {
            if (red == null) {
                red = new long[width];
                green = new long[width];
                blue = new long[width];
                lightness = new long[width];
                lightnessSquare = new long[width];
            }
            if (bandMaxY >= bandMinY && minY == bandMinY && maxY == bandMaxY) {
                return;
            }
            int shift = Math.abs(minY - bandMinY);
            boolean overlapping = bandMaxY >= bandMinY && minY <= bandMaxY && maxY >= bandMinY;
            if (!overlapping || 2 * shift >= maxY - minY + 1) {
                Arrays.fill(red, 0);
                Arrays.fill(green, 0);
                Arrays.fill(blue, 0);
                Arrays.fill(lightness, 0);
                Arrays.fill(lightnessSquare, 0);
                for (int yy = minY; yy <= maxY; yy++) {
                    addRow(yy, 1);
                }
            } else {
                //leaving rows
                for (int yy = bandMinY; yy < minY; yy++) {
                    addRow(yy, -1);
                }
                for (int yy = bandMaxY; yy > maxY; yy--) {
                    addRow(yy, -1);
                }
                //entering rows
                for (int yy = max(minY, bandMaxY + 1); yy <= maxY; yy++) {
                    addRow(yy, 1);
                }
                for (int yy = minY; yy < min(bandMinY, maxY + 1); yy++) {
                    addRow(yy, 1);
                }
            }
            bandMinY = minY;
            bandMaxY = maxY;
        }

        private void addRow(int y, int sign) {
            int rowOffset = y * width;
            boolean withLightness = textureTolerance > 0;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[rowOffset + x];
                int r = (pixel >> 16) & 0xFF, g = (pixel >> 8) & 0xFF, b = pixel & 0xFF;
                red[x] += sign * r;
                green[x] += sign * g;
                blue[x] += sign * b;
                if (withLightness) {
                    long quantizedLightness = Math.round(lightness(r, g, b) * LIGHTNESS_SCALE);
                    lightness[x] += sign * quantizedLightness;
                    lightnessSquare[x] += sign * quantizedLightness * quantizedLightness;
                }
            }
        }
    }

    //CIE L* (0-100) of an sRGB color
    private static double lightness(int r, int g, int b) {
        double luminance = 0.2126 * LINEAR_SRGB[r] + 0.7152 * LINEAR_SRGB[g] + 0.0722 * LINEAR_SRGB[b];
        double f = luminance > 0.008856 ? Math.cbrt(luminance) : 7.787 * luminance + 16d / 116;
        return 116 * f - 16;
    }

    @Override
    public String toString() {
        return "CompressionArtifactDetector{radius=" + radius + ", maxPixelColorDistance=" + maxPixelColorDistance + ", textureTolerance=" + textureTolerance + '}';
    }
}
