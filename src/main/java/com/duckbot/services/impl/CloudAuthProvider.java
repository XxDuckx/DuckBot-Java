package com.duckbot.services.impl;

import com.duckbot.services.AuthException;
import com.duckbot.services.AuthProvider;

/**
 * Placeholder for future cloud authentication implementation.
 */
public final class CloudAuthProvider implements AuthProvider {

    @Override
    public boolean login(String username, String password) throws AuthException {
        throw new AuthException("Cloud authentication is not yet implemented");
    }

    @Override
    public boolean register(String username, String password) throws AuthException {
        throw new AuthException("Cloud authentication is not yet implemented");
    }

    @Override
    public void logout() {
        // No-op until cloud integration is available.
    }

    @Override
    public boolean isOnline() {
        return false;
    }
}