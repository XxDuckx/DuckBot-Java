package com.duckbot.games;

import java.util.*;

/**
 * Central registry for supported games.
 * Each game has its own namespace for scripts, images, and popup definitions.
 */
public class GameRegistry {

    private static final Map<String, GameDefinition> GAMES = new LinkedHashMap<>();

    static {
        // Initialize supported games
        registerGame(new GameDefinition("ants", "Ants Underground Kingdom", "images/ants", "scripts/ants"));
        registerGame(new GameDefinition("al", "Ants Legend", "images/al", "scripts/al"));
        registerGame(new GameDefinition("roe", "Rise of Empires", "images/roe", "scripts/roe"));
        registerGame(new GameDefinition("west", "West Game", "images/west", "scripts/west"));
        registerGame(new GameDefinition("generic", "Generic (Any Game)", "images/generic", "scripts/generic"));
    }

    private static void registerGame(GameDefinition game) {
        GAMES.put(game.getId(), game);
    }

    /**
     * Get a game definition by ID
     */
    public static GameDefinition getGame(String gameId) {
        return GAMES.getOrDefault(gameId, GAMES.get("generic"));
    }

    /**
     * Get all registered games
     */
    public static Collection<GameDefinition> getAllGames() {
        return Collections.unmodifiableCollection(GAMES.values());
    }

    /**
     * Get game names for combo boxes/dropdowns
     */
    public static List<String> getGameIds() {
        return new ArrayList<>(GAMES.keySet());
    }

    /**
     * Check if a game is registered
     */
    public static boolean isGameSupported(String gameId) {
        return GAMES.containsKey(gameId);
    }

    /**
     * Add a new game (for extension)
     */
    public static void registerGameDynamic(String gameId, String displayName, String imageDir, String scriptDir) {
        if (!GAMES.containsKey(gameId)) {
            registerGame(new GameDefinition(gameId, displayName, imageDir, scriptDir));
        }
    }

    /**
     * Game definition with configuration
     */
    public static class GameDefinition {
        private final String id;
        private final String displayName;
        private final String imagePath;
        private final String scriptPath;
        private final Map<String, GameAsset> assets = new HashMap<>();

        public GameDefinition(String id, String displayName, String imagePath, String scriptPath) {
            this.id = id;
            this.displayName = displayName;
            this.imagePath = imagePath;
            this.scriptPath = scriptPath;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getScriptPath() {
            return scriptPath;
        }

        /**
         * Get the popup images subdirectory for this game
         */
        public String getPopupImagesPath() {
            return imagePath + "/popups";
        }

        /**
         * Get the game images subdirectory (sprites, menus, etc.)
         */
        public String getGameImagesPath() {
            return imagePath + "/game";
        }

        /**
         * Register a game-specific asset (image, popup, etc.)
         */
        public void registerAsset(GameAsset asset) {
            assets.put(asset.getName(), asset);
        }

        /**
         * Get an asset by name
         */
        public GameAsset getAsset(String assetName) {
            return assets.get(assetName);
        }

        /**
         * Get all assets
         */
        public Collection<GameAsset> getAllAssets() {
            return assets.values();
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Represents a game asset (image, popup definition, etc.)
     */
    public static class GameAsset {
        private final String name;
        private final String type; // "image", "popup", "sprite"
        private final String path;
        private final String description;

        public GameAsset(String name, String type, String path, String description) {
            this.name = name;
            this.type = type;
            this.path = path;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getPath() {
            return path;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name + " (" + type + ")";
        }
    }
}
