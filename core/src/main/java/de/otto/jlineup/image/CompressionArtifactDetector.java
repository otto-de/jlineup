package de.otto.jlineup.image;

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
 * The window averages are only calculated for different pixels, so no extra memory is needed for big screenshots.
 */
public class CompressionArtifactDetector {

    private static final double[] LINEAR_SRGB = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double c = i / 255d;
            LINEAR_SRGB[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    private final int radius;
    private final double maxPixelColorDistance;
    private final double textureTolerance;

    public CompressionArtifactDetector(int radius, double maxPixelColorDistance, double textureTolerance) {
        if (radius < 1) throw new IllegalArgumentException("Compression artifact radius has to be at least 1, but was " + radius);
        this.radius = radius;
        this.maxPixelColorDistance = maxPixelColorDistance;
        this.textureTolerance = textureTolerance;
    }

    boolean isCompressionArtifact(int[] image1Pixels, int[] image2Pixels, int width, int height, int x, int y, double pixelColorDistance, double maxColorDistance) {
        if (pixelColorDistance > maxPixelColorDistance) {
            return false;
        }
        if (pixelColorDistance < maxColorDistance) {
            return true;
        }
        WindowStatistics window1 = windowStatistics(image1Pixels, width, height, x, y);
        WindowStatistics window2 = windowStatistics(image2Pixels, width, height, x, y);
        double acceptedDistance = maxColorDistance + textureTolerance * min(window1.lightnessStandardDeviation, window2.lightnessStandardDeviation);
        return Math.abs(LAB.ciede2000(window1.averageColor, window2.averageColor)) < acceptedDistance;
    }

    //At the image borders, the window is shifted inward instead of being cut off, so it always has the full size
    //(as long as the image is big enough). A cut off window averages too few pixels to cancel out the noise.
    private WindowStatistics windowStatistics(int[] pixels, int width, int height, int x, int y) {
        int windowSize = 2 * radius + 1;
        int minX = max(0, min(x - radius, width - windowSize)), maxX = min(width - 1, minX + windowSize - 1);
        int minY = max(0, min(y - radius, height - windowSize)), maxY = min(height - 1, minY + windowSize - 1);
        long red = 0, green = 0, blue = 0;
        double lightnessSum = 0, lightnessSquareSum = 0;
        for (int yy = minY; yy <= maxY; yy++) {
            int rowOffset = yy * width;
            for (int xx = minX; xx <= maxX; xx++) {
                int pixel = pixels[rowOffset + xx];
                int r = (pixel >> 16) & 0xFF, g = (pixel >> 8) & 0xFF, b = pixel & 0xFF;
                red += r;
                green += g;
                blue += b;
                if (textureTolerance > 0) {
                    double lightness = lightness(r, g, b);
                    lightnessSum += lightness;
                    lightnessSquareSum += lightness * lightness;
                }
            }
        }
        long count = (long) (maxX - minX + 1) * (maxY - minY + 1);
        //averages are not rounded to integer RGB values, rounding can push the distance over the threshold
        LAB averageColor = LAB.fromRGB((double) red / count, (double) green / count, (double) blue / count);
        double lightnessMean = lightnessSum / count;
        double lightnessStandardDeviation = Math.sqrt(max(0, lightnessSquareSum / count - lightnessMean * lightnessMean));
        return new WindowStatistics(averageColor, lightnessStandardDeviation);
    }

    //CIE L* (0-100) of an sRGB color
    private static double lightness(int r, int g, int b) {
        double luminance = 0.2126 * LINEAR_SRGB[r] + 0.7152 * LINEAR_SRGB[g] + 0.0722 * LINEAR_SRGB[b];
        double f = luminance > 0.008856 ? Math.cbrt(luminance) : 7.787 * luminance + 16d / 116;
        return 116 * f - 16;
    }

    private static final class WindowStatistics {
        private final LAB averageColor;
        private final double lightnessStandardDeviation;

        private WindowStatistics(LAB averageColor, double lightnessStandardDeviation) {
            this.averageColor = averageColor;
            this.lightnessStandardDeviation = lightnessStandardDeviation;
        }
    }

    @Override
    public String toString() {
        return "CompressionArtifactDetector{radius=" + radius + ", maxPixelColorDistance=" + maxPixelColorDistance + ", textureTolerance=" + textureTolerance + '}';
    }
}
