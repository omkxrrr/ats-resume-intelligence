package com.ats.intelligence.parser;

import com.ats.intelligence.dto.MasterResumeDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ResumeDocumentParserTest {

    private ResumeDocumentParser documentParser;
    private TextResumeExtractor textExtractor;

    @BeforeEach
    void setUp() {
        textExtractor = new TextResumeExtractor();
        PdfResumeParser pdfParser = new PdfResumeParser();
        DocxResumeParser docxParser = new DocxResumeParser();
        documentParser = new ResumeDocumentParser(pdfParser, docxParser, textExtractor);
    }

    @Test
    void testExtractProfileFromPlainText() {
        String syntheticResume = """
                Jordan Lee
                jordan.lee.test@example.com | (555) 987-6543 | Chicago, IL
                https://linkedin.com/in/jordanlee-test | https://github.com/jordanlee-test
                
                SUMMARY
                Versatile Java Software Engineer with 5 years of experience building microservices.
                
                SKILLS
                Java, Spring Boot, PostgreSQL, Docker, AWS, Kubernetes, REST
                
                EXPERIENCE
                Senior Software Developer - Tech Corp
                Jan 2021 - Present
                - Architected high-throughput payment pipeline processing 10,000 transactions daily.
                - Reduced latency by 35% through Redis caching and query indexing.
                
                EDUCATION
                Bachelor of Science in Computer Science - University of Illinois
                2016 - 2020
                GPA: 3.9
                
                PROJECTS
                OrderSync | Java, Spring Boot, PostgreSQL
                - Built distributed order reconciliation engine.
                
                CERTIFICATIONS
                AWS Certified Developer - Amazon Web Services
                """;

        MasterResumeDto profile = textExtractor.extractProfile(syntheticResume, "jordan_lee_synthetic.txt");

        assertNotNull(profile);
        assertEquals("Jordan Lee", profile.getFullName());
        assertEquals("jordan.lee.test@example.com", profile.getEmail());
        assertTrue(profile.getPhone().contains("987-6543"));
        assertEquals("Chicago, IL", profile.getLocation());
        assertTrue(profile.getLinkedinUrl().contains("jordanlee-test"));
        assertTrue(profile.getGithubUrl().contains("jordanlee-test"));

        assertNotNull(profile.getSummary());
        assertTrue(profile.getSummary().contains("Java Software Engineer"));

        assertFalse(profile.getSkills().isEmpty());
        assertTrue(profile.getSkills().contains("Java"));
        assertTrue(profile.getSkills().contains("Spring Boot"));
        assertTrue(profile.getSkills().contains("PostgreSQL"));

        assertFalse(profile.getWorkExperiences().isEmpty());
        assertEquals(1, profile.getWorkExperiences().size());
        assertEquals(2, profile.getWorkExperiences().get(0).getBullets().size());
        assertTrue(profile.getWorkExperiences().get(0).isCurrent());

        assertFalse(profile.getEducation().isEmpty());
        assertEquals("Bachelor of Science", profile.getEducation().get(0).getDegree());

        assertFalse(profile.getProjects().isEmpty());
        assertEquals("OrderSync", profile.getProjects().get(0).getName());

        assertFalse(profile.getCertifications().isEmpty());
        assertTrue(profile.getCertifications().get(0).getName().contains("AWS Certified"));
    }

    @Test
    void testParseValidPdfFile() throws IOException {
        byte[] pdfBytes = createSyntheticPdf("Morgan Vance", "morgan.vance@example.com", "Java, Python, Spring Boot");

        MockMultipartFile file = new MockMultipartFile(
                "file", "morgan_vance.pdf", "application/pdf", pdfBytes);

        MasterResumeDto profile = documentParser.parseUploadedFile(file);

        assertNotNull(profile);
        assertEquals("Morgan Vance", profile.getFullName());
        assertEquals("morgan.vance@example.com", profile.getEmail());
        assertTrue(profile.getSkills().contains("Java"));
    }

    @Test
    void testParseValidDocxFile() throws IOException {
        byte[] docxBytes = createSyntheticDocx("Casey Taylor", "casey.taylor@example.com", "Java, Docker, Kubernetes");

        MockMultipartFile file = new MockMultipartFile(
                "file", "casey_taylor.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes);

        MasterResumeDto profile = documentParser.parseUploadedFile(file);

        assertNotNull(profile);
        assertEquals("Casey Taylor", profile.getFullName());
        assertEquals("casey.taylor@example.com", profile.getEmail());
        assertTrue(profile.getSkills().contains("Kubernetes"));
    }

    @Test
    void testRejectInvalidPdfHeader() {
        byte[] fakeBytes = "NOT A REAL PDF HEADER".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", fakeBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentParser.parseUploadedFile(file));
        assertTrue(ex.getMessage().contains("standard PDF header"));
    }

    @Test
    void testRejectInvalidDocxHeader() {
        byte[] fakeBytes = "CORRUPTED DOCX DATA".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.docx", "application/vnd.openxmlformats", fakeBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentParser.parseUploadedFile(file));
        assertTrue(ex.getMessage().contains("PK zip container"));
    }

    @Test
    void testRejectUnsupportedExtension() {
        byte[] bytes = "Some resume text".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "resume.exe", "application/octet-stream", bytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentParser.parseUploadedFile(file));
        assertTrue(ex.getMessage().contains("Unsupported file format"));
    }

    @Test
    void testRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> documentParser.parseUploadedFile(file));
    }

    private byte[] createSyntheticPdf(String name, String email, String skills) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                stream.newLineAtOffset(50, 750);
                stream.showText(name);
                stream.endText();

                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                stream.newLineAtOffset(50, 730);
                stream.showText(email + " | (555) 123-4567 | Denver, CO");
                stream.endText();

                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                stream.newLineAtOffset(50, 700);
                stream.showText("SKILLS");
                stream.endText();

                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                stream.newLineAtOffset(50, 680);
                stream.showText(skills);
                stream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createSyntheticDocx(String name, String email, String skills) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText(name);

            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText(email + " | Austin, TX");

            XWPFParagraph p3 = doc.createParagraph();
            p3.createRun().setText("SKILLS");

            XWPFParagraph p4 = doc.createParagraph();
            p4.createRun().setText(skills);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }
}
