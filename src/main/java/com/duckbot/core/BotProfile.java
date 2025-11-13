package com.duckbot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * High level configuration for an automation bot.
 */
public final class BotProfile {

    public String id = UUID.randomUUID().toString();
    public String name;
    public String game;
    public List<BotInstanceBinding> instances = new ArrayList<>();
    public List<BotScriptRef> scripts = new ArrayList<>();
    public boolean runParallel;
    public long instanceCooldownMs = 1_000L;
    public Map<String, Object> overrides = new HashMap<>();

    public BotProfile() {
    }

    @Override
    public String toString() {
        return "BotProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", game='" + game + '\'' +
                ", instances=" + instances +
                ", scripts=" + scripts +
                ", runParallel=" + runParallel +
                ", instanceCooldownMs=" + instanceCooldownMs +
                ", overrides=" + overrides +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotProfile that)) return false;
        return runParallel == that.runParallel
                && instanceCooldownMs == that.instanceCooldownMs
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(game, that.game)
                && Objects.equals(instances, that.instances)
                && Objects.equals(scripts, that.scripts)
                && Objects.equals(overrides, that.overrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, game, instances, scripts, runParallel, instanceCooldownMs, overrides);
    }
}