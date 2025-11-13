package com.duckbot.ocr;

import java.awt.image.BufferedImage;

/**
 * Placeholder image matcher that always returns a fixed confidence.
 */
public class ImageMatcher {

    private final double simulatedConfidence;

    public ImageMatcher() {
        this(0.5d);
    }

    public ImageMatcher(double simulatedConfidence) {
        this.simulatedConfidence = simulatedConfidence;
    }

    public double match(BufferedImage screenshot, String imagePath) {
        if (screenshot == null || imagePath == null || imagePath.isEmpty()) {
            return 0.0d;
        }
        return simulatedConfidence;
    }
}