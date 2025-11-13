package com.duckbot.services;

import com.duckbot.core.User;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates authentication requests.
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

    public boolean login(String username, String password) throws AuthException {
        if (provider.login(username, password)) {
            currentUser.set(new User(username, "USER", java.time.Instant.now()));
            return true;
        }
        return false;
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