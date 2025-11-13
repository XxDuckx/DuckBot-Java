package com.duckbot.services;

import com.duckbot.core.BotProfile;
import com.duckbot.core.RunStatus;

import java.util.List;

/**
 * Manages running bots.
 */
public interface RunnerService {

    String start(BotProfile bot);

    void stop(String runId);

    List<RunStatus> list();
}