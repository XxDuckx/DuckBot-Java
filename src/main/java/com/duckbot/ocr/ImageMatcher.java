package com.duckbot.ocr;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.javacpp.DoublePointer;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

/**
 * Image matcher backed by OpenCV template matching.
 */
public class ImageMatcher {

    public ImageMatcher() {
        // No-op constructor for backward compatibility
    }

    public ImageMatcher(double simulatedConfidence) {
        // Ignore legacy constructor - now uses real matching
    }

    public double match(BufferedImage screenshot, String imagePath) {
        if (screenshot == null || imagePath == null || imagePath.isEmpty()) {
            return 0.0d;
        }

        File templateFile = new File(imagePath);
        if (!templateFile.exists()) {
            return 0.0;
        }

        try {
            // Convert BufferedImage to OpenCV Mat
            Mat source = bufferedImageToMat(screenshot);
            if (source == null || source.empty()) {
                return 0.0;
            }

            // Load template image
            Mat template = imread(imagePath, IMREAD_COLOR);
            if (template == null || template.empty()) {
                source.release();
                return 0.0;
            }

            // Ensure both images are same type
            if (source.channels() != template.channels()) {
                source.release();
                template.release();
                return 0.0;
            }

            // Create result matrix
            int resultCols = source.cols() - template.cols() + 1;
            int resultRows = source.rows() - template.rows() + 1;
            if (resultCols <= 0 || resultRows <= 0) {
                source.release();
                template.release();
                return 0.0;
            }

            Mat result = new Mat(resultRows, resultCols, CV_32FC1);

            // Perform template matching with normalized correlation
            matchTemplate(source, template, result, TM_CCOEFF_NORMED);

            // Find best match location
            DoublePointer minVal = new DoublePointer(1);
            DoublePointer maxVal = new DoublePointer(1);
            Point minLoc = new Point();
            Point maxLoc = new Point();
            minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

            double confidence = maxVal.get();

            // Cleanup
            source.release();
            template.release();
            result.release();
            minVal.close();
            maxVal.close();

            return confidence;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Convert BufferedImage to OpenCV Mat.
     */
    private Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) {
            return null;
        }

        try {
            // Convert to TYPE_3BYTE_BGR if needed
            BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            convertedImage.getGraphics().drawImage(image, 0, 0, null);

            byte[] pixels = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CV_8UC3);
            mat.data().put(pixels);
            return mat;
        } catch (Exception e) {
            return null;
        }
    }
}