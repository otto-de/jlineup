package de.otto.jlineup.image;

import java.awt.image.BufferedImage;

/**
 *
 * Logic converted from https://github.com/mapbox/pixelmatch, which has an ISC License
 *
 * Idea taken from: http://www.progmat.uaem.mx:8080/artVol2Num2/Articulo3Vol2Num2.pdf
 *
 * ISC License
 *
 * Copyright (c) 2019, Mapbox
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright notice
 * and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 *
 */

public class NtscYiqColorDifferenceCalculator {

    // calculate color difference according to the paper "Measuring perceived color difference
    // using YIQ NTSC transmission color space in mobile applications" by Y. Kotsarenko and F. Ramos
    public static double colorDelta(BufferedImage img1, BufferedImage img2, int xPos, int yPos, boolean yOnly) {
        int pixel1 = img1.getRGB(xPos, yPos);
        int pixel2 = img2.getRGB(xPos, yPos);
        return colorDelta(pixel1, pixel2, yOnly);
    }

    // calculate color difference according to the paper "Measuring perceived color difference
    // using YIQ NTSC transmission color space in mobile applications" by Y. Kotsarenko and F. Ramos
    public static double colorDelta(int argb1, int argb2, boolean yOnly) {
        int a1 = (argb1 >> 24) & 0xff;
        int r1 = (argb1 >> 16) & 0xff;
        int g1 = (argb1 >> 8) & 0xff;
        int b1 = (argb1) & 0xff;

        int a2 = (argb2 >> 24) & 0xff;
        int r2 = (argb2 >> 16) & 0xff;
        int g2 = (argb2 >> 8) & 0xff;
        int b2 = (argb2) & 0xff;

        if (a1 == a2 && r1 == r2 && g1 == g2 && b1 == b2) {
            return 0;
        }

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

    private static double rgb2y(int r, int g, int b) {
        return r * 0.29889531 + g * 0.58662247 + b * 0.11448223;
    }

    private static double rgb2i(int r, int g, int b) {
        return r * 0.59597799 - g * 0.27417610 - b * 0.32180189;
    }

    private static double rgb2q(int r, int g, int b) {
        return r * 0.21147017 - g * 0.52261711 + b * 0.31114694;
    }

    // blend semi-transparent color with white
    private static int blend(int c, int a) {
        return 255 + (c - 255) * a;
    }

}
