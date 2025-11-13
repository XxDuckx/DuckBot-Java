package com.duckbot.services.impl;

import com.duckbot.services.PopupSolverService;
import com.duckbot.store.JsonStore;
import com.duckbot.util.DataPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Placeholder popup solver that scans rule files but does not execute actions yet.
 */
public final class RuleBasedPopupSolver implements PopupSolverService {

    private final JsonStore store;

    public RuleBasedPopupSolver(JsonStore store) {
        this.store = store;
    }

    @Override
    public void tick(String game) {
        Path rulesPath = DataPaths.popupsDir().resolve(game + "/popup_rules.json");
        if (!Files.exists(rulesPath)) {
            return;
        }
        try {
            Files.readString(rulesPath);
        } catch (IOException ignored) {
        }
    }
}