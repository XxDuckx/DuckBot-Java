package com.duckbot.adb;

import java.awt.image.BufferedImage;

/**
 * Minimal stub of an ADB client. Real implementation will bridge to LDPlayer.
 */
public class AdbClient {

    public boolean shell(String serial, String... cmd) {
        return true;
    }

    public boolean tap(String serial, int x, int y) {
        return true;
    }

    public boolean swipe(String serial, int x1, int y1, int x2, int y2, int durationMs) {
        return true;
    }

    public boolean inputText(String serial, String text) {
        return true;
    }

    public BufferedImage screencap(String serial) {
        return new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
    }
}