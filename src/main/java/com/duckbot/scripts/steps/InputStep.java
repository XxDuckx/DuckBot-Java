package com.duckbot.scripts.steps;

import com.duckbot.adb.AdbClient;
import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;
import com.duckbot.util.StringTemplate;

/**
 * Sends text input to the current instance.
 */
public final class InputStep implements Step {

    public String text;

    @Override
    public String type() {
        return "INPUT";
    }

    @Override
    public void execute(ScriptContext ctx) {
        String resolved = StringTemplate.resolve(text, ctx.vars);
        if (ctx.log != null) {
            ctx.log.info("[{}] Inputting text: {}", ctx.instanceName, resolved);
        }
        AdbClient adb = ctx.adb;
        if (adb != null) {
            adb.inputText(ctx.instanceName, resolved);
        }
    }
}