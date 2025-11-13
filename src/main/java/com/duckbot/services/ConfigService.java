package com.duckbot.services;

import com.duckbot.core.Config;

/**
 * Provides access to the persisted configuration file.
 */
public interface ConfigService {

    Config load();

    void save(Config config);
}