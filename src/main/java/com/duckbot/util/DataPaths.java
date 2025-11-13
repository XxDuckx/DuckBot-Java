package com.duckbot.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Utility for working with the application's data directory.
 */
public final class DataPaths {

    private static final Path ROOT = Path.of("data");

    private DataPaths() {
    }

    public static Path root() {
        ensure(ROOT);
        return ROOT;
    }

    public static Path authDir() {
        return ensure(root().resolve("auth"));
    }

    public static Path configFile() {
        return root().resolve("config.json");
    }

    public static Path usersFile() {
        return authDir().resolve("users.json");
    }

    public static Path botsDir() {
        return ensure(root().resolve("bots"));
    }

    public static Path scriptsDir() {
        return ensure(root().resolve("scripts"));
    }

    public static Path imagesDir() {
        return ensure(root().resolve("images"));
    }

    public static Path popupsDir() {
        return ensure(root().resolve("popups"));
    }

    public static Path logsDir() {
        return ensure(root().resolve("logs"));
    }

    public static Path logFileForToday() {
        return logsDir().resolve(LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + ".txt");
    }

    private static Path ensure(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create directory: " + path, e);
            }
        }
        return path;
    }
}