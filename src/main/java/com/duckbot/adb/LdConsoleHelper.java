package com.duckbot.adb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper to interact with LDPlayer's console (ldconsole/dnconsole) to discover instances.
 * Falls back gracefully when console is missing.
 */
public final class LdConsoleHelper {

    private LdConsoleHelper() {}

    public static List<String> detectInstancesFromConsolePaths(List<String> ldRoots) {
        Set<String> instances = new LinkedHashSet<>();
        for (String root : ldRoots) {
            if (root == null || root.isBlank()) continue;
            File base = new File(root);
            if (!base.exists()) continue;

            // Check base folder
            File ldconsole = new File(base, "ldconsole.exe");
            File dnconsole = new File(base, "dnconsole.exe");
            if (ldconsole.exists()) instances.addAll(runList2(ldconsole));
            if (dnconsole.exists()) instances.addAll(runList2(dnconsole));

            // Check LDPlayer9 subfolder (common structure: C:\LDPlayer\LDPlayer9\...)
            File ld9sub = new File(base, "LDPlayer9");
            if (ld9sub.exists()) {
                File ld9console = new File(ld9sub, "ldconsole.exe");
                File dn9console = new File(ld9sub, "dnconsole.exe");
                if (ld9console.exists()) instances.addAll(runList2(ld9console));
                if (dn9console.exists()) instances.addAll(runList2(dn9console));
            }

            // Also check parent folder if user configured subfolder
            File parent = base.getParentFile();
            if (parent != null) {
                File p1 = new File(parent, "ldconsole.exe");
                File p2 = new File(parent, "dnconsole.exe");
                if (p1.exists()) instances.addAll(runList2(p1));
                if (p2.exists()) instances.addAll(runList2(p2));
            }
        }
        return new ArrayList<>(instances);
    }

    private static List<String> runList2(File consoleExe) {
        List<String> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(consoleExe.getAbsolutePath(), "list2");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("UTF-8")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Typical output lines contain names like LDPlayer-1 or custom names
                    // We pick the token after "name:" if present, else whole line heuristics
                    String name = parseInstanceName(line);
                    if (name != null && !name.isBlank()) {
                        result.add(name.trim());
                    }
                }
            }
            p.waitFor();
        } catch (IOException | InterruptedException ignored) {
            // Silently ignore; we'll fallback to directory heuristics
        }
        return result;
    }

    private static String parseInstanceName(String line) {
        // LDPlayer console outputs CSV: index,name,topWindowHandle,boundWindowHandle,isInFocus,x,y,width,height,dpi
        // Example: 6,GoldTownsRun,0,0,0,-1,-1,960,540,240
        String[] parts = line.split(",");
        if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
            return parts[1].trim();
        }
        
        // Fallback for older format: index:0 name:LDPlayer-1 ...
        String lower = line.toLowerCase();
        int idx = lower.indexOf("name:");
        if (idx >= 0) {
            String tail = line.substring(idx + 5).trim();
            int sp = tail.indexOf(' ');
            return sp > 0 ? tail.substring(0, sp) : tail;
        }
        
        // Fallback: find token containing LDPlayer-
        for (String token : line.split("\\s+")) {
            if (token.startsWith("LDPlayer-")) return token;
        }
        return null;
    }

    /**
     * Run arbitrary ldconsole command with given arguments.
     * Returns output lines if successful, null if console not found or command failed.
     */
    public static List<String> runLdConsoleCommand(List<String> ldRoots, String... args) {
        for (String root : ldRoots) {
            if (root == null || root.isBlank()) continue;
            File base = new File(root);
            File ldconsole = new File(base, "ldconsole.exe");
            File dnconsole = new File(base, "dnconsole.exe");
            File ld9console = new File(base, "LDPlayer9/ldconsole.exe");
            File dn9console = new File(base, "LDPlayer9/dnconsole.exe");
            
            File[] consoles = {ldconsole, dnconsole, ld9console, dn9console};
            for (File console : consoles) {
                if (console.exists()) {
                    try {
                        List<String> cmd = new ArrayList<>();
                        cmd.add(console.getAbsolutePath());
                        for (String arg : args) cmd.add(arg);
                        ProcessBuilder pb = new ProcessBuilder(cmd);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        List<String> output = new ArrayList<>();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("UTF-8")))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.add(line);
                            }
                        }
                        int exitCode = p.waitFor();
                        if (exitCode == 0) {
                            return output;
                        }
                    } catch (IOException | InterruptedException ignored) {
                        // Try next console
                    }
                }
            }
        }
        return null; // No console found or command failed
    }
}
