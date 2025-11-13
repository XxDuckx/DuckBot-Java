package com.duckbot.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the state of a running bot instance.
 */
public final class RunStatus {

    public String runId;
    public String botId;
    public String instanceName;
    public String scriptName;
    public String state = "IDLE";
    public String lastMessage = "";
    public long startedAtEpochMs = Instant.now().toEpochMilli();
    public long updatedAtEpochMs = Instant.now().toEpochMilli();

    public RunStatus() {
    }

    public void updateState(String newState, String message) {
        this.state = newState;
        this.lastMessage = message;
        this.updatedAtEpochMs = Instant.now().toEpochMilli();
    }

    @Override
    public String toString() {
        return "RunStatus{" +
                "runId='" + runId + '\'' +
                ", botId='" + botId + '\'' +
                ", instanceName='" + instanceName + '\'' +
                ", scriptName='" + scriptName + '\'' +
                ", state='" + state + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", startedAtEpochMs=" + startedAtEpochMs +
                ", updatedAtEpochMs=" + updatedAtEpochMs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RunStatus that)) return false;
        return startedAtEpochMs == that.startedAtEpochMs && updatedAtEpochMs == that.updatedAtEpochMs && Objects.equals(runId, that.runId) && Objects.equals(botId, that.botId) && Objects.equals(instanceName, that.instanceName) && Objects.equals(scriptName, that.scriptName) && Objects.equals(state, that.state) && Objects.equals(lastMessage, that.lastMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, botId, instanceName, scriptName, state, lastMessage, startedAtEpochMs, updatedAtEpochMs);
    }
}