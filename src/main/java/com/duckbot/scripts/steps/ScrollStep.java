package com.duckbot.scripts.steps;

import com.duckbot.adb.AdbClient;
import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;

/**
 * Simplified scroll step implemented via swipe.
 */
public final class ScrollStep implements Step {

    public String direction = "DOWN";
    public int distance = 400;
    public int durationMs = 300;

    @Override
    public String type() {
        return "SCROLL";
    }

    @Override
    public void execute(ScriptContext ctx) {
        if (ctx.log != null) {
            ctx.log.info("[{}] Scrolling {} for {}px", ctx.instanceName, direction, distance);
        }
        AdbClient adb = ctx.adb;
        if (adb == null) {
            return;
        }
        int centerX = 540;
        int centerY = 960;
        int deltaX = 0;
        int deltaY = 0;
        switch (direction.toUpperCase()) {
            case "UP" -> deltaY = distance;
            case "DOWN" -> deltaY = -distance;
            case "LEFT" -> deltaX = distance;
            case "RIGHT" -> deltaX = -distance;
            default -> {
                if (ctx.log != null) {
                    ctx.log.warn("Unknown scroll direction: {}", direction);
                }
                return;
            }
        }
        adb.swipe(ctx.instanceName, centerX, centerY, centerX + deltaX, centerY + deltaY, durationMs);
    }
}