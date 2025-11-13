package com.duckbot.scripts.steps;

import com.duckbot.ocr.ImageMatcher;
import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Conditional execution based on an image match.
 */
public final class IfImageStep implements Step {

    public String imagePath;
    public double confidence = 0.9d;
    public List<Step> thenSteps = new ArrayList<>();
    public List<Step> elseSteps = new ArrayList<>();
    private final ImageMatcher matcher = new ImageMatcher();

    @Override
    public String type() {
        return "IF_IMAGE";
    }

    @Override
    public void execute(ScriptContext ctx) throws Exception {
        BufferedImage capture = ctx.tryCapture().orElse(null);
        double score = matcher.match(capture, imagePath);
        boolean condition = score >= confidence;
        if (ctx.log != null) {
            ctx.log.info("[{}] IF_IMAGE '{}' -> {} (score={})", ctx.instanceName, imagePath, condition, score);
        }
        List<Step> branch = condition ? thenSteps : elseSteps;
        for (Step step : branch) {
            step.execute(ctx);
        }
    }
}