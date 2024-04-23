package de.otto.jlineup.image;

import de.otto.jlineup.config.JobConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Optional;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ImageService {

    public static final int SAME_COLOR = Color.BLACK.getRGB();
    public static final int HIGHLIGHT_COLOR = Color.WHITE.getRGB();
    public static final int DIFFERENT_SIZE_COLOR = Color.GRAY.getRGB();
    public static final int LOOK_SAME_COLOR = Color.BLUE.getRGB();
    public static final int ANTI_ALIAS_DETECTED_COLOR = Color.GREEN.getRGB();

    public static class ImageComparisonResult {
        private final BufferedImage differenceImage;
        private final double difference;
        private final int acceptedDifferentPixels;

        public ImageComparisonResult(BufferedImage differenceImage, double difference, int acceptedDifferentPixels) {
            this.differenceImage = differenceImage;
            this.difference = difference;
            this.acceptedDifferentPixels = acceptedDifferentPixels;
        }

        public Optional<BufferedImage> getDifferenceImage() {
            return Optional.ofNullable(differenceImage);
        }

        public double getDifference() {
            return difference;
        }

        public int getAcceptedDifferentPixels() {
            return acceptedDifferentPixels;
        }
    }

    public ImageComparisonResult compareImages(BufferedImage image1, BufferedImage image2, int viewportHeight) {
        return this.compareImages(image1, image2, viewportHeight, false, true, JobConfig.DEFAULT_MAX_COLOR_DISTANCE);
    }

    public ImageComparisonResult compareImages(BufferedImage image1, BufferedImage image2, int viewportHeight, boolean ignoreAntiAliased, boolean strictColorComparison, float maxColorDistance) {

        if (image1 == null || image2 == null) throw new NullPointerException("Can't compare null imagebuffers");

        if (bufferedImagesEqualQuick(image1, image2)) {
            return new ImageComparisonResult(null, 0, 0);
        }

        // cache image widths and heights
        final int width1 = image1.getWidth();
        final int height1 = image1.getHeight();
        final int width2 = image2.getWidth();
        final int height2 = image2.getHeight();

        // calculate max dimensions
        final int maxWidth = max(image1.getWidth(), image2.getWidth());
        final int maxHeight = max(image1.getHeight(), image2.getHeight());

        // calculate min width
        final int minWidth = min(image1.getWidth(), image2.getWidth());

        // convert images to pixel arrays
        final int[] image1Pixels = image1.getRGB(0, 0, width1, height1, null, 0, width1);
        final int[] image2Pixels = image2.getRGB(0, 0, width2, height2, null, 0, width2);

        // calculate pixel counts of img1 and img2
        final int pixelCount1 = width1 * height1;
        final int pixelCount2 = width2 * height2;

        // calculate pixel count min and max
        final int maxPixelCount = maxWidth * maxHeight;
        final int minPixelCount = min(pixelCount1, pixelCount2);

        // compare img1 to img2, pixel by pixel. If different, highlight differenceSum image pixel
        int diffPixelCounter = 0;
        int antiAliasedDiffPixelCounter = 0;
        int lookSameDiffPixelCounter = 0;
        float maxDetectedColorDistance = 0f;

        final int[] differenceImagePixels = new int[maxPixelCount];
        //i1 and i2 are the indices in the image pixel arrays of image1pixels and image2pixels
        //iD is the index of the differenceSum image
        for (int i1 = 0, i2 = 0, iD = 0, x = 0, y = 0; iD < maxPixelCount; ) {
            //mark same pixels with same_color and different pixels in highlight_colors
            if (image1Pixels[i1] != image2Pixels[i2]) {
                if (!strictColorComparison && doColorsLookSame(image1Pixels[i1], image2Pixels[i2], maxColorDistance)) {
                    differenceImagePixels[iD] = LOOK_SAME_COLOR;
                    lookSameDiffPixelCounter++;
                } else if (ignoreAntiAliased && AntiAliasingIgnoringComparator.checkIsAntialiased(image1, image2, x, y, 0d)) {
                    differenceImagePixels[iD] = ANTI_ALIAS_DETECTED_COLOR;
                    antiAliasedDiffPixelCounter++;
                } else {
                    differenceImagePixels[iD] = HIGHLIGHT_COLOR;
                    diffPixelCounter++;
                }
            } else {
                differenceImagePixels[iD] = SAME_COLOR;
            }

            //advance all indices
            i1++;
            i2++;
            iD++;

            //Just calc x and y pos of image1 for anti alias comparison
            if (i1 % width1 == 0) {
                x = 0;
                y++;
            } else {
                x++;
            }

            //one of the two images has a smaller width than the other
            //move index of other picture to end of line and mark pixels
            //with different_size_color
            if (width1 < width2 && i1 % minWidth == 0) {
                while (i2 % maxWidth != 0) {
                    i2++;
                    differenceImagePixels[iD] = DIFFERENT_SIZE_COLOR;
                    diffPixelCounter++;
                    iD++;
                }
            } else if (width2 < width1 && i2 % minWidth == 0) {
                while (i1 % maxWidth != 0) {
                    i1++;
                    differenceImagePixels[iD] = DIFFERENT_SIZE_COLOR;
                    diffPixelCounter++;
                    iD++;
                }
            }

            //one of the two pictures is over
            //mark pixels within width of other remaining picture
            //with different_size_color and mark pixels
            //that neither exist in img1 nor in img2 as same_color
            //(both images don't exist in that area, so they are the same there ;))
            if (i1 == minPixelCount || i2 == minPixelCount) {
                while (iD < maxPixelCount) {
                    if (iD % maxWidth < minWidth) {
                        differenceImagePixels[iD] = DIFFERENT_SIZE_COLOR;
                        diffPixelCounter++;
                    } else {
                        differenceImagePixels[iD] = SAME_COLOR;
                    }
                    iD++;
                }
            }
        }

        double difference = (1d * diffPixelCounter) / min(maxPixelCount, maxWidth * viewportHeight);

        // save differenceImagePixels to a new BufferedImage
        final BufferedImage out = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, maxWidth, maxHeight, differenceImagePixels, 0, maxWidth);
        return new ImageComparisonResult(out, difference, lookSameDiffPixelCounter + antiAliasedDiffPixelCounter);
    }

    private static int[] getARGB(int pixel) {

        int alpha = (pixel >> 24) & 0xFF;
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;

        return new int[]{alpha, red, green, blue};
    }

    static Color getColor(int pixel) {
        int[] argb = getARGB(pixel);
        return new Color(argb[1], argb[2], argb[3], argb[0]);
    }

    //Helper function to compare two BufferedImage instances (BufferedImage doesn't override equals())
    public static boolean bufferedImagesEqual(BufferedImage image1, BufferedImage image2) {
        if (image1.getWidth() == image2.getWidth() && image1.getHeight() == image2.getHeight()) {
            for (int xPosition = 0; xPosition < image1.getWidth(); xPosition++) {
                for (int yPosition = 0; yPosition < image1.getHeight(); yPosition++) {
                    if (image1.getRGB(xPosition, yPosition) != image2.getRGB(xPosition, yPosition))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    static boolean doColorsLookSame(int argbColor1, int argbColor2, double maxColorDistance) {
        int[] argb1 = getARGB(argbColor1);
        int[] argb2 = getARGB(argbColor2);
        LAB lab1 = LAB.fromRGB(argb1[1], argb1[2], argb1[3], 0);
        LAB lab2 = LAB.fromRGB(argb2[1], argb2[2], argb2[3], 0);

        double distance = LAB.ciede2000(lab1, lab2);
        return distance <= maxColorDistance;
    }

    //A very fast byte buffer based image comparison for images containing INT or BYTE type representations
    public static boolean bufferedImagesEqualQuick(BufferedImage image1, BufferedImage image2) {
        DataBuffer dataBuffer1 = image1.getRaster().getDataBuffer();
        DataBuffer dataBuffer2 = image2.getRaster().getDataBuffer();
        if (dataBuffer1 instanceof DataBufferByte && dataBuffer2 instanceof DataBufferByte) {
            DataBufferByte dataBufferBytes1 = (DataBufferByte) dataBuffer1;
            DataBufferByte dataBufferBytes2 = (DataBufferByte) dataBuffer2;
            for (int bank = 0; bank < dataBufferBytes1.getNumBanks(); bank++) {
                byte[] bytes1 = dataBufferBytes1.getData(bank);
                byte[] bytes2 = dataBufferBytes2.getData(bank);
                if (!Arrays.equals(bytes1, bytes2)) {
                    return false;
                }
            }
        } else if (dataBuffer1 instanceof DataBufferInt && dataBuffer2 instanceof DataBufferInt) {
            DataBufferInt dataBufferInt1 = (DataBufferInt) dataBuffer1;
            DataBufferInt dataBufferInt2 = (DataBufferInt) dataBuffer2;
            for (int bank = 0; bank < dataBufferInt1.getNumBanks(); bank++) {
                int[] ints1 = dataBufferInt1.getData(bank);
                int[] ints2 = dataBufferInt2.getData(bank);
                if (!Arrays.equals(ints1, ints2)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }


}
