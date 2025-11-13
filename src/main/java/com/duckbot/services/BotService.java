package com.duckbot.services;

import com.duckbot.core.BotProfile;

import java.util.List;
import java.util.Optional;

/**
 * Persistence API for bot profiles.
 */
public interface BotService {

    void save(BotProfile bot);

    List<BotProfile> loadAll();

    Optional<BotProfile> find(String id);
}