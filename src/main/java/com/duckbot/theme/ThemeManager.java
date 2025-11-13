package com.duckbot.theme;

import javafx.scene.Scene;

import java.util.Objects;

/**
 * Applies CSS themes to JavaFX scenes.
 */
public final class ThemeManager {

    private static final String BLACK_BLUE = "/themes/black-blue.css";
    private static final String DARK_GOLD = "/themes/dark-gold.css";

    private ThemeManager() {
    }

    public static void apply(Scene scene, String theme) {
        Objects.requireNonNull(scene, "scene");
        scene.getStylesheets().clear();
        if (theme == null || theme.equalsIgnoreCase("black-blue")) {
            scene.getStylesheets().add(BLACK_BLUE);
        } else if (theme.equalsIgnoreCase("dark-gold")) {
            // Placeholder stub theme.
            scene.getStylesheets().add(DARK_GOLD);
        } else {
            scene.getStylesheets().add(BLACK_BLUE);
        }
    }
}