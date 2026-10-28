package com.ats.intelligence.service;

import com.ats.intelligence.dto.CertificationDto;
import com.ats.intelligence.dto.EducationDto;
import com.ats.intelligence.dto.ProjectDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PdfAndDocxGeneratorServiceTest {

    private PdfGeneratorService pdfService;
    private DocxGeneratorService docxService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfGeneratorService();
        docxService = new DocxGeneratorService();
    }

    @Test
    void testPdfGenerationAndTextExtractability() throws IOException {
        TailoredResumeDto resume = createSampleTailoredResume();

        byte[] pdfBytes = pdfService.generatePdf(resume);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Generated PDF must be non-trivial in size");

        // Verify PDF is parsable and text is 100% selectable by an ATS
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertFalse(doc.isEncrypted());
            assertTrue(doc.getNumberOfPages() >= 1);

            PDFTextStripper stripper = new PDFTextStripper();
            String extracted = stripper.getText(doc);

            assertTrue(extracted.toUpperCase().contains("DANA SCULLY"));
            assertTrue(extracted.contains("dana.scully@example.com"));
            assertTrue(extracted.contains("Java"));
            assertTrue(extracted.contains("Spring Boot"));
            assertTrue(extracted.contains("Federal Investigator Tech"));
            assertTrue(extracted.contains("Built automated forensic telemetry pipelines"));
            assertTrue(extracted.contains("University of Maryland"));
            assertTrue(extracted.contains("Winner of National Cyber Defense Challenge 2023"));
            assertTrue(extracted.contains("Competitive Programmer"));
        }
    }

    @Test
    void testDocxGenerationAndTextExtractability() throws IOException {
        TailoredResumeDto resume = createSampleTailoredResume();

        byte[] docxBytes = docxService.generateDocx(resume);

        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 500, "Generated DOCX must be non-trivial in size");

        // Verify DOCX is a valid OOXML document and contains expected text
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes));
             org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
            String docxText = extractor.getText();

            assertTrue(docxText.toUpperCase().contains("DANA SCULLY"));
            assertTrue(docxText.contains("dana.scully@example.com"));
            assertTrue(docxText.contains("Java"));
            assertTrue(docxText.contains("Federal Investigator Tech"));
            assertTrue(docxText.contains("Built automated forensic telemetry pipelines"));
            assertTrue(docxText.contains("Winner of National Cyber Defense Challenge 2023"));
            assertTrue(docxText.contains("Competitive Programmer"));
        }
    }

    private TailoredResumeDto createSampleTailoredResume() {
        TailoredResumeDto resume = new TailoredResumeDto();
        resume.setFullName("Dana Scully");
        resume.setHeadline("Software Developer | Systems Engineer");
        resume.setEmail("dana.scully@example.com");
        resume.setPhone("+1 (555) 789-0123");
        resume.setLocation("Washington, DC");
        resume.setLinkedinUrl("https://linkedin.com/in/dana-scully-demo");
        resume.setGithubUrl("https://github.com/dana-scully-demo");

        resume.setTailoredSummary("Senior Technical Investigator with 7+ years of experience designing robust data capture systems.");
        resume.setTailoredSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot", "PostgreSQL", "Docker", "REST")));

        WorkExperienceDto exp = new WorkExperienceDto();
        exp.setCompany("Federal Investigator Tech");
        exp.setJobTitle("Lead Systems Engineer");
        exp.setLocation("Washington, DC");
        exp.setStartDate("01/2020");
        exp.setEndDate("Present");
        exp.setCurrent(true);
        exp.setBullets(new ArrayList<>(Arrays.asList(
                "Built automated forensic telemetry pipelines in Java processing 2M records daily.",
                "Optimized database index strategies reducing query time by 55%."
        )));
        resume.setTailoredExperiences(new ArrayList<>(Collections.singletonList(exp)));

        ProjectDto proj = new ProjectDto();
        proj.setName("ForensicData Vault");
        proj.setTechnologies(Arrays.asList("Java", "Spring Boot", "PostgreSQL"));
        proj.setDescription("Encrypted distributed audit ledger.");
        proj.setBullets(new ArrayList<>(Collections.singletonList("Deployed across 12 distributed nodes.")));
        resume.setTailoredProjects(new ArrayList<>(Collections.singletonList(proj)));

        EducationDto edu = new EducationDto();
        edu.setInstitution("University of Maryland");
        edu.setDegree("B.S.");
        edu.setFieldOfStudy("Physics & Computer Science");
        edu.setGraduationDate("2017");
        resume.setEducation(new ArrayList<>(Collections.singletonList(edu)));

        CertificationDto cert = new CertificationDto();
        cert.setName("Certified Information Privacy Professional (CIPP)");
        cert.setIssuingOrganization("IAPP");
        resume.setCertifications(new ArrayList<>(Collections.singletonList(cert)));

        resume.setAchievements(new ArrayList<>(Collections.singletonList("Winner of National Cyber Defense Challenge 2023")));
        resume.setActivities(new ArrayList<>(Collections.singletonList("Competitive Programmer (LeetCode Knight - Solved 700+ Problems)")));
        resume.setSoftSkills(new ArrayList<>(Arrays.asList("Problem Solving", "Collaboration", "Mentorship")));

        return resume;
    }
}
