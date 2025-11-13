package com.duckbot.scripts.steps;

import com.duckbot.adb.AdbClient;
import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;
import com.duckbot.util.StringTemplate;

import java.util.Map;

/**
 * Performs a swipe gesture.
 */
public final class SwipeStep implements Step {

    public String x1;
    public String y1;
    public String x2;
    public String y2;
    public int durationMs = 300;

    @Override
    public String type() {
        return "SWIPE";
    }

    @Override
    public void execute(ScriptContext ctx) {
        Map<String, Object> vars = ctx.vars;
        int resolvedX1 = Integer.parseInt(StringTemplate.resolve(x1, vars));
        int resolvedY1 = Integer.parseInt(StringTemplate.resolve(y1, vars));
        int resolvedX2 = Integer.parseInt(StringTemplate.resolve(x2, vars));
        int resolvedY2 = Integer.parseInt(StringTemplate.resolve(y2, vars));
        if (ctx.log != null) {
            ctx.log.info("[{}] Swiping from ({}, {}) to ({}, {}) in {}ms", ctx.instanceName, resolvedX1, resolvedY1, resolvedX2, resolvedY2, durationMs);
        }
        AdbClient adb = ctx.adb;
        if (adb != null) {
            adb.swipe(ctx.instanceName, resolvedX1, resolvedY1, resolvedX2, resolvedY2, durationMs);
        }
    }
}