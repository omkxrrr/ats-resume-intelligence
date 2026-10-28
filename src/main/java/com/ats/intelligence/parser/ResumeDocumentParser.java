package com.ats.intelligence.parser;

import com.ats.intelligence.dto.MasterResumeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class ResumeDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(ResumeDocumentParser.class);

    private final PdfResumeParser pdfResumeParser;
    private final DocxResumeParser docxResumeParser;
    private final TextResumeExtractor textResumeExtractor;

    public ResumeDocumentParser(PdfResumeParser pdfResumeParser, DocxResumeParser docxResumeParser, TextResumeExtractor textResumeExtractor) {
        this.pdfResumeParser = pdfResumeParser;
        this.docxResumeParser = docxResumeParser;
        this.textResumeExtractor = textResumeExtractor;
    }

    public MasterResumeDto parseUploadedFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "resume";
        }
        // Sanitize filename against path traversal
        originalFilename = originalFilename.replaceAll("[\\\\/]", "_");

        String lowerName = originalFilename.toLowerCase();
        byte[] bytes = file.getBytes();

        String extractedText;
        if (lowerName.endsWith(".pdf")) {
            validatePdfMagicBytes(bytes);
            extractedText = pdfResumeParser.extractText(new ByteArrayInputStream(bytes));
        } else if (lowerName.endsWith(".docx")) {
            validateDocxMagicBytes(bytes);
            extractedText = docxResumeParser.extractText(new ByteArrayInputStream(bytes));
        } else if (lowerName.endsWith(".txt")) {
            extractedText = new String(bytes, StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported file format: '" + originalFilename + "'. Supported formats are .pdf, .docx, and .txt");
        }

        if (extractedText.isBlank()) {
            throw new IllegalArgumentException("No readable text could be extracted from '" + originalFilename + "'. Please ensure the document is not an image-only scan.");
        }

        MasterResumeDto profile = textResumeExtractor.extractProfile(extractedText, originalFilename);
        log.info("Successfully parsed resume '{}': name='{}', email='{}', {} skills, {} experiences",
                originalFilename, profile.getFullName(), profile.getEmail(),
                profile.getSkills().size(), profile.getWorkExperiences().size());
        return profile;
    }

    public MasterResumeDto parseRawText(String rawText, String sourceName) {
        return textResumeExtractor.extractProfile(rawText, sourceName);
    }

    private void validatePdfMagicBytes(byte[] bytes) {
        if (bytes.length < 4 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new IllegalArgumentException("File content does not match standard PDF header (%PDF)");
        }
    }

    private void validateDocxMagicBytes(byte[] bytes) {
        // Standard PK zip header (PK\x03\x04)
        if (bytes.length < 4 || bytes[0] != 0x50 || bytes[1] != 0x4B || bytes[2] != 0x03 || bytes[3] != 0x04) {
            throw new IllegalArgumentException("File content does not match standard DOCX format (PK zip container)");
        }
    }
}
