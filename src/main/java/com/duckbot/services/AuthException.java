package com.duckbot.services;

/**
 * Thrown when authentication fails or cannot be processed.
 */
public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}