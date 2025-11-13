package com.duckbot.services;

/**
 * Logging service abstraction so the UI and script engine can share a logger.
 */
public interface LogService {

    void debug(String fmt, Object... args);

    void info(String fmt, Object... args);

    void warn(String fmt, Object... args);

    void error(String fmt, Object... args);
}