package com.duckbot.scripts.steps;

import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;

/**
 * Pauses execution for the given delay.
 */
public final class WaitStep implements Step {

    public long delay;

    @Override
    public String type() {
        return "WAIT";
    }

    @Override
    public void execute(ScriptContext ctx) throws InterruptedException {
        if (ctx.log != null) {
            ctx.log.info("[{}] Waiting {}ms", ctx.instanceName, delay);
        }
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }
}