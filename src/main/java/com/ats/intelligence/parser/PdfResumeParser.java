package com.ats.intelligence.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfResumeParser {

    private static final Logger log = LoggerFactory.getLogger(PdfResumeParser.class);

    public String extractText(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
            throw new IllegalArgumentException("PDF file is empty");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Cannot parse encrypted/password-protected PDF resume");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                log.warn("Extracted empty text from PDF. File may contain scanned images without text layer.");
                return "";
            }
            return text.trim();
        }
    }
}
