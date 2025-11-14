package com.duckbot.core;

import com.duckbot.games.GamePopupManager;
import com.duckbot.games.GameRegistry;
import com.duckbot.games.GameScriptManager;
import com.duckbot.services.*;
import com.duckbot.services.impl.*;
import com.duckbot.store.JsonStore;
import com.duckbot.util.DataPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap class for DuckBot application initialization.
 * Mirrors LSS Bot's Bootstrap pattern for consistency.
 */
public final class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private AuthService authService;
    private ConfigService configService;
    private LogService logService;
    private BotService botService;
    private RunnerService runnerService;
    private Config config;
    private GameScriptManager gameScriptManager;
    private GamePopupManager gamePopupManager;

    private Bootstrap() {
    }

    public static void initialize() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.setup();
            logger.info("DuckBot Bootstrap completed successfully");
        } catch (Exception e) {
            logger.error("Bootstrap failed", e);
            throw new RuntimeException("Failed to initialize DuckBot", e);
        }
    }

    private void setup() {
        // Initialize data paths
        DataPaths.root();
        logger.info("Data paths initialized: {}", DataPaths.root());

        // Initialize storage
        JsonStore store = new JsonStore();

        // Initialize config
        configService = new FileConfigService(store);
        config = configService.load();
        logger.info("Configuration loaded from {}", DataPaths.configFile());

        // Initialize logging
        logService = new FileLogService();

        // Initialize bot service
        botService = new FileBotService(store);
        logger.info("Bot service initialized with {} bots", botService.loadAll().size());

        // Initialize runner service
        InstanceRegistry instanceRegistry = new InMemoryInstanceRegistry();
        runnerService = new DefaultRunnerService(logService, instanceRegistry);

        // Initialize authentication
        AuthProvider authProvider = createAuthProvider(store, config);
        authService = new AuthService(authProvider);

        // Initialize game-specific managers
        gameScriptManager = new GameScriptManager(DataPaths.root());
        gamePopupManager = new GamePopupManager();
        
        // Load popup definitions for all games
        for (GameRegistry.GameDefinition game : GameRegistry.getAllGames()) {
            gamePopupManager.loadGamePopups(game, DataPaths.root());
        }
        logger.info("Game managers initialized with {} supported games", GameRegistry.getGameIds().size());

        // Auto-initialize admin account if needed
        if (authProvider instanceof LocalAuthProvider) {
            LocalAuthProvider localProvider = (LocalAuthProvider) authProvider;
            if (localProvider.requiresAdminSetup()) {
                try {
                    localProvider.register("Duck", "Aedyn2013");
                    logger.info("Admin account initialized");
                } catch (AuthException e) {
                    logger.warn("Failed to initialize admin account", e);
                }
            }
        }

        logger.info("DuckBot initialization complete");
    }

    private AuthProvider createAuthProvider(JsonStore store, Config config) {
        if ("cloud".equalsIgnoreCase(config.authMode)) {
            logger.info("Using cloud authentication provider");
            return new CloudAuthProvider("https://api.duckbot.example.com");
        }
        logger.info("Using local authentication provider");
        return new LocalAuthProvider(store);
    }

    // Static accessors for global services
    private static Bootstrap instance;

    public static Bootstrap getInstance() {
        if (instance == null) {
            instance = new Bootstrap();
            instance.setup();
        }
        return instance;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public LogService getLogService() {
        return logService;
    }

    public BotService getBotService() {
        return botService;
    }

    public RunnerService getRunnerService() {
        return runnerService;
    }

    public Config getConfig() {
        return config;
    }

    public GameScriptManager getGameScriptManager() {
        return gameScriptManager;
    }

    public GamePopupManager getGamePopupManager() {
        return gamePopupManager;
    }
}
