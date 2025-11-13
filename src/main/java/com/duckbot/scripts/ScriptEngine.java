package com.duckbot.scripts;

/**
 * Runs scripts asynchronously.
 */
public interface ScriptEngine {

    void runAsync(ScriptRunSpec spec);

    void stop(String runId);

    boolean isRunning(String runId);
}