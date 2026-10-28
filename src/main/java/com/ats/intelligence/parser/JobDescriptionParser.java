package com.ats.intelligence.parser;

import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.engine.KeywordNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JobDescriptionParser {

    private final KeywordNormalizer normalizer;

    // Standard technical terms dictionary for precision extraction
    private static final Set<String> KNOWN_TECH_KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            // Languages
            "Java", "Python", "JavaScript", "TypeScript", "Go", "C++", "C#", "Kotlin", "Swift", "Rust", "SQL", "Scala", "Ruby", "PHP", "HTML5", "CSS3",
            // Frameworks & Libraries
            "Spring Boot", "Spring", "Spring MVC", "Spring Security", "React", "Angular", "Vue.js", "Node.js", "Express", "Django", "Flask", "FastAPI",
            ".NET", "ASP.NET", "Hibernate", "JPA", "Next.js", "Tailwind CSS", "Bootstrap",
            // Cloud & Infrastructure
            "AWS", "Azure", "GCP", "Google Cloud", "Docker", "Kubernetes", "Terraform", "CI/CD", "Jenkins", "GitHub Actions", "GitLab CI", "Linux", "Ansible",
            // Databases & Messaging
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Cassandra", "Oracle", "SQLite", "Elasticsearch", "DynamoDB", "Kafka", "RabbitMQ",
            // Architecture & Practices
            "REST", "RESTful API", "GraphQL", "Microservices", "System Design", "Agile", "Scrum", "TDD", "Unit Testing", "Clean Architecture",
            "Event-Driven Architecture", "OOP", "Maven", "Gradle", "Git"
    ));

    private static final Pattern YEARS_EXP_PATTERN = Pattern.compile(
            "\\b(\\d+\\+?\\s*(?:to\\s*\\d+)?\\s*(?:years?|yrs?)(?:\\s+of)?\\s+(?:experience|exp)[^.;,\\n]*)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "\\b((?:Bachelor|Master|B\\.S|M\\.S|Ph\\.D|degree)\\s+(?:in\\s+)?(?:Computer Science|Software Engineering|Engineering|related field)?[^.;,\\n]*)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ROLE_HINT_PATTERN = Pattern.compile(
            "(?i)\\b(?:title|role|position|looking for a|seeking a|hiring a)\\s*[:\\-]?\\s*([A-Z][A-Za-z0-9\\s/\\-]+(?:Developer|Engineer|Architect|Manager|Lead|Analyst|Consultant))\\b");

    private static final Pattern COMPANY_HINT_PATTERN = Pattern.compile(
            "(?i)\\b(?:at|company)\\s*[:\\-]?\\s*([A-Z][A-Za-z0-9&., ]{2,30})\\b");

    public JobDescriptionParser(KeywordNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public JobDescriptionDto parse(String rawText, String manualRole, String manualCompany, String preferredLength) {
        JobDescriptionDto dto = new JobDescriptionDto();
        dto.setRawText(rawText);
        dto.setPreferredLength(preferredLength != null && !preferredLength.isBlank() ? preferredLength : "AUTO");

        if (rawText == null || rawText.isBlank()) {
            dto.setTargetRole(manualRole);
            dto.setTargetCompany(manualCompany);
            return dto;
        }

        // Target Role & Company
        dto.setTargetRole(manualRole != null && !manualRole.isBlank() ? manualRole.trim() : inferTargetRole(rawText));
        dto.setTargetCompany(manualCompany != null && !manualCompany.isBlank() ? manualCompany.trim() : inferTargetCompany(rawText));

        // Segment text into sections
        String[] lines = rawText.split("\\r?\\n");
        Set<String> requiredSet = new LinkedHashSet<>();
        Set<String> preferredSet = new LinkedHashSet<>();
        Set<String> responsibilitiesSet = new LinkedHashSet<>();
        Set<String> toolsSet = new LinkedHashSet<>();
        Set<String> eduExpSet = new LinkedHashSet<>();
        Set<String> domainSet = new LinkedHashSet<>();

        SectionScope currentScope = SectionScope.GENERAL;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            SectionScope detectedScope = detectScope(line);
            if (detectedScope != SectionScope.GENERAL) {
                currentScope = detectedScope;
                continue;
            }

            // Extract Years of Experience and Degree Requirements
            Matcher expMatcher = YEARS_EXP_PATTERN.matcher(line);
            if (expMatcher.find()) {
                eduExpSet.add(cleanItem(expMatcher.group(1)));
            }
            Matcher degMatcher = DEGREE_PATTERN.matcher(line);
            if (degMatcher.find()) {
                eduExpSet.add(cleanItem(degMatcher.group(1)));
            }

            // Check known tech terms
            for (String kw : KNOWN_TECH_KEYWORDS) {
                if (normalizer.matches(kw, line)) {
                    if (currentScope == SectionScope.PREFERRED) {
                        preferredSet.add(kw);
                    } else if (currentScope == SectionScope.TOOLS) {
                        toolsSet.add(kw);
                    } else {
                        requiredSet.add(kw);
                    }
                }
            }

            // Categorize bullet points
            if (isBullet(line)) {
                String bulletText = cleanItem(line);
                if (bulletText.length() > 10) {
                    if (currentScope == SectionScope.RESPONSIBILITIES) {
                        responsibilitiesSet.add(bulletText);
                    } else if (currentScope == SectionScope.REQUIRED) {
                        if (bulletText.length() < 60) {
                            requiredSet.add(bulletText);
                        } else {
                            responsibilitiesSet.add(bulletText);
                        }
                    } else if (currentScope == SectionScope.PREFERRED) {
                        preferredSet.add(bulletText);
                    } else if (currentScope == SectionScope.TOOLS) {
                        toolsSet.add(bulletText);
                    }
                }
            }
        }

        // Domain terms extraction (phrases like "distributed systems", "high availability", "scalability", "cloud native")
        extractDomainTerms(rawText, domainSet);

        // Fallback: If no required skills were detected via section tags, search the entire text for known tech terms
        if (requiredSet.isEmpty()) {
            for (String kw : KNOWN_TECH_KEYWORDS) {
                if (normalizer.matches(kw, rawText)) {
                    requiredSet.add(kw);
                }
            }
        }

        dto.setRequiredSkills(new ArrayList<>(requiredSet));
        dto.setPreferredSkills(new ArrayList<>(preferredSet));
        dto.setResponsibilities(new ArrayList<>(responsibilitiesSet));
        dto.setToolsAndPlatforms(new ArrayList<>(toolsSet));
        dto.setEducationAndExperience(new ArrayList<>(eduExpSet));
        dto.setDomainTerms(new ArrayList<>(domainSet));

        return dto;
    }

    private String inferTargetRole(String text) {
        Matcher matcher = ROLE_HINT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Check first line
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < Math.min(lines.length, 3); i++) {
            String l = lines[i].trim();
            if (l.toLowerCase().contains("developer") || l.toLowerCase().contains("engineer") || l.toLowerCase().contains("architect")) {
                return l.replaceAll("(?i)^(job title|role|position)\\s*[:\\-]?\\s*", "").trim();
            }
        }
        return "Software Engineer";
    }

    private String inferTargetCompany(String text) {
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < Math.min(lines.length, 6); i++) {
            String l = lines[i].trim();
            if (l.toLowerCase().startsWith("company:") || l.toLowerCase().startsWith("company -")) {
                return l.replaceAll("(?i)^company\\s*[:\\-]?\\s*", "").trim();
            }
        }
        Matcher matcher = COMPANY_HINT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Target Company";
    }

    private enum SectionScope {
        GENERAL,
        REQUIRED,
        PREFERRED,
        RESPONSIBILITIES,
        TOOLS
    }

    private SectionScope detectScope(String line) {
        String lower = line.toLowerCase().replaceAll("[^a-z\\s]", "").trim();
        if (lower.contains("preferred") || lower.contains("nice to have") || lower.contains("bonus")
                || lower.contains("desired") || lower.contains("plus")) {
            return SectionScope.PREFERRED;
        }
        if (lower.contains("requirement") || lower.contains("qualifications") || lower.contains("must have")
                || lower.contains("basic qualification") || lower.contains("what you bring") || lower.contains("skills required")) {
            return SectionScope.REQUIRED;
        }
        if (lower.contains("responsibilities") || lower.contains("what you will do") || lower.contains("duties")
                || lower.contains("day to day") || lower.contains("the role")) {
            return SectionScope.RESPONSIBILITIES;
        }
        if (lower.contains("tech stack") || lower.contains("technologies") || lower.contains("tools") || lower.contains("environment")) {
            return SectionScope.TOOLS;
        }
        return SectionScope.GENERAL;
    }

    private void extractDomainTerms(String text, Set<String> domainSet) {
        List<String> terms = Arrays.asList(
                "Distributed Systems", "High Availability", "Microservices Architecture", "Cloud Native",
                "Scalability", "System Design", "Performance Optimization", "Data Pipelines",
                "Event-Driven", "RESTful Web Services", "Concurrency", "Multithreading",
                "Security Best Practices", "Database Design", "Fault Tolerance", "Observability"
        );
        for (String term : terms) {
            if (normalizer.matches(term, text)) {
                domainSet.add(term);
            }
        }
    }

    private boolean isBullet(String line) {
        return line.matches("^\\s*[-*•·–]\\s+.*") || line.matches("^\\s*\\d+[.)]\\s+.*");
    }

    private String cleanItem(String line) {
        return line.replaceAll("^\\s*[-*•·–]\\s+", "")
                .replaceAll("^\\s*\\d+[.)]\\s+", "")
                .trim();
    }
}
