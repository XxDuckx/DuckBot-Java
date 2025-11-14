package com.duckbot.games;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages game-specific scripts, images, and popup definitions.
 * Organizes files by game directory structure:
 *
 * data/
 * ├── scripts/
 * │   ├── ants/
 * │   │   ├── farm_script.json
 * │   │   ├── march_script.json
 * │   │   └── daily_tasks_script.json
 * │   ├── al/
 * │   │   ├── building_script.json
 * │   │   └── march_script.json
 * │   ├── roe/
 * │   │   └── gather_script.json
 * │   └── generic/
 * │       └── custom_script.json
 * └── images/
 *     ├── ants/
 *     │   ├── popups/
 *     │   │   ├── ok_button.png
 *     │   │   ├── confirm_dialog.png
 *     │   │   └── popup_definitions.json
 *     │   └── game/
 *     │       ├── ant_queen.png
 *     │       ├── farm_icon.png
 *     │       └── menu_assets.json
 *     ├── al/
 *     │   ├── popups/
 *     │   └── game/
 *     ├── roe/
 *     │   ├── popups/
 *     │   └── game/
 *     └── generic/
 *         ├── popups/
 *         └── game/
 */
public class GameScriptManager {

    private final Path dataRoot;
    private final Map<String, GameScripts> gameScriptsCache = new HashMap<>();

    public GameScriptManager(Path dataRoot) {
        this.dataRoot = dataRoot;
        initializeGameDirectories();
    }

    /**
     * Initialize directory structure for all games
     */
    private void initializeGameDirectories() {
        for (GameRegistry.GameDefinition game : GameRegistry.getAllGames()) {
            createGameDirectories(game);
        }
    }

    /**
     * Create directory structure for a specific game
     */
    private void createGameDirectories(GameRegistry.GameDefinition game) {
        try {
            // Create script directories
            Path scriptDir = dataRoot.resolve(game.getScriptPath());
            Files.createDirectories(scriptDir);

            // Create image directories
            Path imageDir = dataRoot.resolve(game.getImagePath());
            Files.createDirectories(imageDir);

            Path popupDir = dataRoot.resolve(game.getPopupImagesPath());
            Files.createDirectories(popupDir);

            Path gameImageDir = dataRoot.resolve(game.getGameImagesPath());
            Files.createDirectories(gameImageDir);
        } catch (Exception e) {
            System.err.println("Failed to create game directories for " + game.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Get all scripts for a specific game
     */
    public GameScripts getGameScripts(String gameId) {
        if (gameScriptsCache.containsKey(gameId)) {
            return gameScriptsCache.get(gameId);
        }

        GameRegistry.GameDefinition game = GameRegistry.getGame(gameId);
        GameScripts scripts = new GameScripts(game, dataRoot);
        gameScriptsCache.put(gameId, scripts);
        return scripts;
    }

    /**
     * Get all game scripts (lazy load all games)
     */
    public Map<String, GameScripts> getAllGameScripts() {
        Map<String, GameScripts> allScripts = new HashMap<>();
        for (String gameId : GameRegistry.getGameIds()) {
            allScripts.put(gameId, getGameScripts(gameId));
        }
        return allScripts;
    }

    /**
     * Clear cache to reload from disk
     */
    public void refreshCache() {
        gameScriptsCache.clear();
    }

    /**
     * Container for all scripts/images/popups for a single game
     */
    public static class GameScripts {
        private final GameRegistry.GameDefinition gameDef;
        private final Path dataRoot;
        private final List<GameScriptFile> scripts = new ArrayList<>();
        private final List<GameImageFile> gameImages = new ArrayList<>();
        private final List<GamePopupFile> popupImages = new ArrayList<>();

        public GameScripts(GameRegistry.GameDefinition gameDef, Path dataRoot) {
            this.gameDef = gameDef;
            this.dataRoot = dataRoot;
            loadScripts();
            loadGameImages();
            loadPopupImages();
        }

        private void loadScripts() {
            try {
                Path scriptDir = dataRoot.resolve(gameDef.getScriptPath());
                if (Files.exists(scriptDir)) {
                    Files.list(scriptDir)
                            .filter(p -> p.toString().endsWith(".json"))
                            .forEach(p -> {
                                String name = p.getFileName().toString().replace(".json", "");
                                scripts.add(new GameScriptFile(name, p));
                            });
                }
            } catch (Exception e) {
                System.err.println("Failed to load scripts for " + gameDef.getId() + ": " + e.getMessage());
            }
        }

        private void loadGameImages() {
            try {
                Path imgDir = dataRoot.resolve(gameDef.getGameImagesPath());
                if (Files.exists(imgDir)) {
                    Files.list(imgDir)
                            .filter(p -> isImageFile(p.toString()))
                            .forEach(p -> {
                                String name = p.getFileName().toString();
                                gameImages.add(new GameImageFile(name, p, "game"));
                            });
                }
            } catch (Exception e) {
                System.err.println("Failed to load game images for " + gameDef.getId() + ": " + e.getMessage());
            }
        }

        private void loadPopupImages() {
            try {
                Path popupDir = dataRoot.resolve(gameDef.getPopupImagesPath());
                if (Files.exists(popupDir)) {
                    Files.list(popupDir)
                            .filter(p -> isImageFile(p.toString()))
                            .forEach(p -> {
                                String name = p.getFileName().toString();
                                popupImages.add(new GamePopupFile(name, p));
                            });
                }
            } catch (Exception e) {
                System.err.println("Failed to load popup images for " + gameDef.getId() + ": " + e.getMessage());
            }
        }

        private boolean isImageFile(String path) {
            String lower = path.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        }

        // Getters
        public GameRegistry.GameDefinition getGameDefinition() {
            return gameDef;
        }

        public List<GameScriptFile> getScripts() {
            return Collections.unmodifiableList(scripts);
        }

        public List<GameImageFile> getGameImages() {
            return Collections.unmodifiableList(gameImages);
        }

        public List<GamePopupFile> getPopupImages() {
            return Collections.unmodifiableList(popupImages);
        }

        public int getScriptCount() {
            return scripts.size();
        }

        public int getGameImageCount() {
            return gameImages.size();
        }

        public int getPopupImageCount() {
            return popupImages.size();
        }
    }

    /**
     * Represents a script file for a game
     */
    public static class GameScriptFile {
        private final String name;
        private final Path filePath;

        public GameScriptFile(String name, Path filePath) {
            this.name = name;
            this.filePath = filePath;
        }

        public String getName() {
            return name;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getContent() throws Exception {
            return Files.readString(filePath);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Represents a game image (sprite, menu asset, etc.)
     */
    public static class GameImageFile {
        private final String name;
        private final Path filePath;
        private final String category;

        public GameImageFile(String name, Path filePath, String category) {
            this.name = name;
            this.filePath = filePath;
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public String toString() {
            return name + " [" + category + "]";
        }
    }

    /**
     * Represents a popup image (dialog, confirmation, etc.)
     */
    public static class GamePopupFile {
        private final String name;
        private final Path filePath;

        public GamePopupFile(String name, Path filePath) {
            this.name = name;
            this.filePath = filePath;
        }

        public String getName() {
            return name;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getImageName() {
            return name.replace(".png", "").replace(".jpg", "").replace(".jpeg", "");
        }

        @Override
        public String toString() {
            return name + " [popup]";
        }
    }
}
