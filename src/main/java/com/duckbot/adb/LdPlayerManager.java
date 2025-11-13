package com.duckbot.adb;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub LDPlayer manager that pretends there is a single instance.
 */
public class LdPlayerManager {

    private final List<Instance> instances = new ArrayList<>();

    public LdPlayerManager() {
        Instance defaultInstance = new Instance();
        defaultInstance.name = "LDPlayer-1";
        defaultInstance.running = true;
        defaultInstance.display = "127.0.0.1:5555";
        instances.add(defaultInstance);
    }

    public List<Instance> listInstances() {
        return List.copyOf(instances);
    }

    public boolean launch(String instance) {
        return true;
    }

    public boolean quit(String instance) {
        return true;
    }

    public boolean focus(String instance) {
        return true;
    }

    public String resolveSerial(String instance) {
        return instances.stream()
                .filter(inst -> inst.name.equalsIgnoreCase(instance))
                .findFirst()
                .map(inst -> inst.display)
                .orElse("127.0.0.1:5555");
    }
}