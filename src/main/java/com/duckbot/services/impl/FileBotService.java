package com.duckbot.services.impl;

import com.duckbot.core.BotProfile;
import com.duckbot.services.BotService;
import com.duckbot.store.JsonStore;
import com.duckbot.util.DataPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Stores bot profiles as JSON files on disk.
 */
public final class FileBotService implements BotService {

    private final JsonStore store;

    public FileBotService(JsonStore store) {
        this.store = store;
    }

    @Override
    public void save(BotProfile bot) {
        String filename = sanitize(bot.name == null ? bot.id : bot.name) + ".json";
        Path path = DataPaths.botsDir().resolve(filename);
        store.write(path, bot);
    }

    @Override
    public List<BotProfile> loadAll() {
        List<BotProfile> bots = new ArrayList<>();
        try (var paths = Files.list(DataPaths.botsDir())) {
            paths.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> store.read(path, BotProfile.class).ifPresent(bots::add));
        } catch (IOException ignored) {
        }
        return bots;
    }

    @Override
    public Optional<BotProfile> find(String id) {
        return loadAll().stream().filter(bot -> bot.id.equals(id)).findFirst();
    }

    private static String sanitize(String name) {
        return name == null ? "bot" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-_]", "_");
    }
}