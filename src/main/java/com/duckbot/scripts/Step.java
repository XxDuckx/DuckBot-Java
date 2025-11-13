package com.duckbot.scripts;

/**
 * Abstraction for a single automation step.
 */
public interface Step {

    /**
     * @return the type identifier of the step.
     */
    String type();

    /**
     * Executes the step using the supplied context.
     */
    void execute(ScriptContext ctx) throws Exception;
}