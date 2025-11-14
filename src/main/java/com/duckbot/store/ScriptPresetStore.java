package com.duckbot.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Lightweight store for per-script presets.
 * Location: data/scripts/<game>/_presets/<scriptName>/<presetName>.json
 */
public final class ScriptPresetStore {
    private final Path dataRoot;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ScriptPresetStore(Path dataRoot) {
        this.dataRoot = dataRoot;
    }

    private Path dir(String game, String scriptName) {
        return dataRoot.resolve("scripts").resolve(game).resolve("_presets").resolve(scriptName);
    }

    public List<String> listPresets(String game, String scriptName) {
        try {
            Path d = dir(game, scriptName);
            if (!Files.exists(d)) return Collections.emptyList();
            List<String> names = new ArrayList<>();
            try (var s = Files.list(d)) {
                s.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    String n = p.getFileName().toString();
                    names.add(n.substring(0, n.length() - 5));
                });
            }
            Collections.sort(names);
            return names;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void savePreset(String game, String scriptName, String presetName, Map<String, Object> values) throws IOException {
        Path d = dir(game, scriptName);
        Files.createDirectories(d);
        Path f = d.resolve(presetName + ".json");
        String json = gson.toJson(values);
        Files.writeString(f, json);
    }

    public Map<String, Object> loadPreset(String game, String scriptName, String presetName) throws IOException {
        Path f = dir(game, scriptName).resolve(presetName + ".json");
        String json = Files.readString(f);
        @SuppressWarnings("unchecked")
        Map<String,Object> map = gson.fromJson(json, Map.class);
        return map;
    }
}
