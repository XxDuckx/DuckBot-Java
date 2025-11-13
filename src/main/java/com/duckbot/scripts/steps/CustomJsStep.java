package com.duckbot.scripts.steps;

import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;

/**
 * Placeholder for future JavaScript execution.
 */
public final class CustomJsStep implements Step {

    public String code;

    @Override
    public String type() {
        return "CUSTOM_JS";
    }

    @Override
    public void execute(ScriptContext ctx) {
        if (ctx.log != null) {
            ctx.log.warn("CUSTOM_JS step is not implemented. Code length={}.", code == null ? 0 : code.length());
        }
    }
}