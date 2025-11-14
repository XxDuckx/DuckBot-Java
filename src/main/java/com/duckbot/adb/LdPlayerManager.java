package com.duckbot.adb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LDPlayer manager using ldconsole for real instance operations.
 */
public class LdPlayerManager {

    private final List<String> ldRoots;

    public LdPlayerManager(String... roots) {
        this.ldRoots = Arrays.asList(roots);
    }

    public List<Instance> listInstances() {
        List<String> names = LdConsoleHelper.detectInstancesFromConsolePaths(ldRoots);
        List<Instance> result = new ArrayList<>();
        for (String name : names) {
            Instance inst = new Instance();
            inst.name = name;
            inst.running = true; // Assume detected = running
            inst.display = AdbClient.resolveInstanceSerial(name, ldRoots);
            result.add(inst);
        }
        return result;
    }

    public boolean launch(String instance) {
        try {
            List<String> result = LdConsoleHelper.runLdConsoleCommand(ldRoots, "launch", "--name", instance);
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean quit(String instance) {
        try {
            List<String> result = LdConsoleHelper.runLdConsoleCommand(ldRoots, "quit", "--name", instance);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean focus(String instance) {
        try {
            List<String> result = LdConsoleHelper.runLdConsoleCommand(ldRoots, "modify", "--name", instance, "--focus", "1");
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String resolveSerial(String instance) {
        return AdbClient.resolveInstanceSerial(instance, ldRoots);
    }
}