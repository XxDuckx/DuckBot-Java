package com.duckbot.services;

import com.duckbot.core.User;
import com.duckbot.core.UserTier;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates authentication requests and user tier management.
 */
public final class AuthService {

    private final AuthProvider provider;
    private final AtomicReference<User> currentUser = new AtomicReference<>();

    public AuthService(AuthProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser.get());
    }

    /**
     * Login with credentials and set user tier based on role.
     */
    public boolean login(String username, String password) throws AuthException {
        if (provider.login(username, password)) {
            User user = new User(username, "USER", UserTier.FREE, java.time.Instant.now());
            // Optionally set tier based on username or role in future
            currentUser.set(user);
            return true;
        }
        return false;
    }

    /**
     * Guest login - no credentials required, read-only access.
     */
    public void loginAsGuest() {
        User guest = new User("Guest", "GUEST", UserTier.GUEST, java.time.Instant.now());
        currentUser.set(guest);
    }

    /**
     * Check if current user is logged in (not guest).
     */
    public boolean isLoggedIn() {
        Optional<User> user = currentUser();
        return user.isPresent() && !user.get().role.equals("GUEST");
    }

    /**
     * Check if user has access to a feature.
     */
    public boolean hasAccess(UserTier.Feature feature) {
        Optional<User> user = currentUser();
        if (user.isEmpty()) {
            return UserTier.GUEST.hasAccess(feature);
        }
        return user.get().tier.hasAccess(feature);
    }

    /**
     * Get current user tier.
     */
    public UserTier getCurrentTier() {
        return currentUser()
            .map(u -> u.tier)
            .orElse(UserTier.GUEST);
    }

    /**
     * Set user tier (admin only).
     */
    public void setUserTier(UserTier tier) throws AuthException {
        Optional<User> user = currentUser();
        if (user.isEmpty()) {
            throw new AuthException("No user logged in");
        }
        user.get().tier = tier;
    }

    public boolean register(String username, String password) throws AuthException {
        return provider.register(username, password);
    }

    public void logout() {
        provider.logout();
        currentUser.set(null);
    }

    public boolean isOnline() {
        return provider.isOnline();
    }
}