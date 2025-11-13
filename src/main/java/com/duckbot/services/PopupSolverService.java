package com.duckbot.services;

/**
 * Detects and resolves in-game popups using image rules.
 */
public interface PopupSolverService {

    void tick(String game);
}