package com.duckbot.services;

/**
 * Abstraction for different authentication backends.
 */
public interface AuthProvider {

    boolean login(String username, String password) throws AuthException;

    boolean register(String username, String password) throws AuthException;

    void logout();

    boolean isOnline();
}