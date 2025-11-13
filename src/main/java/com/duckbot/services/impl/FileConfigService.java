package com.duckbot.services.impl;

import com.duckbot.core.Config;
import com.duckbot.services.ConfigService;
import com.duckbot.store.JsonStore;
import com.duckbot.util.DataPaths;

/**
 * Persists configuration to data/config.json.
 */
public final class FileConfigService implements ConfigService {

    private final JsonStore store;

    public FileConfigService(JsonStore store) {
        this.store = store;
    }

    @Override
    public Config load() {
        return store.read(DataPaths.configFile(), Config.class).orElseGet(Config::new);
    }

    @Override
    public void save(Config config) {
        store.write(DataPaths.configFile(), config);
    }
}