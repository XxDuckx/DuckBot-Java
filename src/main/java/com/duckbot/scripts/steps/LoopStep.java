package com.duckbot.scripts.steps;

import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * Repeats nested steps a fixed number of times.
 */
public final class LoopStep implements Step {

    public int count = 1;
    public List<Step> steps = new ArrayList<>();

    @Override
    public String type() {
        return "LOOP";
    }

    @Override
    public void execute(ScriptContext ctx) throws Exception {
        for (int i = 0; i < count; i++) {
            if (ctx.log != null) {
                ctx.log.info("[{}] LOOP iteration {}/{}", ctx.instanceName, i + 1, count);
            }
            for (Step step : steps) {
                step.execute(ctx);
            }
        }
    }
}