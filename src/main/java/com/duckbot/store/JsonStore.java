package com.duckbot.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Simple JSON persistence helper built on top of Gson.
 */
public class JsonStore {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public <T> Optional<T> read(Path path, Class<T> type) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            return Optional.ofNullable(gson.fromJson(reader, type));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JSON: " + path, e);
        }
    }

    public void write(Path path, Object data) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create directories for: " + path, e);
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write JSON: " + path, e);
        }
    }

    public Gson gson() {
        return gson;
    }
}