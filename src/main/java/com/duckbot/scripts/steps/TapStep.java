package com.duckbot.scripts.steps;

import com.duckbot.adb.AdbClient;
import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;
import com.duckbot.util.StringTemplate;

import java.util.Map;

/**
 * Issues a tap command via ADB.
 */
public final class TapStep implements Step {

    public String x;
    public String y;
    public long delay;

    @Override
    public String type() {
        return "TAP";
    }

    @Override
    public void execute(ScriptContext ctx) throws Exception {
        Map<String, Object> vars = ctx.vars;
        int resolvedX = Integer.parseInt(StringTemplate.resolve(x, vars));
        int resolvedY = Integer.parseInt(StringTemplate.resolve(y, vars));
        if (ctx.log != null) {
            ctx.log.info("[{}] Tapping at ({}, {})", ctx.instanceName, resolvedX, resolvedY);
        }
        AdbClient adb = ctx.adb;
        if (adb != null) {
            adb.tap(ctx.instanceName, resolvedX, resolvedY);
        }
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }
}