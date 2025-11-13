package com.duckbot.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a script loaded from JSON.
 */
public final class Script {

    public String name;
    public String game;
    public String author;
    public List<ScriptVariable> variables = new ArrayList<>();
    public List<Step> steps = new ArrayList<>();

    public Script() {
    }

    public boolean isEmpty() {
        return steps == null || steps.isEmpty();
    }

    @Override
    public String toString() {
        return "Script{" +
                "name='" + name + '\'' +
                ", game='" + game + '\'' +
                ", author='" + author + '\'' +
                ", variables=" + variables +
                ", steps=" + steps +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Script script)) return false;
        return Objects.equals(name, script.name) && Objects.equals(game, script.game) && Objects.equals(author, script.author) && Objects.equals(variables, script.variables) && Objects.equals(steps, script.steps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, game, author, variables, steps);
    }
}