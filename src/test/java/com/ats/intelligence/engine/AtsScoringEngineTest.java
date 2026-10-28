package com.ats.intelligence.engine;

import com.ats.intelligence.config.AtsScoringProperties;
import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.MatchReportDto;
import com.ats.intelligence.dto.RequirementMatchDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import com.ats.intelligence.model.RequirementCategory;
import com.ats.intelligence.model.SkillSupportLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AtsScoringEngineTest {

    private AtsScoringEngine scoringEngine;
    private KeywordNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new KeywordNormalizer();
        AtsScoringProperties properties = new AtsScoringProperties();
        scoringEngine = new AtsScoringEngine(normalizer, properties);
    }

    @Test
    void testMatchScoringAndSkillClassification() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Taylor Reed");
        master.setSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot", "PostgreSQL", "Docker")));

        WorkExperienceDto exp = new WorkExperienceDto();
        exp.setCompany("Nexus Software");
        exp.setJobTitle("Software Engineer");
        exp.setBullets(new ArrayList<>(Arrays.asList(
                "Built RESTful microservices in Java with Spring Boot.",
                "Optimized relational schema and queries in PostgreSQL database."
        )));
        master.setWorkExperiences(new ArrayList<>(Collections.singletonList(exp)));

        JobDescriptionDto jd = new JobDescriptionDto();
        // 3 required skills: Java (matched), Spring Boot (matched), Rust (missing)
        jd.setRequiredSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot", "Rust")));
        // 1 preferred skill: Docker (matched)
        jd.setPreferredSkills(new ArrayList<>(Collections.singletonList("Docker")));
        // 1 tool: PostgreSQL (matched)
        jd.setToolsAndPlatforms(new ArrayList<>(Collections.singletonList("PostgreSQL")));

        MatchReportDto report = scoringEngine.evaluateMatch(master, jd);

        assertNotNull(report);
        assertTrue(report.getOverallScore() > 0 && report.getOverallScore() < 100);

        // Verify status classification
        RequirementMatchDto rustMatch = report.getRequirementMatches().stream()
                .filter(m -> m.getRequirement().equals("Rust"))
                .findFirst()
                .orElse(null);

        assertNotNull(rustMatch);
        assertEquals(SkillSupportLevel.MISSING, rustMatch.getStatus());
        assertTrue(rustMatch.getSuggestedAction().contains("gap"));

        RequirementMatchDto javaMatch = report.getRequirementMatches().stream()
                .filter(m -> m.getRequirement().equals("Java"))
                .findFirst()
                .orElse(null);

        assertNotNull(javaMatch);
        assertEquals(SkillSupportLevel.VERIFIED, javaMatch.getStatus());
        assertNotNull(javaMatch.getEvidence());

        assertEquals(4, report.getMatchedCount()); // Java, Spring Boot, Docker, PostgreSQL
        assertEquals(1, report.getMissingCount()); // Rust
    }

    @Test
    void testWeightRedistributionWhenCategoryEmpty() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Dev Candidate");
        master.setSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot")));

        JobDescriptionDto jd = new JobDescriptionDto();
        // Only required skills present, other categories empty
        jd.setRequiredSkills(new ArrayList<>(Arrays.asList("Java", "Spring Boot")));

        MatchReportDto report = scoringEngine.evaluateMatch(master, jd);

        // Because all required skills matched 100%, and other categories were omitted, final score must be 100% without penalty
        assertEquals(100.0, report.getOverallScore(), 0.1);
        assertNotNull(report.getScoreExplanation());
    }
}
