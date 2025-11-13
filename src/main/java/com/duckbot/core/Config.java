package com.duckbot.core;

import java.util.Objects;

/**
 * Application level configuration object.
 */
public final class Config {

    public String authMode = "local";
    public String ldplayer5Path = "C:/LDPlayer4.0/LDPlayer";
    public String ldplayer9Path = "C:/LDPlayer9";
    public String theme = "black-blue";
    public OcrConfig ocr = new OcrConfig();

    public static final class OcrConfig {
        public String tesseractPath = "";
        public String lang = "eng";

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OcrConfig ocrConfig)) return false;
            return Objects.equals(tesseractPath, ocrConfig.tesseractPath) && Objects.equals(lang, ocrConfig.lang);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tesseractPath, lang);
        }
    }
}