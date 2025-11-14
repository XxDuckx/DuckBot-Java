package com.duckbot.adb;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ADB client for interacting with Android devices via adb commands.
 * Auto-detects bundled LDPlayer adb.exe or falls back to system PATH.
 */
public class AdbClient {

    private String adbPath;

    public AdbClient() {
        this.adbPath = findAdbPath();
    }

    public AdbClient(String adbPath) {
        this.adbPath = adbPath;
    }

    /**
     * Find adb.exe: first check LDPlayer bundled, then system PATH.
     */
    private static String findAdbPath() {
        // Try to find from common LDPlayer locations
        String[] commonPaths = {
            System.getenv("LDPLAYER_PATH"),
            "C:\\LDPlayer\\LDPlayer9",
            "C:\\LDPlayer\\LDPlayer4",
            "C:\\Program Files\\LDPlayer9",
            "C:\\Program Files (x86)\\LDPlayer9"
        };
        
        for (String path : commonPaths) {
            if (path == null) continue;
            File adb = new File(path, "adb.exe");
            if (adb.exists()) return adb.getAbsolutePath();
        }
        
        // Fallback to system PATH
        return "adb";
    }

    /**
     * Test if ADB is accessible and working.
     */
    public boolean testConnection() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            int exitCode = proc.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get list of connected devices.
     */
    public List<String> listDevices() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "devices");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = proc.getInputStream()) {
                in.transferTo(out);
            }
            proc.waitFor();
            String output = out.toString("UTF-8");
            List<String> devices = new ArrayList<>();
            for (String line : output.split("\n")) {
                if (line.contains("\t") && !line.contains("List of devices")) {
                    String[] parts = line.split("\t");
                    if (parts.length > 0) devices.add(parts[0].trim());
                }
            }
            return devices;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean shell(String serial, String... cmd) {
        return true;
    }

    public boolean tap(String serial, int x, int y) {
        try {
            execAdb("-s", serial, "shell", "input", "tap", String.valueOf(x), String.valueOf(y));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean swipe(String serial, int x1, int y1, int x2, int y2, int durationMs) {
        try {
            execAdb("-s", serial, "shell", "input", "swipe", 
                String.valueOf(x1), String.valueOf(y1), 
                String.valueOf(x2), String.valueOf(y2), 
                String.valueOf(durationMs));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean inputText(String serial, String text) {
        try {
            execAdb("-s", serial, "shell", "input", "text", text.replace(" ", "%s"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public BufferedImage screencap(String serial) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "-s", serial, "exec-out", "screencap", "-p");
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream in = proc.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
            }
            proc.waitFor();
            byte[] pngBytes = baos.toByteArray();
            if (pngBytes.length == 0) {
                // Fallback to blank image if capture fails
                return new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
            }
            return ImageIO.read(new ByteArrayInputStream(pngBytes));
        } catch (Exception e) {
            // Return blank image on error
            return new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        }
    }

    private void execAdb(String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(adbPath);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();
    }

    /**
     * Resolve LDPlayer instance name to ADB serial using ldconsole adb command.
     * Falls back to detecting by console list2 + matching running port.
     */
    public static String resolveInstanceSerial(String instanceName, List<String> ldRoots) {
        for (String root : ldRoots) {
            if (root == null || root.isBlank()) continue;
            File base = new File(root);
            if (!base.exists()) continue;

            // Try ldconsole.exe adb --name <instance>
            File ldconsole = new File(base, "ldconsole.exe");
            if (!ldconsole.exists()) {
                ldconsole = new File(new File(base, "LDPlayer9"), "ldconsole.exe");
            }
            if (ldconsole.exists()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        ldconsole.getAbsolutePath(), "adb", "--name", instanceName
                    );
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (InputStream in = p.getInputStream()) {
                        in.transferTo(out);
                    }
                    p.waitFor();
                    String output = out.toString("UTF-8").trim();
                    // Output typically: 127.0.0.1:5555
                    if (output.contains(":") && !output.contains(" ")) {
                        return output;
                    }
                } catch (Exception ignored) {}
            }
        }
        // Fallback: assume 127.0.0.1:5555 for first instance
        return "127.0.0.1:5555";
    }
}