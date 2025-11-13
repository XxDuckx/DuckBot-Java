package com.duckbot.services;

import java.util.Optional;

/**
 * Coordinates access to LDPlayer instances to avoid conflicts.
 */
public interface InstanceRegistry {

    boolean reserve(String instanceName, String runId);

    void release(String instanceName, String runId);

    boolean isReserved(String instanceName);

    Optional<String> byRun(String runId);
}