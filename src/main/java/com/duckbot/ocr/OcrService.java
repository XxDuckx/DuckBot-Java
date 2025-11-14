package com.duckbot.ocr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.awt.image.BufferedImage;

/**
 * OCR service backed by Tess4J/Tesseract.
 */
public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        tesseract = new Tesseract();
        // Try to set tessdata path - user should place tessdata folder in project root or set TESSDATA_PREFIX
        try {
            String tessDataPath = System.getenv("TESSDATA_PREFIX");
            if (tessDataPath != null && !tessDataPath.isEmpty()) {
                tesseract.setDatapath(tessDataPath);
            } else {
                // Default to tessdata in project root
                tesseract.setDatapath("./tessdata");
            }
        } catch (Exception e) {
            // Fallback to default Tess4J behavior
        }
        tesseract.setLanguage("eng"); // Default English
    }

    public String read(BufferedImage image, String region, String language) {
        if (image == null) {
            return "";
        }
        
        try {
            // Parse region if provided (format: "x,y,width,height")
            BufferedImage target = image;
            if (region != null && !region.isBlank()) {
                String[] parts = region.split(",");
                if (parts.length == 4) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int w = Integer.parseInt(parts[2].trim());
                        int h = Integer.parseInt(parts[3].trim());
                        // Clamp to image bounds
                        x = Math.max(0, Math.min(x, image.getWidth() - 1));
                        y = Math.max(0, Math.min(y, image.getHeight() - 1));
                        w = Math.max(1, Math.min(w, image.getWidth() - x));
                        h = Math.max(1, Math.min(h, image.getHeight() - y));
                        target = image.getSubimage(x, y, w, h);
                    } catch (NumberFormatException ignored) {
                        // Use full image if parsing fails
                    }
                }
            }
            
            if (language != null && !language.isBlank()) {
                tesseract.setLanguage(language);
            }
            
            return tesseract.doOCR(target).trim();
        } catch (TesseractException e) {
            return "OCR_ERROR: " + e.getMessage();
        }
    }
}