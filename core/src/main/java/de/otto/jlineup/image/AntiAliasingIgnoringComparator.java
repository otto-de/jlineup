package de.otto.jlineup.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * This class is ported from looks-same by Yandex: https://github.com/gemini-testing/looks-same
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 YANDEX LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

public class AntiAliasingIgnoringComparator {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final static double DEFAULT_BRIGHTNESS_TOLERANCE = 0d;
    private final static double JUST_NOTICEABLE_DIFFERENCE = 2.3d;  // Just noticeable difference if ciede2000 >= JND then colors difference is noticeable by human eye

    public static boolean checkIsAntialiased(BufferedImage img1, BufferedImage img2, int x, int y) {
        boolean isPixelAntiAliased = isAntialiased(img2, x, y, img1)
                || isAntialiased(img1, x, y, img2);
        LOG.debug("Check if different pixel {}|{} is because of anti-aliasing: {}", x, y, isPixelAntiAliased);
        return isPixelAntiAliased;
    }

    private static boolean isAntialiased(BufferedImage img1, int xPos, int yPos, BufferedImage img2) {

        if (xPos >= img1.getWidth() || yPos >= img1.getHeight()) {
            return false;
        }

        final Color color1 = ImageService.getColor(img1.getRGB(xPos, yPos));
        final int width = img1.getWidth();
        final int height = img1.getHeight();
        final int x0 = Math.max(xPos - 1, 0);
        final int y0 = Math.max(yPos - 1, 0);
        final int x2 = Math.min(xPos + 1, width - 1);
        final int y2 = Math.min(yPos + 1, height - 1);

        boolean checkExtremePixels = img2 == null;
        double brightnessTolerance = checkExtremePixels ? JUST_NOTICEABLE_DIFFERENCE : DEFAULT_BRIGHTNESS_TOLERANCE;

        int zeroes = 0;
        int positives = 0;
        int negatives = 0;
        double min = 0;
        double max = 0;
        int minX = 0, minY = 0, maxX = 0, maxY = 0;

        for (int y = y0; y <= y2; y++) {
            for (int x = x0; x <= x2; x++) {
                if (x == xPos && y == yPos) {
                    continue;
                }

                // brightness delta between the center pixel and adjacent one
                final double delta = brightnessDelta(ImageService.getColor(img1.getRGB(x, y)), color1);

                // count the number of equal, darker and brighter adjacent pixels
                if (Math.abs(delta) <= brightnessTolerance) {
                    zeroes++;
                } else if (delta > brightnessTolerance) {
                    positives++;
                } else {
                    negatives++;
                }

                // if found more than 2 equal siblings, it's definitely not anti-aliasing
                if (zeroes > 2) {
                    return false;
                }

                if (checkExtremePixels) {
                    continue;
                }

                // remember the darkest pixel
                if (delta < min) {
                    min = delta;
                    minX = x;
                    minY = y;
                }
                // remember the brightest pixel
                if (delta > max) {
                    max = delta;
                    maxX = x;
                    maxY = y;
                }
            }
        }

        if (checkExtremePixels) {
            return true;
        }

        // if there are no both darker and brighter pixels among siblings, it's not anti-aliasing
        if (negatives == 0 || positives == 0) {
            return false;
        }

        // if either the darkest or the brightest pixel has more than 2 equal siblings in both images
        // (definitely not anti-aliased), this pixel is anti-aliased
        return (!isAntialiased(img1, minX, minY, null) && !isAntialiased(img2, minX, minY, null)) ||
                (!isAntialiased(img1, maxX, maxY, null) && !isAntialiased(img2, maxX, maxY, null));
    }

    private static double brightnessDelta(Color color1, Color color2) {
        return rgb2y(color1.getRed(), color1.getGreen(), color1.getBlue()) - rgb2y(color2.getRed(), color2.getGreen(), color2.getBlue());
    }

    // gamma-corrected luminance of a color (YIQ NTSC transmission color space)
    // see https://www.academia.edu/8200524/DIGITAL_IMAGE_PROCESSING_Digital_Image_Processing_PIKS_Inside_Third_Edition
    private static double rgb2y(int r, int g, int b) {
        return r * 0.29889531d + g * 0.58662247d + b * 0.11448223d;
    }

}
