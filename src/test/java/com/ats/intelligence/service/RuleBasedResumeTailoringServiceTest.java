package com.ats.intelligence.service;

import com.ats.intelligence.config.AtsScoringProperties;
import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import com.ats.intelligence.engine.AtsScoringEngine;
import com.ats.intelligence.engine.FactPreservationValidator;
import com.ats.intelligence.engine.KeywordNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedResumeTailoringServiceTest {

    private RuleBasedResumeTailoringService tailoringService;

    @BeforeEach
    void setUp() {
        KeywordNormalizer normalizer = new KeywordNormalizer();
        AtsScoringProperties scoringProperties = new AtsScoringProperties();
        AtsScoringEngine scoringEngine = new AtsScoringEngine(normalizer, scoringProperties);
        FactPreservationValidator factValidator = new FactPreservationValidator(normalizer);

        tailoringService = new RuleBasedResumeTailoringService(normalizer, scoringEngine, factValidator);
    }

    @Test
    void testTailoringPrioritizesSkillsAndStrengthensVerbs() {
        MasterResumeDto master = new MasterResumeDto();
        master.setFullName("Avery Brooks");
        master.setEmail("avery.brooks@example.com");
        master.setSummary("Backend engineer experienced in distributed systems.");
        master.setSkills(new ArrayList<>(Arrays.asList("Python", "Java", "Docker", "PostgreSQL", "React")));

        WorkExperienceDto exp = new WorkExperienceDto();
        exp.setCompany("Apex Enterprise");
        exp.setJobTitle("Software Engineer");
        exp.setBullets(new ArrayList<>(Arrays.asList(
                "Responsible for developing high throughput REST services in Java.",
                "Worked on frontend widgets in React."
        )));
        master.setWorkExperiences(new ArrayList<>(Collections.singletonList(exp)));

        JobDescriptionDto jd = new JobDescriptionDto();
        jd.setTargetRole("Senior Java Developer");
        jd.setTargetCompany("Target Corp");
        jd.setRequiredSkills(new ArrayList<>(Arrays.asList("Java", "PostgreSQL")));

        TailoredResumeDto tailored = tailoringService.tailor(master, jd);

        assertNotNull(tailored);
        assertEquals("Avery Brooks", tailored.getFullName());
        assertEquals("Senior Java Developer", tailored.getTargetRole());

        // Java and PostgreSQL must be prioritized to the front of skills
        assertTrue(tailored.getTailoredSkills().indexOf("Java") < tailored.getTailoredSkills().indexOf("Python"));
        assertTrue(tailored.getTailoredSkills().indexOf("PostgreSQL") < tailored.getTailoredSkills().indexOf("Python"));

        // Action verb enhancement on bullets
        String enhancedBullet = tailored.getTailoredExperiences().get(0).getBullets().get(0);
        assertFalse(enhancedBullet.startsWith("Responsible for"));
        assertTrue(enhancedBullet.contains("high throughput REST services in Java"));

        // Match report attached
        assertNotNull(tailored.getMatchReport());
        assertTrue(tailored.getMatchReport().getOverallScore() > 0);
    }
}
