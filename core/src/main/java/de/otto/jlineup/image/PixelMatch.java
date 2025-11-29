package de.otto.jlineup.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * This class is ported from pixelmatch by mapbox.
 * <p>
 * This is pixelmatch's license:
 * <p>
 * ISC License
 * <p>
 * Copyright (c) 2019, Mapbox
 * <p>
 * Permission to use, copy, modify, and/or distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright notice
 * and this permission notice appear in all copies.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

public class PixelMatch {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static boolean isAntiAliased(BufferedImage bufferedImage, int x1, int y1, int width, int height, BufferedImage bufferedImage2) {

        final boolean hasAlphaChannel = bufferedImage.getAlphaRaster() != null;
        final boolean hasImage2AlphaChannel = bufferedImage2.getAlphaRaster() != null;
        if (!hasAlphaChannel || !hasImage2AlphaChannel) {
            throw new RuntimeException("Image must have an alpha channel");
            //LOG.warn("Image 1 ({},{}) and image 2 ({},{}) must have an alpha channel for pixelmatch to work", bufferedImage, hasAlphaChannel, bufferedImage2, hasImage2AlphaChannel);
            //return false;
        }

        byte[] img1 = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        byte[] img2 = ((DataBufferByte) bufferedImage2.getRaster().getDataBuffer()).getData();

        if (img1.length != width * height * 4) {
            LOG.debug("Image 1 data size does not match width/height.");
            return false;
        } else if (img2.length != width * height * 4) {
            LOG.debug("Image 2 data size does not match width/height.");
            return false;
        }

        int x0 = Math.max(x1 - 1, 0);
        int y0 = Math.max(y1 - 1, 0);
        int x2 = Math.min(x1 + 1, width - 1);
        int y2 = Math.min(y1 + 1, height - 1);
        int pos = (y1 * width + x1) * 4;
        int zeroes = x1 == x0 || x1 == x2 || y1 == y0 || y1 == y2 ? 1 : 0;
        double min = 0, max = 0;
        int minX = 0, minY = 0, maxX = 0, maxY = 0;

        // go through 8 adjacent pixels
        for (int x = x0; x <= x2; x++) {
            for (int y = y0; y <= y2; y++) {
                if (x == x1 && y == y1) continue;

                // brightness delta between the center pixel and adjacent one
                double delta = colorDelta(img1, img1, pos, (y * width + x) * 4, true);

                // count the number of equal, darker and brighter adjacent pixels
                if (delta == 0) {
                    zeroes++;
                    // if found more than 2 equal siblings, it's definitely not anti-aliasing
                    if (zeroes > 2) return false;

                    // remember the darkest pixel
                } else if (delta < min) {
                    min = delta;
                    minX = x;
                    minY = y;

                    // remember the brightest pixel
                } else if (delta > max) {
                    max = delta;
                    maxX = x;
                    maxY = y;
                }
            }
        }

        // if there are no both darker and brighter pixels among siblings, it's not anti-aliasing
        if (min == 0 || max == 0) return false;

        // if either the darkest or the brightest pixel has 3+ equal siblings in both images
        // (definitely not anti-aliased), this pixel is anti-aliased
        return (hasManySiblings(img1, minX, minY, width, height) && hasManySiblings(img2, minX, minY, width, height)) ||
                (hasManySiblings(img1, maxX, maxY, width, height) && hasManySiblings(img2, maxX, maxY, width, height));
    }

    // check if a pixel has 3+ adjacent pixels of the same color.
    static boolean hasManySiblings(byte[] img, int x1, int y1, int width, int height) {

        int x0 = Math.max(x1 - 1, 0);
        int y0 = Math.max(y1 - 1, 0);
        int x2 = Math.min(x1 + 1, width - 1);
        int y2 = Math.min(y1 + 1, height - 1);
        int pos = (y1 * width + x1) * 4;
        int zeroes = x1 == x0 || x1 == x2 || y1 == y0 || y1 == y2 ? 1 : 0;

        // go through 8 adjacent pixels
        for (int x = x0; x <= x2; x++) {
            for (int y = y0; y <= y2; y++) {
                if (x == x1 && y == y1) continue;

                int pos2 = (y * width + x) * 4;
                if (img[pos] == img[pos2] &&
                        img[pos + 1] == img[pos2 + 1] &&
                        img[pos + 2] == img[pos2 + 2] &&
                        img[pos + 3] == img[pos2 + 3]) zeroes++;

                if (zeroes > 2) return true;
            }
        }

        return false;
    }

    // calculate color difference according to the paper "Measuring perceived color difference
    // using YIQ NTSC transmission color space in mobile applications" by Y. Kotsarenko and F. Ramos
    static double colorDelta(byte[] img1, byte[] img2, int k, int m, boolean yOnly) {

        double r1 = img1[k + 0] + 128;
        double g1 = img1[k + 1] + 128;
        double b1 = img1[k + 2] + 128;
        double a1 = img1[k + 3] + 128;

        double r2 = img2[m + 0] + 128;
        double g2 = img2[m + 1] + 128;
        double b2 = img2[m + 2] + 128;
        double a2 = img2[m + 3] + 128;

        if (a1 == a2 && r1 == r2 && g1 == g2 && b1 == b2) return 0;

        if (a1 < 255) {
            a1 /= 255;
            r1 = blend(r1, a1);
            g1 = blend(g1, a1);
            b1 = blend(b1, a1);
        }

        if (a2 < 255) {
            a2 /= 255;
            r2 = blend(r2, a2);
            g2 = blend(g2, a2);
            b2 = blend(b2, a2);
        }

        double y1 = rgb2y(r1, g1, b1);
        double y2 = rgb2y(r2, g2, b2);
        double y = y1 - y2;

        if (yOnly) return y; // brightness difference only

        double i = rgb2i(r1, g1, b1) - rgb2i(r2, g2, b2);
        double q = rgb2q(r1, g1, b1) - rgb2q(r2, g2, b2);

        double delta = 0.5053 * y * y + 0.299 * i * i + 0.1957 * q * q;

        // encode whether the pixel lightens or darkens in the sign
        return y1 > y2 ? -delta : delta;
    }

    static double rgb2y(double r, double g, double b) {
        return r * 0.29889531 + g * 0.58662247 + b * 0.11448223;
    }

    static double rgb2i(double r, double g, double b) {
        return r * 0.59597799 - g * 0.27417610 - b * 0.32180189;
    }

    static double rgb2q(double r, double g, double b) {
        return r * 0.21147017 - g * 0.52261711 + b * 0.31114694;
    }

    // blend semi-transparent color with white
    static double blend(double c, double a) {
        return 255 + (c - 255) * a;
    }

}
