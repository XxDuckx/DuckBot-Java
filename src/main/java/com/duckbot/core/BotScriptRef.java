package com.duckbot.core;

import java.util.Objects;

/**
 * Reference to a script within a bot profile.
 */
public final class BotScriptRef {

    public String scriptName;
    public boolean enabled = true;

    public BotScriptRef() {
    }

    public BotScriptRef(String scriptName, boolean enabled) {
        this.scriptName = scriptName;
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "BotScriptRef{" +
                "scriptName='" + scriptName + '\'' +
                ", enabled=" + enabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotScriptRef that)) return false;
        return enabled == that.enabled && Objects.equals(scriptName, that.scriptName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scriptName, enabled);
    }
}