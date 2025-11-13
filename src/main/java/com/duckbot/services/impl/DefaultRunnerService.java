package com.duckbot.services.impl;

import com.duckbot.core.BotProfile;
import com.duckbot.core.BotScriptRef;
import com.duckbot.core.RunStatus;
import com.duckbot.services.InstanceRegistry;
import com.duckbot.services.LogService;
import com.duckbot.services.RunnerService;
import com.duckbot.scripts.DefaultScriptEngine;
import com.duckbot.scripts.Script;
import com.duckbot.scripts.ScriptEngine;
import com.duckbot.scripts.ScriptRunSpec;
import com.duckbot.scripts.steps.LogStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified runner service that schedules scripts sequentially.
 */
public final class DefaultRunnerService implements RunnerService {

    private final ScriptEngine scriptEngine;
    private final InstanceRegistry registry;
    private final LogService logService;
    private final Map<String, List<RunStatus>> runs = new ConcurrentHashMap<>();

    public DefaultRunnerService(LogService logService, InstanceRegistry registry) {
        this.logService = Objects.requireNonNull(logService);
        this.registry = Objects.requireNonNull(registry);
        this.scriptEngine = new DefaultScriptEngine(logService);
    }

    @Override
    public String start(BotProfile bot) {
        String runId = UUID.randomUUID().toString();
        List<RunStatus> statuses = new ArrayList<>();
        runs.put(runId, statuses);
        if (bot.instances.isEmpty()) {
            RunStatus status = new RunStatus();
            status.runId = runId;
            status.botId = bot.id;
            status.instanceName = "default";
            status.scriptName = "N/A";
            status.updateState("ERROR", "No instances configured");
            statuses.add(status);
            return runId;
        }
        bot.instances.forEach(binding -> {
            RunStatus status = new RunStatus();
            status.runId = runId;
            status.botId = bot.id;
            status.instanceName = binding.instanceName;
            status.scriptName = bot.scripts.isEmpty() ? "N/A" : bot.scripts.get(0).scriptName;
            status.updateState("RUNNING", "Scheduled");
            statuses.add(status);
            if (registry.reserve(binding.instanceName, runId)) {
                scheduleScripts(runId, bot, binding.instanceName, bot.scripts);
            } else {
                status.updateState("WAITING", "Instance busy");
            }
        });
        return runId;
    }

    private void scheduleScripts(String runId, BotProfile bot, String instanceName, List<BotScriptRef> scripts) {
        scripts.stream().filter(ref -> ref.enabled).forEach(ref -> {
            Script script = new Script();
            script.name = ref.scriptName;
            LogStep logStep = new LogStep();
            logStep.message = "Running script " + ref.scriptName;
            script.steps.add(logStep);
            ScriptRunSpec spec = new ScriptRunSpec();
            spec.runId = runId;
            spec.botId = bot.id;
            spec.instanceName = instanceName;
            spec.script = script;
            Map<String, Object> vars = bot.overrides == null ? Map.of() : bot.overrides;
            spec.variables = new HashMap<>(vars);
            scriptEngine.runAsync(spec);
        });
    }

    @Override
    public void stop(String runId) {
        List<RunStatus> statuses = runs.remove(runId);
        if (statuses != null) {
            statuses.forEach(status -> {
                registry.release(status.instanceName, runId);
                status.updateState("STOPPED", "Stopped by user");
            });
        }
    }

    @Override
    public List<RunStatus> list() {
        return runs.values().stream().flatMap(List::stream).toList();
    }
}