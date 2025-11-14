package com.duckbot.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes a variable that can be supplied to a script.
 */
public final class ScriptVariable {

    public String key;
    public String prompt;
    public Object defaultValue;
    public List<Object> options = new ArrayList<>();
    // New, optional UI metadata for dynamic forms
    public String type; // text, number, boolean, select, multiselect, weekdays
    public Double min;
    public Double max;
    public Double step;
    public String section; // grouping label

    public ScriptVariable() {
    }

    @Override
    public String toString() {
        return "ScriptVariable{" +
                "key='" + key + '\'' +
                ", prompt='" + prompt + '\'' +
                ", defaultValue=" + defaultValue +
                ", options=" + options +
                ", type='" + type + '\'' +
                ", min=" + min +
                ", max=" + max +
                ", step=" + step +
                ", section='" + section + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScriptVariable that)) return false;
        return Objects.equals(key, that.key) && Objects.equals(prompt, that.prompt) && Objects.equals(defaultValue, that.defaultValue) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, prompt, defaultValue, options, type, min, max, step, section);
    }
}