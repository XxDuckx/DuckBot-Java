package com.duckbot.scripts.steps;

import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;
import com.duckbot.util.StringTemplate;

/**
 * Logs a message with optional level.
 */
public final class LogStep implements Step {

    public String message;
    public String level = "INFO";

    @Override
    public String type() {
        return "LOG";
    }

    @Override
    public void execute(ScriptContext ctx) {
        if (ctx.log == null) {
            return;
        }
        String resolved = StringTemplate.resolve(message, ctx.vars);
        String lvl = level == null ? "INFO" : level.toUpperCase();
        switch (lvl) {
            case "DEBUG" -> ctx.log.debug(resolved);
            case "WARN" -> ctx.log.warn(resolved);
            case "ERROR" -> ctx.log.error(resolved);
            default -> ctx.log.info(resolved);
        }
    }
}