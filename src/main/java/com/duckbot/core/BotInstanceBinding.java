package com.duckbot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the binding between a bot profile and an LDPlayer instance.
 */
public final class BotInstanceBinding {

    public String instanceName;
    public List<String> accountIds;

    public BotInstanceBinding() {
        this(null, new ArrayList<>());
    }

    public BotInstanceBinding(String instanceName, List<String> accountIds) {
        this.instanceName = instanceName;
        this.accountIds = accountIds == null ? new ArrayList<>() : new ArrayList<>(accountIds);
    }

    public List<String> getAccountIds() {
        return Collections.unmodifiableList(accountIds);
    }

    @Override
    public String toString() {
        return "BotInstanceBinding{" +
                "instanceName='" + instanceName + '\'' +
                ", accountIds=" + accountIds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotInstanceBinding that)) return false;
        return Objects.equals(instanceName, that.instanceName) && Objects.equals(accountIds, that.accountIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceName, accountIds);
    }
}