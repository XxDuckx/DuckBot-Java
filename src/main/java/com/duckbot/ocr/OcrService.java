package com.duckbot.ocr;

import java.awt.image.BufferedImage;

/**
 * Placeholder OCR service backed by Tess4J in the future.
 */
public class OcrService {

    public String read(BufferedImage image, String region, String language) {
        if (image == null) {
            return "";
        }
        return "stub";
    }
}