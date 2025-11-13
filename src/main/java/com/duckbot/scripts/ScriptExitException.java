package com.duckbot.scripts;

/**
 * Signals an intentional script exit triggered by EXIT() step.
 */
public class ScriptExitException extends RuntimeException {
    public ScriptExitException(String message) {
        super(message);
    }
}