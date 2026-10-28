package com.ats.intelligence.service;

import com.ats.intelligence.dto.CertificationDto;
import com.ats.intelligence.dto.EducationDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.ProjectDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResumeStorageService {

    private static final Logger log = LoggerFactory.getLogger(ResumeStorageService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // In-memory active session caches
    private MasterResumeDto currentMasterResume;
    private final Map<String, TailoredResumeDto> tailoredVersions = new ConcurrentHashMap<>();
    private String activeTailoredVersionId;
    private boolean sqliteAvailable = false;

    public ResumeStorageService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initDatabase() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS master_resumes (
                        id TEXT PRIMARY KEY,
                        full_name TEXT,
                        email TEXT,
                        verified INTEGER DEFAULT 0,
                        payload_json TEXT,
                        updated_at TEXT
                    );
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS tailored_resumes (
                        id TEXT PRIMARY KEY,
                        master_id TEXT,
                        target_role TEXT,
                        target_company TEXT,
                        overall_score REAL,
                        payload_json TEXT,
                        created_at TEXT
                    );
                    """);
            sqliteAvailable = true;
            log.info("SQLite persistence initialized successfully.");

            // Try loading most recent master resume if exists
            loadLatestMasterResume();
        } catch (Exception e) {
            log.warn("SQLite database initialization failed ({}), using in-memory session persistence.", e.getMessage());
            sqliteAvailable = false;
        }
    }

    public synchronized void saveMasterResume(MasterResumeDto master) {
        if (master.getId() == null) {
            master.setId(UUID.randomUUID().toString());
        }
        this.currentMasterResume = master;

        if (sqliteAvailable) {
            try {
                String json = objectMapper.writeValueAsString(master);
                jdbcTemplate.update("""
                        INSERT INTO master_resumes (id, full_name, email, verified, payload_json, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            full_name = excluded.full_name,
                            email = excluded.email,
                            verified = excluded.verified,
                            payload_json = excluded.payload_json,
                            updated_at = excluded.updated_at
                        """,
                        master.getId(),
                        master.getFullName(),
                        master.getEmail(),
                        master.isVerified() ? 1 : 0,
                        json,
                        LocalDateTime.now().toString());
                log.info("Master resume saved to SQLite: id='{}', name='{}'", master.getId(), master.getFullName());
            } catch (Exception e) {
                log.warn("Failed to persist master resume to SQLite: {}", e.getMessage());
            }
        }
    }

    public synchronized MasterResumeDto getMasterResume() {
        return this.currentMasterResume;
    }

    public synchronized void saveTailoredResume(TailoredResumeDto tailored) {
        if (tailored.getVersionId() == null) {
            tailored.setVersionId(UUID.randomUUID().toString());
        }
        this.tailoredVersions.put(tailored.getVersionId(), tailored);
        this.activeTailoredVersionId = tailored.getVersionId();

        if (sqliteAvailable) {
            try {
                String json = objectMapper.writeValueAsString(tailored);
                double score = tailored.getMatchReport() != null ? tailored.getMatchReport().getOverallScore() : 0.0;
                String masterId = currentMasterResume != null ? currentMasterResume.getId() : null;

                jdbcTemplate.update("""
                        INSERT INTO tailored_resumes (id, master_id, target_role, target_company, overall_score, payload_json, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            target_role = excluded.target_role,
                            target_company = excluded.target_company,
                            overall_score = excluded.overall_score,
                            payload_json = excluded.payload_json
                        """,
                        tailored.getVersionId(),
                        masterId,
                        tailored.getTargetRole(),
                        tailored.getTargetCompany(),
                        score,
                        json,
                        tailored.getCreatedAt());
            } catch (Exception e) {
                log.warn("Failed to persist tailored resume to SQLite: {}", e.getMessage());
            }
        }
    }

    public synchronized TailoredResumeDto getActiveTailoredResume() {
        if (activeTailoredVersionId != null) {
            return tailoredVersions.get(activeTailoredVersionId);
        }
        return null;
    }

    public synchronized TailoredResumeDto getTailoredResume(String versionId) {
        return tailoredVersions.get(versionId);
    }

    public synchronized List<TailoredResumeDto> getAllTailoredVersions() {
        return new ArrayList<>(tailoredVersions.values());
    }

    public synchronized void clearAll() {
        this.currentMasterResume = null;
        this.tailoredVersions.clear();
        this.activeTailoredVersionId = null;

        if (sqliteAvailable) {
            try {
                jdbcTemplate.execute("DELETE FROM tailored_resumes");
                jdbcTemplate.execute("DELETE FROM master_resumes");
                log.info("Cleared SQLite database tables.");
            } catch (Exception e) {
                log.warn("Error clearing SQLite tables: {}", e.getMessage());
            }
        }
    }

    private void loadLatestMasterResume() {
        try {
            List<String> jsons = jdbcTemplate.query(
                    "SELECT payload_json FROM master_resumes ORDER BY updated_at DESC LIMIT 1",
                    (rs, rowNum) -> rs.getString("payload_json"));
            if (!jsons.isEmpty()) {
                this.currentMasterResume = objectMapper.readValue(jsons.get(0), MasterResumeDto.class);
                log.info("Loaded previous master resume for candidate: '{}'", currentMasterResume.getFullName());
            }
        } catch (Exception e) {
            log.debug("No existing master resume found in SQLite.");
        }
    }

    /**
     * Generates a realistic synthetic / fictional profile for instant demo and automated testing.
     */
    public MasterResumeDto createSyntheticSampleProfile() {
        MasterResumeDto master = new MasterResumeDto();
        master.setId(UUID.randomUUID().toString());
        master.setOriginalFileName("Alex_Mercer_Fictional_Resume.pdf");
        master.setVerified(true);

        master.setFullName("Alex Mercer");
        master.setHeadline("Software Developer | Full Stack Developer (React · Node.js · PostgreSQL)");
        master.setEmail("alex.mercer.dev@example.com");
        master.setPhone("+1 (555) 234-5678");
        master.setLocation("Seattle, WA");
        master.setLinkedinUrl("https://linkedin.com/in/alex-mercer-demo");
        master.setGithubUrl("https://github.com/alex-mercer-demo");
        master.setPortfolioUrl("https://alexmercer.dev");

        master.setSummary("Full Stack Software Developer with experience designing, building, and shipping production features for high-performance applications. Proficient in JavaScript/TypeScript, React, Node.js, Express.js, and PostgreSQL, with a solid foundation in Data Structures & Algorithms, RESTful API design, authentication (JWT), and performance optimization.");

        master.setSkills(new ArrayList<>(Arrays.asList(
                "JavaScript", "TypeScript", "React", "Next.js 14", "Node.js", "Express.js", "PostgreSQL",
                "Supabase", "MySQL", "REST APIs", "RBAC", "JWT Authentication", "Git", "GitHub", "Docker",
                "Data Structures & Algorithms", "OOP", "System Design Basics", "Java", "Tailwind CSS"
        )));

        // Categorized Skills (exact structure as requested)
        java.util.Map<String, List<String>> catMap = new java.util.LinkedHashMap<>();
        catMap.put("Languages", Arrays.asList("JavaScript", "TypeScript", "Java", "C", "C++"));
        catMap.put("Frontend", Arrays.asList("React.js", "Next.js 14", "HTML5", "CSS3", "Tailwind CSS", "Responsive Design"));
        catMap.put("Backend", Arrays.asList("Node.js", "Express.js", "REST APIs", "Role-Based Access Control (RBAC)", "JWT Authentication"));
        catMap.put("Database & BaaS", Arrays.asList("Supabase", "MySQL", "PostgreSQL", "Database Management Systems (DBMS)"));
        catMap.put("DevOps & Tools", Arrays.asList("Git", "GitHub", "Vercel", "Netlify", "Postman", "VS Code", "basic Docker", "CI/CD concepts"));
        catMap.put("Core CS Concepts", Arrays.asList("Data Structures & Algorithms", "Object-Oriented Programming", "System Design Basics", "Problem Solving"));
        catMap.put("AI-Assisted Development", Arrays.asList("ChatGPT", "GitHub Copilot", "Claude", "Gemini"));
        master.setCategorizedSkills(catMap);

        // Experience 1
        WorkExperienceDto exp1 = new WorkExperienceDto();
        exp1.setId(UUID.randomUUID().toString());
        exp1.setCompany("Apex Cloud Technologies");
        exp1.setJobTitle("Senior Software Engineer");
        exp1.setLocation("Seattle, WA");
        exp1.setStartDate("03/2021");
        exp1.setEndDate("Present");
        exp1.setCurrent(true);
        exp1.setTechnologies(Arrays.asList("Java", "Spring Boot", "Kafka", "PostgreSQL", "Docker", "Kubernetes", "AWS"));
        exp1.setBullets(new ArrayList<>(Arrays.asList(
                "Architected and deployed a multi-tenant event processing microservice handling over 15,000 requests per second with 99.99% uptime.",
                "Spearheaded database query optimization and caching layer with Redis, reducing p99 API latency from 450ms to 65ms.",
                "Engineered end-to-end CI/CD delivery pipelines in GitHub Actions and Kubernetes, decreasing release deployment cycle time by 40%."
        )));

        // Experience 2
        WorkExperienceDto exp2 = new WorkExperienceDto();
        exp2.setId(UUID.randomUUID().toString());
        exp2.setCompany("Beacon Financial Systems");
        exp2.setJobTitle("Software Engineer");
        exp2.setLocation("San Francisco, CA");
        exp2.setStartDate("06/2018");
        exp2.setEndDate("02/2021");
        exp2.setCurrent(false);
        exp2.setTechnologies(Arrays.asList("Java", "Spring", "REST", "MySQL", "Docker", "JUnit"));
        exp2.setBullets(new ArrayList<>(Arrays.asList(
                "Engineered secure payment settlement REST APIs processing over $2.5M in daily financial transactions adhering to PCI-DSS standards.",
                "Implemented comprehensive automated testing suites utilizing JUnit 5 and Mockito, increasing unit and integration test coverage to 88%."
        )));

        master.setWorkExperiences(new ArrayList<>(Arrays.asList(exp1, exp2)));

        // Education
        EducationDto edu = new EducationDto();
        edu.setId(UUID.randomUUID().toString());
        edu.setInstitution("University of Washington");
        edu.setDegree("Bachelor of Science");
        edu.setFieldOfStudy("Computer Science");
        edu.setGraduationDate("05/2018");
        edu.setGpa("3.8");
        edu.setHonors("Magna Cum Laude");
        master.setEducation(new ArrayList<>(Collections.singletonList(edu)));

        // Projects
        ProjectDto proj = new ProjectDto();
        proj.setId(UUID.randomUUID().toString());
        proj.setName("CloudMetrics Pulse");
        proj.setRole("Lead Creator");
        proj.setDescription("Open-source distributed observability agent and analytics dashboard.");
        proj.setTechnologies(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL", "Docker"));
        proj.setUrl("https://github.com/alex-mercer-demo/cloudmetrics-pulse");
        proj.setBullets(new ArrayList<>(Arrays.asList(
                "Built lightweight agent streaming real-time JVM telemetry and metrics over WebSocket.",
                "Adopted by over 1,200 active open-source developers worldwide with 350+ GitHub stars."
        )));
        master.setProjects(new ArrayList<>(Collections.singletonList(proj)));

        // Certifications
        CertificationDto cert = new CertificationDto();
        cert.setId(UUID.randomUUID().toString());
        cert.setName("AWS Certified Solutions Architect - Associate");
        cert.setIssuingOrganization("Amazon Web Services");
        cert.setIssueDate("2023");
        master.setCertifications(new ArrayList<>(Collections.singletonList(cert)));

        master.setAchievements(new ArrayList<>(Arrays.asList(
                "Ranked in Top 10 in multiple competitive hackathons.",
                "Active open-source contributor with consistent public GitHub repositories and contributions.",
                "Strong technical community presence with active engineering articles and knowledge sharing."
        )));

        master.setActivities(new ArrayList<>(Arrays.asList(
                "Competitive Programming — actively participates in algorithmic coding competitions.",
                "Open Source Contributions — contributes to community-driven web and database projects.",
                "Event Management & Leadership — leads and organizes technical workshops and hackathons.",
                "Content Creation — produces technical documentation and tutorials for engineering peers."
        )));

        master.setSoftSkills(new ArrayList<>(Arrays.asList(
                "Leadership", "Team Collaboration", "Communication", "Problem Solving", "Quick Learner", "Time Management"
        )));

        saveMasterResume(master);
        return master;
    }
}
