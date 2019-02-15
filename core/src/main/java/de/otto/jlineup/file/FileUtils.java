package de.otto.jlineup.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {


    public static void clearDirectory(String path) throws IOException {
        Path directory = Paths.get(path);
        try(DirectoryStream<Path> paths = Files.newDirectoryStream(directory);) {
            paths.forEach(file -> {
                try {
                    if (Files.isDirectory(file)) {
                        clearDirectory(file.toAbsolutePath().toString());
                    }

                    Files.delete(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static void deleteDirectory(Path path) throws IOException {
        clearDirectory(path.toString());
        Files.deleteIfExists(path);
    }

    public static void deleteDirectory(String path) throws IOException {
        deleteDirectory(Paths.get(path));
    }

}
