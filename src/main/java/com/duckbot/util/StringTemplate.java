package com.duckbot.util;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple ${var} string substitution utility.
 */
public final class StringTemplate {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private StringTemplate() {
    }

    public static String resolve(String value, Map<String, Object> vars) {
        if (value == null) {
            return null;
        }
        if (vars == null || vars.isEmpty()) {
            return value;
        }
        Matcher matcher = VAR_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object replacement = vars.get(key);
            if (replacement == null) {
                throw new IllegalArgumentException("Missing variable: " + key);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(Objects.toString(replacement)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}