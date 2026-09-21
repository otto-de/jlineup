package de.otto.jlineup.lambda;

import de.otto.jlineup.config.RunStep;
import org.jspecify.annotations.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Packs the artifacts a single Lambda invocation produced (all screenshots of one
 * {@link de.otto.jlineup.browser.ScreenshotContext}, its file tracker and its log) into one ZIP archive,
 * so that the exchange between Lambda and the core module needs exactly one S3 object per invocation
 * instead of one object per screenshot file.
 *
 * <p>The payload is dominated by PNG files, which are already DEFLATE compressed. The point of the bundle
 * is therefore <em>not</em> byte reduction but the elimination of round trips: a run with 200 contexts and
 * 20 scroll positions each drops from ~4400 S3 objects to 200. PNG entries are consequently written with
 * {@link ZipEntry#STORED} so no CPU is wasted re-compressing incompressible data, while the (small but very
 * compressible) JSON and log entries are deflated.
 *
 * <p>A bundle is also atomic: a Lambda that dies mid-upload leaves no partially visible artifacts in S3.
 *
 * <p>Entry names are relative to the report directory, which is exactly the layout
 * {@code LambdaBrowser#mergeLambdaContextsIntoLocalFileStructure} expects after extraction:
 * <pre>
 *     {contextHash}/&lt;screenshot&gt;.png
 *     {contextHash}/metadata_{step}.json
 *     files_{step}_{contextHash}.json
 *     context_{contextHash}_jlineup.log
 * </pre>
 */
public final class ScreenshotBundle {

    /** File extension and S3 key suffix that identifies a bundle. */
    public static final String BUNDLE_EXTENSION = ".zip";
    /** Content type the bundle is uploaded with. */
    public static final String BUNDLE_CONTENT_TYPE = "application/zip";

    private static final String BUNDLE_NAME_PREFIX = "bundle_";
    private static final String PNG_EXTENSION = ".png";
    private static final int BUFFER_SIZE = 256 * 1024;

    private ScreenshotBundle() {
    }

    /**
     * File name (and S3 object name) of the bundle of a single screenshot context. Unique within a run,
     * because a run uses exactly one step and invokes at most one Lambda per context hash.
     *
     * @param contextHash hash of the screenshot context the Lambda worked on
     * @param step        the run step the Lambda was invoked for
     * @return the bundle file name
     */
    public static String bundleFileName(String contextHash, RunStep step) {
        return BUNDLE_NAME_PREFIX + contextHash + "_" + step + BUNDLE_EXTENSION;
    }

    /**
     * Tells bundles apart from the loose files an older Lambda version uploads.
     *
     * @param s3Key an S3 object key
     * @return {@code true} if the key denotes a bundle rather than a loose screenshot file
     */
    public static boolean isBundleKey(String s3Key) {
        return s3Key != null && s3Key.endsWith(BUNDLE_EXTENSION);
    }

    /**
     * The common S3 key prefix of all objects belonging to one run. Shared by the uploading Lambda and the
     * downloading core so that both sides cannot drift apart.
     *
     * @param s3Prefix the configured bucket prefix, may be {@code null}
     * @param runId    the id of the run
     * @return the key prefix of all objects of this run
     */
    public static @NonNull String s3KeyPrefixForRun(String s3Prefix, String runId) {
        String prefix = s3Prefix;
        if (prefix != null) {
            prefix = prefix.endsWith("/") ? prefix : prefix + "/";
            prefix = prefix + "jlineup-" + runId;
        } else {
            prefix = "jlineup-" + runId;
        }
        return prefix;
    }

    /**
     * Zips every regular file below {@code sourceDir} into {@code targetZip}, keeping the paths relative to
     * {@code sourceDir}.
     *
     * @param sourceDir the directory to bundle
     * @param targetZip the archive to create, must lie outside of {@code sourceDir}
     * @return the number of files that were added
     * @throws IOException if the archive cannot be created or a file cannot be read
     */
    public static int zipDirectory(Path sourceDir, Path targetZip) throws IOException {
        if (targetZip.toAbsolutePath().normalize().startsWith(sourceDir.toAbsolutePath().normalize())) {
            throw new IOException("The bundle must not be created inside the directory that is bundled: " + targetZip);
        }
        Path parent = targetZip.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        int count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(targetZip), BUFFER_SIZE))) {
            final List<Path> files;
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                files = walk.filter(Files::isRegularFile).sorted().toList();
            }
            for (Path file : files) {
                addEntry(zip, toEntryName(sourceDir, file), file);
                count++;
            }
        }
        return count;
    }

    private static String toEntryName(Path sourceDir, Path file) {
        return sourceDir.relativize(file).toString().replace(File.separatorChar, '/');
    }

    private static void addEntry(ZipOutputStream zip, String entryName, Path file) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setLastModifiedTime(Files.getLastModifiedTime(file));
        if (entryName.endsWith(PNG_EXTENSION)) {
            // PNG payload is already DEFLATE compressed. Deflating it again costs CPU (which is billed
            // by the millisecond in Lambda) and saves close to nothing, so store it verbatim.
            byte[] content = Files.readAllBytes(file);
            CRC32 crc = new CRC32();
            crc.update(content);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(content.length);
            entry.setCompressedSize(content.length);
            entry.setCrc(crc.getValue());
            zip.putNextEntry(entry);
            zip.write(content);
        } else {
            entry.setMethod(ZipEntry.DEFLATED);
            zip.putNextEntry(entry);
            Files.copy(file, zip);
        }
        zip.closeEntry();
    }

    /**
     * Extracts a bundle straight from the given stream into {@code targetDir}, without an intermediate file
     * on disk. The stream is consumed and closed.
     *
     * @param inputStream the bundle content
     * @param targetDir   the directory to extract into, created if it does not exist
     * @return the number of files that were extracted
     * @throws IOException if the archive is unreadable or an entry would escape {@code targetDir}
     */
    public static int unzipInto(InputStream inputStream, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path root = targetDir.toAbsolutePath().normalize();
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(inputStream, BUFFER_SIZE))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = resolveSafely(root, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path entryParent = target.getParent();
                    if (entryParent != null) {
                        Files.createDirectories(entryParent);
                    }
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                }
                zip.closeEntry();
            }
        }
        return count;
    }

    /**
     * Guards against "zip slip": an entry name like {@code ../../etc/passwd} would otherwise escape the
     * target directory.
     */
    static Path resolveSafely(Path root, String entryName) throws IOException {
        Path target = root.resolve(entryName).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Refusing to extract bundle entry outside of the target directory: '" + entryName + "'");
        }
        return target;
    }
}
