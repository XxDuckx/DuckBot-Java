package com.duckbot.scripts;

import java.util.ArrayList;
import java.util.List;

/** Builder-only script structure used for authoring before converting to runtime Script. */
public final class EditableScript {
    public String name;
    public String game;
    public String author;
    public List<ScriptVariable> variables = new ArrayList<>();
    public List<EditableStep> steps = new ArrayList<>();
}
