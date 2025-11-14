package com.duckbot.scripts;

import com.duckbot.scripts.steps.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory to convert EditableStep (builder representation) to runtime Step instances.
 */
public final class StepFactory {

    private StepFactory() {}

    /**
     * Convert an EditableScript (with EditableSteps) to a runtime Script.
     */
    public static Script toRuntimeScript(EditableScript editable) {
        Script script = new Script();
        script.name = editable.name;
        script.game = editable.game;
        script.author = editable.author;
        script.variables = new ArrayList<>(editable.variables);
        script.steps = new ArrayList<>();
        
        for (EditableStep es : editable.steps) {
            Step step = createStep(es);
            if (step != null) {
                script.steps.add(step);
            }
        }
        
        return script;
    }

    /**
     * Create a runtime Step from an EditableStep.
     */
    public static Step createStep(EditableStep es) {
        if (es.type == null) return null;
        
        return switch (es.type.toLowerCase()) {
            case "tap" -> {
                TapStep step = new TapStep();
                step.x = getStringProp(es, "x", "0");
                step.y = getStringProp(es, "y", "0");
                step.delay = getLongProp(es, "delay", 0L);
                yield step;
            }
            case "swipe" -> {
                SwipeStep step = new SwipeStep();
                step.x1 = getStringProp(es, "x1", "0");
                step.y1 = getStringProp(es, "y1", "0");
                step.x2 = getStringProp(es, "x2", "0");
                step.y2 = getStringProp(es, "y2", "0");
                step.durationMs = getIntProp(es, "durationMs", 300);
                yield step;
            }
            case "scroll" -> {
                ScrollStep step = new ScrollStep();
                step.direction = getStringProp(es, "direction", "DOWN");
                step.distance = getIntProp(es, "distance", 400);
                step.durationMs = getIntProp(es, "durationMs", 300);
                yield step;
            }
            case "wait" -> {
                WaitStep step = new WaitStep();
                step.delay = getLongProp(es, "delay", 1000L);
                yield step;
            }
            case "input", "input text" -> {
                InputStep step = new InputStep();
                step.text = getStringProp(es, "text", "");
                yield step;
            }
            case "if image", "ifimage" -> {
                IfImageStep step = new IfImageStep();
                step.imagePath = getStringProp(es, "imagePath", "");
                step.confidence = getDoubleProp(es, "confidence", 0.9);
                step.thenSteps = new ArrayList<>(); // Nested steps handled separately if needed
                step.elseSteps = new ArrayList<>();
                yield step;
            }
            case "loop" -> {
                LoopStep step = new LoopStep();
                step.count = getIntProp(es, "count", 1);
                step.steps = new ArrayList<>(); // Nested steps
                yield step;
            }
            case "ocr read", "ocrread" -> {
                OcrReadStep step = new OcrReadStep();
                step.region = getStringProp(es, "region", "0,0,100,100");
                step.outVar = getStringProp(es, "outVar", "ocrResult");
                yield step;
            }
            case "log" -> {
                LogStep step = new LogStep();
                step.message = getStringProp(es, "message", "");
                yield step;
            }
            case "exit" -> new ExitStep();
            case "custom js", "customjs" -> {
                CustomJsStep step = new CustomJsStep();
                step.code = getStringProp(es, "code", "");
                yield step;
            }
            default -> null;
        };
    }

    private static String getStringProp(EditableStep es, String key, String defaultValue) {
        Object val = es.props.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private static int getIntProp(EditableStep es, String key, int defaultValue) {
        Object val = es.props.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception e) { return defaultValue; }
        }
        return defaultValue;
    }

    private static long getLongProp(EditableStep es, String key, long defaultValue) {
        Object val = es.props.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception e) { return defaultValue; }
        }
        return defaultValue;
    }

    private static double getDoubleProp(EditableStep es, String key, double defaultValue) {
        Object val = es.props.get(key);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return defaultValue; }
        }
        return defaultValue;
    }

    /**
     * Get list of all supported step types for UI display.
     */
    public static List<String> getSupportedStepTypes() {
        return List.of("Tap", "Swipe", "Scroll", "Wait", "Input Text", "If Image",
                "Loop", "OCR Read", "Log", "Exit", "Custom JS");
    }
}
