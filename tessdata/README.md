# Tesseract OCR Data Files

This directory contains Tesseract language data files required for OCR functionality.

## Required Files

Download the following file from the Tesseract repository:
- **eng.traineddata** (English language data)

## Download Link

https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

## Installation

1. Download `eng.traineddata` from the link above
2. Place it in this `tessdata/` directory
3. Restart DuckBot if it's running

## Additional Languages

For other languages, download the corresponding `.traineddata` files from:
https://github.com/tesseract-ocr/tessdata

Place them in this directory and use the language code in your OCR Read step (e.g., "chi_sim" for Simplified Chinese).

## Environment Variable (Optional)

Alternatively, you can set the `TESSDATA_PREFIX` environment variable to point to a different tessdata directory location.
