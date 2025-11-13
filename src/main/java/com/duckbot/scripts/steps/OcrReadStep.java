package com.duckbot.scripts.steps;

import com.duckbot.ocr.OcrService;
import com.duckbot.scripts.ScriptContext;
import com.duckbot.scripts.Step;

/**
 * Stub OCR read step.
 */
public final class OcrReadStep implements Step {

    public String region;
    public String outVar;
    private final OcrService ocrService = new OcrService();

    @Override
    public String type() {
        return "OCR_READ";
    }

    @Override
    public void execute(ScriptContext ctx) {
        String result = ocrService.read(ctx.tryCapture().orElse(null), region, "eng");
        if (ctx.log != null) {
            ctx.log.info("[{}] OCR_READ region={} -> {}", ctx.instanceName, region, result);
        }
        if (ctx.vars != null && outVar != null && !outVar.isBlank()) {
            ctx.vars.put(outVar, result);
        }
    }
}