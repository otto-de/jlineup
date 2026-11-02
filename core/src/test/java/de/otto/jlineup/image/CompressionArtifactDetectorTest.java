package de.otto.jlineup.image;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * The sliding window detector has to rate pixels like the straightforward implementation.
 */
class CompressionArtifactDetectorTest {

    private static final double MAX_COLOR_DISTANCE = 0.8;
    private static final double MAX_PIXEL_COLOR_DISTANCE = 20;
    //between max color distance and max pixel color distance, so the windows are needed
    private static final double PIXEL_COLOR_DISTANCE = 5;

    @Test
    void shouldRateLikeNaiveImplementationWithoutTextureToleranceInRasterOrder() {
        for (int radius : new int[]{1, 2, 5}) {
            for (int[] size : new int[][]{{40, 30}, {1, 25}, {25, 1}, {3, 3}, {7, 4}, {13, 13}, {1, 1}}) {
                int[][] images = noisyImages(size[0], size[1], 1234L + radius, 6);
                Mismatches mismatches = compare(radius, 0, images, size[0], size[1], false);
                assertThat("radius " + radius + ", size " + size[0] + "x" + size[1], mismatches.count, is(0));
            }
        }
    }

    @Test
    void shouldRateLikeNaiveImplementationWithoutTextureToleranceInRandomOrder() {
        int[][] images = noisyImages(50, 40, 99L, 6);
        Mismatches mismatches = compare(3, 0, images, 50, 40, true);
        assertThat(mismatches.count, is(0));
        assertThat("test should contain accepted pixels", mismatches.accepted, greaterThan(0));
        assertThat("test should contain rejected pixels", mismatches.rejected, greaterThan(0));
    }

    @Test
    void shouldRateAlmostLikeNaiveImplementationWithTextureTolerance() {
        for (int radius : new int[]{1, 3, 6}) {
            for (int[] size : new int[][]{{60, 45}, {1, 30}, {30, 1}, {4, 4}, {9, 5}}) {
                int[][] images = noisyImages(size[0], size[1], 4711L + radius, 40);
                for (boolean randomOrder : new boolean[]{false, true}) {
                    Mismatches mismatches = compare(radius, 0.15, images, size[0], size[1], randomOrder);
                    //lightness is rounded to 3 decimals, so only pixels very close to the threshold can differ
                    assertThat("radius " + radius + ", size " + size[0] + "x" + size[1], mismatches.count, lessThanOrEqualTo(mismatches.total / 500));
                }
            }
        }
    }

    @Test
    void shouldAcceptDistantPixelsAndRejectNearPixelsWithoutWindows() {
        CompressionArtifactDetector detector = new CompressionArtifactDetector(2, MAX_PIXEL_COLOR_DISTANCE, 0);
        int[] pixels = new int[25];
        CompressionArtifactDetector.Comparison comparison = detector.forImages(pixels, pixels, 5, 5);

        assertThat(comparison.isCompressionArtifact(2, 2, MAX_PIXEL_COLOR_DISTANCE + 0.1, MAX_COLOR_DISTANCE), is(false));
        assertThat(comparison.isCompressionArtifact(2, 2, MAX_COLOR_DISTANCE - 0.1, MAX_COLOR_DISTANCE), is(true));
    }

    @Test
    void shouldNotShareStateBetweenPairsOfImages() {
        CompressionArtifactDetector detector = new CompressionArtifactDetector(2, MAX_PIXEL_COLOR_DISTANCE, 0);
        int[] white = filled(0xFFFFFFFF, 100);
        int[] black = filled(0xFF000000, 100);

        //a window of black pixels against a window of white pixels is far away
        assertThat(detector.forImages(white, black, 10, 10).isCompressionArtifact(5, 5, PIXEL_COLOR_DISTANCE, MAX_COLOR_DISTANCE), is(false));
        //the same detector with other images has to start from scratch
        assertThat(detector.forImages(white, white, 10, 10).isCompressionArtifact(5, 5, PIXEL_COLOR_DISTANCE, MAX_COLOR_DISTANCE), is(true));
    }

    private static int[] filled(int color, int count) {
        int[] pixels = new int[count];
        java.util.Arrays.fill(pixels, color);
        return pixels;
    }

    private static class Mismatches {
        int count, accepted, rejected, total;
    }

    private static Mismatches compare(int radius, double textureTolerance, int[][] images, int width, int height, boolean randomOrder) {
        NaiveCompressionArtifactDetector naive = new NaiveCompressionArtifactDetector(radius, MAX_PIXEL_COLOR_DISTANCE, textureTolerance);
        CompressionArtifactDetector.Comparison comparison = new CompressionArtifactDetector(radius, MAX_PIXEL_COLOR_DISTANCE, textureTolerance)
                .forImages(images[0], images[1], width, height);

        int[] order = new int[width * height];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        if (randomOrder) {
            Random random = new Random(7);
            for (int i = order.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
            }
        }

        Mismatches mismatches = new Mismatches();
        for (int index : order) {
            int x = index % width, y = index / width;
            boolean expected = naive.isCompressionArtifact(images[0], images[1], width, height, x, y, PIXEL_COLOR_DISTANCE, MAX_COLOR_DISTANCE);
            boolean actual = comparison.isCompressionArtifact(x, y, PIXEL_COLOR_DISTANCE, MAX_COLOR_DISTANCE);
            mismatches.total++;
            if (expected != actual) {
                mismatches.count++;
            }
            if (expected) {
                mismatches.accepted++;
            } else {
                mismatches.rejected++;
            }
        }
        return mismatches;
    }

    //Two versions of a picture with smooth areas and edges, with different noise
    private static int[][] noisyImages(int width, int height, long seed, int noise) {
        Random random = new Random(seed);
        int[][] images = new int[2][width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int base = ((x / 7 + y / 5) % 2 == 0) ? 90 : 170;
                int gradient = (x * 40) / Math.max(1, width);
                for (int[] image : images) {
                    int r = clamp(base + gradient + random.nextInt(2 * noise + 1) - noise);
                    int g = clamp(base - gradient + random.nextInt(2 * noise + 1) - noise);
                    int b = clamp(base + random.nextInt(2 * noise + 1) - noise);
                    image[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        return images;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
