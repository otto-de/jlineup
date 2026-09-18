package de.otto.jlineup.lambda;

import de.otto.jlineup.config.RunStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ScreenshotBundleTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripKeepsTheDirectoryLayoutAndContent() throws IOException {
        Path source = Files.createDirectories(tempDir.resolve("report"));
        Files.createDirectories(source.resolve("1234567"));
        Files.write(source.resolve("1234567/example_root_abc_0800_00000_before.png"), pngLikeBytes());
        Files.writeString(source.resolve("1234567/metadata_before.json"), "{\"a\":1}");
        Files.writeString(source.resolve("files_before_1234567.json"), "{\"contexts\":{}}");
        Files.writeString(source.resolve("context_1234567_jlineup.log"), "some log line");

        Path bundle = tempDir.resolve("bundle.zip");
        int zipped = ScreenshotBundle.zipDirectory(source, bundle);
        assertEquals(4, zipped);

        Path target = tempDir.resolve("extracted");
        int extracted;
        try (var in = Files.newInputStream(bundle)) {
            extracted = ScreenshotBundle.unzipInto(in, target);
        }

        assertEquals(4, extracted);
        assertArrayEquals(pngLikeBytes(), Files.readAllBytes(target.resolve("1234567/example_root_abc_0800_00000_before.png")));
        assertEquals("{\"a\":1}", Files.readString(target.resolve("1234567/metadata_before.json")));
        assertEquals("{\"contexts\":{}}", Files.readString(target.resolve("files_before_1234567.json")));
        assertEquals("some log line", Files.readString(target.resolve("context_1234567_jlineup.log")));
    }

    @Test
    void storesPngsUncompressedAndDeflatesEverythingElse() throws IOException {
        Path source = Files.createDirectories(tempDir.resolve("report"));
        // Highly compressible content, so DEFLATED entries are unmistakably smaller than STORED ones.
        byte[] compressible = new byte[8192];
        Files.write(source.resolve("shot.png"), compressible);
        Files.write(source.resolve("files.json"), compressible);

        Path bundle = tempDir.resolve("bundle.zip");
        ScreenshotBundle.zipDirectory(source, bundle);

        Map<String, Integer> methods = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(bundle))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                zip.readAllBytes();
                methods.put(entry.getName(), entry.getMethod());
            }
        }

        assertEquals(ZipEntry.STORED, methods.get("shot.png"), "PNG is already deflate compressed, it must be stored verbatim");
        assertEquals(ZipEntry.DEFLATED, methods.get("files.json"));
    }

    @Test
    void refusesToWriteTheBundleIntoTheDirectoryItBundles() throws IOException {
        Path source = Files.createDirectories(tempDir.resolve("report"));
        IOException e = assertThrows(IOException.class, () -> ScreenshotBundle.zipDirectory(source, source.resolve("bundle.zip")));
        assertTrue(e.getMessage().contains("must not be created inside"));
    }

    @Test
    void refusesToExtractOutsideOfTheTargetDirectory() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("../escaped.png"));
            zip.write("evil".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        Path target = tempDir.resolve("extracted");
        IOException e = assertThrows(IOException.class,
                () -> ScreenshotBundle.unzipInto(new ByteArrayInputStream(out.toByteArray()), target));
        assertTrue(e.getMessage().contains("outside of the target directory"));
        assertFalse(Files.exists(tempDir.resolve("escaped.png")));
    }

    @Test
    void s3KeyPrefixForRunMatchesTheLegacyLayout() {
        assertEquals("jlineup-run1", ScreenshotBundle.s3KeyPrefixForRun(null, "run1"));
        assertEquals("some/prefix/jlineup-run1", ScreenshotBundle.s3KeyPrefixForRun("some/prefix", "run1"));
        assertEquals("some/prefix/jlineup-run1", ScreenshotBundle.s3KeyPrefixForRun("some/prefix/", "run1"));
    }

    @Test
    void bundleFileNameIsUniquePerContextAndStep() {
        assertEquals("bundle_1234567_before.zip", ScreenshotBundle.bundleFileName("1234567", RunStep.before));
        assertEquals("bundle_1234567_after.zip", ScreenshotBundle.bundleFileName("1234567", RunStep.after));
        assertTrue(ScreenshotBundle.isBundleKey("prefix/jlineup-run1/bundle_1234567_before.zip"));
        assertFalse(ScreenshotBundle.isBundleKey("prefix/jlineup-run1/files_before_1234567.json"));
    }

    private static byte[] pngLikeBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4, 5};
    }
}
