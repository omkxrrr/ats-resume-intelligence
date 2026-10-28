package com.ats.intelligence.engine;

import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.PreDownloadValidationReport;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.ValidationCheckDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FactPreservationValidatorTest {

    private FactPreservationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FactPreservationValidator(new KeywordNormalizer());
    }

    @Test
    void testRejectsFabricatedSkills() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Samir Patel");
        master.setEmail("samir.patel@example.com");
        master.setSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot", "SQL")));

        TailoredResumeDto tailored = new TailoredResumeDto();
        tailored.setFullName("Samir Patel");
        tailored.setEmail("samir.patel@example.com");
        // Injected fabricated skills not present in master resume
        tailored.setTailoredSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot", "Golang", "Kubernetes", "AWS")));

        PreDownloadValidationReport report = validator.validateAndSanitize(master, tailored);

        assertNotNull(report);
        // Golang, Kubernetes, AWS should have been removed
        assertEquals(2, tailored.getTailoredSkills().size());
        assertTrue(tailored.getTailoredSkills().contains("Java"));
        assertTrue(tailored.getTailoredSkills().contains("Spring Boot"));
        assertFalse(tailored.getTailoredSkills().contains("Golang"));
        assertFalse(tailored.getTailoredSkills().contains("Kubernetes"));
        assertFalse(tailored.getTailoredSkills().contains("AWS"));

        assertTrue(report.getUnsupportedClaimsCount() >= 3);
    }

    @Test
    void testCatchesAndRevertsFabricatedMetrics() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Robin Williams");
        master.setEmail("robin.williams@example.com");

        WorkExperienceDto mExp = new WorkExperienceDto();
        mExp.setCompany("Acme Systems");
        mExp.setJobTitle("Software Engineer");
        mExp.setBullets(new ArrayList<>(Collections.singletonList(
                "Improved database latency for payment transactions."
        )));
        master.setWorkExperiences(new ArrayList<>(Collections.singletonList(mExp)));

        TailoredResumeDto tailored = new TailoredResumeDto();
        tailored.setFullName("Robin Williams");
        tailored.setEmail("robin.williams@example.com");

        WorkExperienceDto tExp = new WorkExperienceDto();
        tExp.setCompany("Acme Systems");
        tExp.setJobTitle("Software Engineer");
        // Injected fabricated metric "92%"
        tExp.setBullets(new ArrayList<>(Collections.singletonList(
                "Improved database latency by 92% for payment transactions."
        )));
        tailored.setTailoredExperiences(new ArrayList<>(Collections.singletonList(tExp)));

        PreDownloadValidationReport report = validator.validateAndSanitize(master, tailored);

        assertNotNull(report);
        // The bullet with the fabricated 92% should be reverted to master bullet
        assertEquals(1, tailored.getTailoredExperiences().get(0).getBullets().size());
        String sanitizedBullet = tailored.getTailoredExperiences().get(0).getBullets().get(0);
        assertFalse(sanitizedBullet.contains("92%"));
        assertTrue(sanitizedBullet.contains("Improved database latency for payment transactions."));
        assertTrue(report.getUnsupportedClaimsCount() > 0);
    }

    @Test
    void testPreservesLegitimateMetricsExactly() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Robin Williams");
        master.setEmail("robin.williams@example.com");

        WorkExperienceDto mExp = new WorkExperienceDto();
        mExp.setCompany("Acme Systems");
        mExp.setJobTitle("Software Engineer");
        mExp.setBullets(new ArrayList<>(Collections.singletonList(
                "Reduced infrastructure cost by $50k and improved throughput by 40%."
        )));
        master.setWorkExperiences(new ArrayList<>(Collections.singletonList(mExp)));

        TailoredResumeDto tailored = new TailoredResumeDto();
        tailored.setFullName("Robin Williams");
        tailored.setEmail("robin.williams@example.com");

        WorkExperienceDto tExp = new WorkExperienceDto();
        tExp.setCompany("Acme Systems");
        tExp.setJobTitle("Software Engineer");
        tExp.setBullets(new ArrayList<>(Collections.singletonList(
                "Engineered scalable solutions reducing infrastructure cost by $50k and improved throughput by 40%."
        )));
        tailored.setTailoredExperiences(new ArrayList<>(Collections.singletonList(tExp)));

        PreDownloadValidationReport report = validator.validateAndSanitize(master, tailored);

        // Genuine metrics are preserved, 0 unsupported claims
        assertEquals(0, report.getUnsupportedClaimsCount());
        assertTrue(tailored.getTailoredExperiences().get(0).getBullets().get(0).contains("$50k"));
        assertTrue(tailored.getTailoredExperiences().get(0).getBullets().get(0).contains("40%"));
    }

    @Test
    void testIdentityLocking() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Original Name");
        master.setEmail("original@example.com");

        TailoredResumeDto tailored = new TailoredResumeDto();
        tailored.setFullName("Manipulated Name");
        tailored.setEmail("manipulated@example.com");

        validator.validateAndSanitize(master, tailored);

        assertEquals("Original Name", tailored.getFullName());
        assertEquals("original@example.com", tailored.getEmail());
    }
}
