package com.duckbot.scripts;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable run specification combining a script with runtime variables.
 */
public final class ScriptRunSpec {

    public String runId;
    public String botId;
    public String instanceName;
    public Script script;
    public Map<String, Object> variables = new HashMap<>();

    public ScriptRunSpec() {
    }

    public Map<String, Object> getVariables() {
        return Map.copyOf(variables);
    }

    @Override
    public String toString() {
        return "ScriptRunSpec{" +
                "runId='" + runId + '\'' +
                ", botId='" + botId + '\'' +
                ", instanceName='" + instanceName + '\'' +
                ", script=" + script +
                ", variables=" + variables +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScriptRunSpec that)) return false;
        return Objects.equals(runId, that.runId)
                && Objects.equals(botId, that.botId)
                && Objects.equals(instanceName, that.instanceName)
                && Objects.equals(script, that.script)
                && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, botId, instanceName, script, variables);
    }
}