package com.ats.intelligence.parser;

import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.engine.KeywordNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobDescriptionParserTest {

    private JobDescriptionParser parser;

    @BeforeEach
    void setUp() {
        parser = new JobDescriptionParser(new KeywordNormalizer());
    }

    @Test
    void testParseComprehensiveJobDescription() {
        String jdText = """
                Role: Senior Java Backend Engineer
                Company: CloudNexus Global
                
                About the Role:
                We are seeking a Senior Java Backend Engineer to design scalable distributed systems.
                
                Responsibilities:
                - Architect, build, and maintain high-throughput microservices using Java and Spring Boot.
                - Collaborate with cross-functional teams to design RESTful APIs.
                - Monitor and optimize database performance in PostgreSQL.
                
                Requirements:
                - 5+ years of software development experience with Java.
                - Strong hands-on experience with Spring Boot, Docker, and PostgreSQL.
                - Solid understanding of Microservices and Git.
                
                Preferred Qualifications:
                - Experience with Kubernetes and Kafka.
                - Bachelor's degree in Computer Science or related field.
                
                Tech Stack:
                Java, Spring Boot, PostgreSQL, Redis, Docker, Kubernetes, AWS
                """;

        JobDescriptionDto jd = parser.parse(jdText, null, null, "AUTO");

        assertNotNull(jd);
        assertEquals("Senior Java Backend Engineer", jd.getTargetRole());
        assertEquals("CloudNexus Global", jd.getTargetCompany());

        // Required skills
        assertTrue(jd.getRequiredSkills().contains("Java"));
        assertTrue(jd.getRequiredSkills().contains("Spring Boot"));
        assertTrue(jd.getRequiredSkills().contains("PostgreSQL"));

        // Preferred skills
        assertTrue(jd.getPreferredSkills().contains("Kubernetes") || jd.getToolsAndPlatforms().contains("Kubernetes"));
        assertTrue(jd.getPreferredSkills().contains("Kafka") || jd.getToolsAndPlatforms().contains("Kafka"));

        // Responsibilities
        assertFalse(jd.getResponsibilities().isEmpty());

        // Domain terms
        assertTrue(jd.getDomainTerms().contains("Distributed Systems") || jd.getDomainTerms().contains("Microservices Architecture")
                || jd.getRequiredSkills().contains("Microservices"));

        // Education & Experience
        assertFalse(jd.getEducationAndExperience().isEmpty());
    }

    @Test
    void testSynonymNormalization() {
        KeywordNormalizer normalizer = new KeywordNormalizer();

        assertEquals("kubernetes", normalizer.normalize("k8s"));
        assertEquals("postgresql", normalizer.normalize("postgres"));
        assertEquals("react", normalizer.normalize("react.js"));
        assertEquals("spring boot", normalizer.normalize("springboot"));
        assertEquals("continuous integration", normalizer.normalize("ci/cd"));

        assertTrue(normalizer.matches("k8s", "Experienced with Kubernetes container orchestration"));
        assertTrue(normalizer.matches("postgres", "Developed relational data models in PostgreSQL"));
        assertTrue(normalizer.matches("react.js", "Frontend state management using React components"));
    }
}
