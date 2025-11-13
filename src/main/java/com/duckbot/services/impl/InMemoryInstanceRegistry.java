package com.duckbot.services.impl;

import com.duckbot.services.InstanceRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory instance registry.
 */
public final class InMemoryInstanceRegistry implements InstanceRegistry {

    private final Map<String, String> instanceToRun = new ConcurrentHashMap<>();

    @Override
    public boolean reserve(String instanceName, String runId) {
        return instanceToRun.putIfAbsent(instanceName, runId) == null;
    }

    @Override
    public void release(String instanceName, String runId) {
        instanceToRun.computeIfPresent(instanceName, (key, value) -> value.equals(runId) ? null : value);
    }

    @Override
    public boolean isReserved(String instanceName) {
        return instanceToRun.containsKey(instanceName);
    }

    @Override
    public Optional<String> byRun(String runId) {
        return instanceToRun.entrySet().stream()
                .filter(entry -> entry.getValue().equals(runId))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}