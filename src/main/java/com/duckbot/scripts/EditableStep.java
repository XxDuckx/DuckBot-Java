package com.duckbot.scripts;

import java.util.HashMap;
import java.util.Map;

/** Builder-only representation of a step with arbitrary properties. */
public final class EditableStep {
    public String type;
    public Map<String,Object> props = new HashMap<>();
}
