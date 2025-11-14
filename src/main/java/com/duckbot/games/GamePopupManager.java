package com.duckbot.games;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages game-specific popup definitions.
 * Each game can define its own popup patterns (dialogs, confirmations, error messages, etc.)
 *
 * Popup definitions are stored in: data/images/{game}/popups/definitions.json
 *
 * Example structure:
 * {
 *   "popups": [
 *     {
 *       "id": "ok_button",
 *       "type": "button",
 *       "game": "ants",
 *       "imagePath": "ok_button.png",
 *       "description": "Generic OK confirmation button",
 *       "coordinates": {"x": 512, "y": 800},
 *       "size": {"width": 100, "height": 40}
 *     },
 *     {
 *       "id": "confirm_dialog",
 *       "type": "dialog",
 *       "game": "ants",
 *       "imagePath": "confirm_dialog.png",
 *       "description": "Confirmation dialog with yes/no buttons",
 *       "multipleMatches": true
 *     }
 *   ]
 * }
 */
public class GamePopupManager {

    private final Gson gson = new Gson();
    private final Map<String, List<PopupDefinition>> popupDefinitions = new HashMap<>();

    /**
     * Load popup definitions for a game
     */
    public void loadGamePopups(GameRegistry.GameDefinition game, Path dataRoot) {
        try {
            Path definitionsFile = dataRoot.resolve(game.getPopupImagesPath()).resolve("definitions.json");

            if (Files.exists(definitionsFile)) {
                String json = Files.readString(definitionsFile);
                PopupDefinitionFile file = gson.fromJson(json, PopupDefinitionFile.class);
                if (file.popups != null) {
                    popupDefinitions.put(game.getId(), file.popups);
                }
            } else {
                // Initialize empty list for this game
                popupDefinitions.put(game.getId(), new ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Failed to load popups for " + game.getId() + ": " + e.getMessage());
            popupDefinitions.put(game.getId(), new ArrayList<>());
        }
    }

    /**
     * Get all popup definitions for a game
     */
    public List<PopupDefinition> getGamePopups(String gameId) {
        return popupDefinitions.getOrDefault(gameId, new ArrayList<>());
    }

    /**
     * Get a specific popup definition by ID
     */
    public PopupDefinition getPopupDefinition(String gameId, String popupId) {
        return getGamePopups(gameId).stream()
                .filter(p -> p.id.equals(popupId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a new popup definition to a game
     */
    public void addPopupDefinition(String gameId, PopupDefinition popup) {
        popupDefinitions.computeIfAbsent(gameId, k -> new ArrayList<>()).add(popup);
    }

    /**
     * Save popup definitions to file
     */
    public void saveGamePopups(String gameId, Path dataRoot) {
        try {
            GameRegistry.GameDefinition game = GameRegistry.getGame(gameId);
            Path definitionsFile = dataRoot.resolve(game.getPopupImagesPath()).resolve("definitions.json");

            PopupDefinitionFile file = new PopupDefinitionFile();
            file.popups = getGamePopups(gameId);

            String json = gson.toJson(file);
            Files.createDirectories(definitionsFile.getParent());
            Files.writeString(definitionsFile, json);
        } catch (Exception e) {
            System.err.println("Failed to save popups for " + gameId + ": " + e.getMessage());
        }
    }

    /**
     * Popup definition - represents a UI element pattern in a game
     */
    public static class PopupDefinition {
        public String id;                  // Unique identifier (e.g., "ok_button", "confirm_dialog")
        public String type;                // Type: button, dialog, message, error, loading, etc.
        public String game;                // Game ID this popup belongs to
        public String imagePath;           // Path to popup image in popups/ directory
        public String description;         // Human-readable description
        public Map<String, Integer> coordinates; // Optional: x, y coordinates if fixed
        public Map<String, Integer> size;  // Optional: width, height of the element
        public boolean multipleMatches;    // If true, can appear in multiple locations
        public String action;              // Optional: action to take (tap, swipe, etc.)
        public Map<String, Object> metadata; // Game-specific metadata

        public PopupDefinition() {
            this.metadata = new HashMap<>();
        }

        public PopupDefinition(String id, String type, String game, String imagePath, String description) {
            this.id = id;
            this.type = type;
            this.game = game;
            this.imagePath = imagePath;
            this.description = description;
            this.metadata = new HashMap<>();
        }

        @Override
        public String toString() {
            return id + " (" + type + ") - " + description;
        }
    }

    /**
     * Container for popup definitions JSON file
     */
    public static class PopupDefinitionFile {
        public List<PopupDefinition> popups;

        public PopupDefinitionFile() {
            this.popups = new ArrayList<>();
        }
    }
}
