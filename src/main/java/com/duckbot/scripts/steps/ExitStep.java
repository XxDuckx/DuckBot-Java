package com.duckbot.scripts.steps;

import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.ScriptExitException;
import com.duckbot.scripts.Step;

/**
 * Immediately exits the script when executed.
 */
public final class ExitStep implements Step {

    @Override
    public String type() {
        return "EXIT";
    }

    @Override
    public void execute(ScriptContext ctx) {
        if (ctx.log != null) {
            ctx.log.warn("[{}] EXIT invoked", ctx.instanceName);
        }
        throw new ScriptExitException("Script terminated via EXIT step");
    }
}