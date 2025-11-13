package com.duckbot.scripts;

import com.duckbot.services.LogService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation that runs scripts on a cached thread pool.
 */
public final class DefaultScriptEngine implements ScriptEngine {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, RunControl> runs = new ConcurrentHashMap<>();
    private final LogService log;

    public DefaultScriptEngine(LogService log) {
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public void runAsync(ScriptRunSpec spec) {
        Objects.requireNonNull(spec.runId, "runId");
        Objects.requireNonNull(spec.script, "script");
        RunControl control = new RunControl();
        RunControl previous = runs.putIfAbsent(spec.runId, control);
        if (previous != null) {
            throw new IllegalStateException("Run already exists: " + spec.runId);
        }
        control.future = executor.submit(() -> executeScript(spec, control));
    }

    private void executeScript(ScriptRunSpec spec, RunControl control) {
        ScriptContext ctx = new ScriptContext();
        ctx.runId = spec.runId;
        ctx.botId = spec.botId;
        ctx.instanceName = spec.instanceName;
        ctx.vars = spec.variables == null ? new HashMap<>() : new HashMap<>(spec.variables);
        ctx.adb = control.adb;
        ctx.log = log;
        ctx.screencap = control.screencap;
        try {
            for (Step step : spec.script.steps) {
                if (control.stopRequested.get()) {
                    log.warn("Run {} interrupted", spec.runId);
                    break;
                }
                step.execute(ctx);
            }
        } catch (ScriptExitException exit) {
            log.info("Run {} exited: {}", spec.runId, exit.getMessage());
        } catch (Exception ex) {
            log.error("Run {} failed: {}", spec.runId, ex.getMessage());
        } finally {
            runs.remove(spec.runId);
        }
    }

    @Override
    public void stop(String runId) {
        RunControl control = runs.get(runId);
        if (control != null) {
            control.stopRequested.set(true);
            if (control.future != null) {
                control.future.cancel(true);
            }
        }
    }

    @Override
    public boolean isRunning(String runId) {
        RunControl control = runs.get(runId);
        return control != null && control.future != null && !control.future.isDone();
    }

    private static final class RunControl {
        final AtomicBoolean stopRequested = new AtomicBoolean(false);
        Future<?> future;
        com.duckbot.adb.AdbClient adb;
        java.util.function.Supplier<java.awt.image.BufferedImage> screencap;
    }
}