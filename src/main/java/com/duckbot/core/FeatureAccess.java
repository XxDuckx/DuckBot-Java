package com.duckbot.core;

import java.util.EnumSet;
import java.util.Set;

/**
 * Defines feature access for a user tier.
 */
public class FeatureAccess {
    public Set<UserTier.Feature> allowedFeatures = EnumSet.noneOf(UserTier.Feature.class);

    public FeatureAccess() {
    }

    public FeatureAccess(Set<UserTier.Feature> features) {
        this.allowedFeatures = EnumSet.copyOf(features);
    }

    /**
     * Check if a specific feature is allowed.
     */
    public boolean hasAccess(UserTier.Feature feature) {
        return allowedFeatures.contains(feature);
    }

    /**
     * Check if all features in a set are allowed.
     */
    public boolean hasAccessAll(UserTier.Feature... features) {
        for (UserTier.Feature feature : features) {
            if (!hasAccess(feature)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if any feature in a set is allowed.
     */
    public boolean hasAccessAny(UserTier.Feature... features) {
        for (UserTier.Feature feature : features) {
            if (hasAccess(feature)) {
                return true;
            }
        }
        return false;
    }
}
