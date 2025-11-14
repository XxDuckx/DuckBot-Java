package com.duckbot.core;

import java.util.EnumSet;
import java.util.Set;

/**
 * Defines user tier levels and their feature access.
 * Tiers are hierarchical: ADMIN > PREMIUM > FREE > GUEST
 */
public enum UserTier {
    GUEST(0, "Guest (Read-Only)", new FeatureAccess() {
        {
            allowedFeatures = EnumSet.of(
                Feature.VIEW_BOTS,
                Feature.VIEW_LOGS,
                Feature.VIEW_UPDATES
            );
        }
    }),

    FREE(1, "Free User", new FeatureAccess() {
        {
            allowedFeatures = EnumSet.of(
                Feature.VIEW_BOTS,
                Feature.CREATE_BOT,
                Feature.VIEW_SCRIPTS,
                Feature.VIEW_LOGS,
                Feature.VIEW_UPDATES,
                Feature.RUN_SCRIPT_LIMITED  // Limited to 2 concurrent runs
            );
        }
    }),

    PREMIUM(2, "Premium User", new FeatureAccess() {
        {
            allowedFeatures = EnumSet.of(
                Feature.VIEW_BOTS,
                Feature.CREATE_BOT,
                Feature.EDIT_BOT,
                Feature.DELETE_BOT,
                Feature.VIEW_SCRIPTS,
                Feature.CREATE_SCRIPT,
                Feature.EDIT_SCRIPT,
                Feature.DELETE_SCRIPT,
                Feature.VIEW_LOGS,
                Feature.RUN_SCRIPT_UNLIMITED,  // Unlimited concurrent runs
                Feature.ADVANCED_POPUP_SOLVER,
                Feature.VIEW_UPDATES,
                Feature.EXPORT_SCRIPTS
            );
        }
    }),

    ADMIN(3, "Administrator", new FeatureAccess() {
        {
            allowedFeatures = EnumSet.allOf(Feature.class);  // All features
        }
    });

    public final int level;
    public final String displayName;
    public final FeatureAccess defaultAccess;

    UserTier(int level, String displayName, FeatureAccess defaultAccess) {
        this.level = level;
        this.displayName = displayName;
        this.defaultAccess = defaultAccess;
    }

    /**
     * Check if this tier has access to a feature.
     */
    public boolean hasAccess(Feature feature) {
        return defaultAccess.allowedFeatures.contains(feature);
    }

    /**
     * Check if this tier is >= another tier (hierarchical check).
     */
    public boolean isAtLeast(UserTier other) {
        return this.level >= other.level;
    }

    /**
     * Available features that can be restricted by tier.
     */
    public enum Feature {
        // Bot Management
        VIEW_BOTS,
        CREATE_BOT,
        EDIT_BOT,
        DELETE_BOT,
        RUN_BOT,

        // Script Management
        VIEW_SCRIPTS,
        CREATE_SCRIPT,
        EDIT_SCRIPT,
        DELETE_SCRIPT,
        RUN_SCRIPT_LIMITED,
        RUN_SCRIPT_UNLIMITED,

        // Advanced Features
        ADVANCED_POPUP_SOLVER,
        EXPORT_SCRIPTS,
        IMPORT_SCRIPTS,
        CUSTOM_JS_STEPS,
        OCR_FEATURES,

        // Logging & Monitoring
        VIEW_LOGS,
        EXPORT_LOGS,
        REAL_TIME_MONITORING,

        // Settings & Admin
        VIEW_SETTINGS,
        EDIT_SETTINGS,
        MANAGE_USERS,
        VIEW_UPDATES,
        AUTO_UPDATE
    }
}
