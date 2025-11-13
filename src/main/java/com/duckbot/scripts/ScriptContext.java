package com.duckbot.scripts;

import com.duckbot.adb.AdbClient;
import com.duckbot.services.LogService;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Holds per-run context for script execution.
 */
public final class ScriptContext {

    public String runId;
    public String botId;
    public String instanceName;
    public Map<String, Object> vars;
    public AdbClient adb;
    public LogService log;
    public Supplier<BufferedImage> screencap;

    public ScriptContext() {
    }

    public Object requireVar(String key) {
        Objects.requireNonNull(vars, "vars");
        if (!vars.containsKey(key)) {
            throw new IllegalStateException("Missing variable: " + key);
        }
        return vars.get(key);
    }

    public Optional<BufferedImage> tryCapture() {
        if (screencap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(screencap.get());
    }
}