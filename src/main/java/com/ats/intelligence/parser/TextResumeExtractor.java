package com.ats.intelligence.parser;

import com.ats.intelligence.dto.CertificationDto;
import com.ats.intelligence.dto.EducationDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.ProjectDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TextResumeExtractor {

    private static final Logger log = LoggerFactory.getLogger(TextResumeExtractor.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "(?:https?:\\/\\/)?(?:www\\.)?linkedin\\.com\\/in\\/[a-zA-Z0-9_-]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "(?:https?:\\/\\/)?(?:www\\.)?github\\.com\\/[a-zA-Z0-9_-]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?:\\/\\/(?:www\\.)?[a-zA-Z0-9._-]+\\.[a-zA-Z]{2,}(?:\\/[^\\s]*)?", Pattern.CASE_INSENSITIVE);

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z\\s.-]+,\\s*(?:[A-Z]{2}|[A-Z][a-zA-Z]+))\\b");

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "\\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)?\\.?\\s*\\d{4}|\\d{1,2}\\/\\d{4})\\s*(?:–|-|to)\\s*((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)?\\.?\\s*\\d{4}|\\d{1,2}\\/\\d{4}|Present|Current)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "\\b(Bachelor(?:'s)?(?:\\s+of\\s+[A-Za-z]+)?|Master(?:'s)?(?:\\s+of\\s+[A-Za-z]+)?|B\\.S\\.?|M\\.S\\.?|B\\.A\\.?|M\\.A\\.?|Ph\\.?D\\.?|B\\.Tech|M\\.Tech|Associate(?:'s)?)\\b",
            Pattern.CASE_INSENSITIVE);

    private enum SectionType {
        SUMMARY,
        EXPERIENCE,
        EDUCATION,
        SKILLS,
        PROJECTS,
        CERTIFICATIONS,
        ACHIEVEMENTS,
        ACTIVITIES,
        SOFT_SKILLS,
        UNKNOWN
    }

    public MasterResumeDto extractProfile(String rawText, String originalFileName) {
        MasterResumeDto profile = new MasterResumeDto();
        profile.setOriginalFileName(originalFileName);

        if (rawText == null || rawText.isBlank()) {
            return profile;
        }

        String[] lines = rawText.split("\\r?\\n");

        // 1. Contact Information
        extractContactInfo(lines, rawText, profile);

        // 2. Identify Sections
        Map<SectionType, List<String>> sectionBlocks = segmentSections(lines);

        // 3. Parse Professional Summary
        if (sectionBlocks.containsKey(SectionType.SUMMARY)) {
            String summary = String.join(" ", sectionBlocks.get(SectionType.SUMMARY)).trim();
            profile.setSummary(summary);
        }

        // 4. Parse Skills (both categorized and flat list)
        if (sectionBlocks.containsKey(SectionType.SKILLS)) {
            List<String> rawSkillsLines = sectionBlocks.get(SectionType.SKILLS);
            List<String> skills = parseSkills(rawSkillsLines);
            profile.setSkills(skills);
            profile.setCategorizedSkills(parseCategorizedSkills(rawSkillsLines));
        }

        // 5. Parse Work Experience
        if (sectionBlocks.containsKey(SectionType.EXPERIENCE)) {
            List<WorkExperienceDto> experiences = parseExperience(sectionBlocks.get(SectionType.EXPERIENCE));
            profile.setWorkExperiences(experiences);
        }

        // 6. Parse Education
        if (sectionBlocks.containsKey(SectionType.EDUCATION)) {
            List<EducationDto> education = parseEducation(sectionBlocks.get(SectionType.EDUCATION));
            profile.setEducation(education);
        }

        // 7. Parse Projects
        if (sectionBlocks.containsKey(SectionType.PROJECTS)) {
            List<ProjectDto> projects = parseProjects(sectionBlocks.get(SectionType.PROJECTS));
            profile.setProjects(projects);
        }

        // 8. Parse Certifications
        if (sectionBlocks.containsKey(SectionType.CERTIFICATIONS)) {
            List<CertificationDto> certs = parseCertifications(sectionBlocks.get(SectionType.CERTIFICATIONS));
            profile.setCertifications(certs);
        }

        // 9. Parse Achievements
        if (sectionBlocks.containsKey(SectionType.ACHIEVEMENTS)) {
            List<String> achievements = parseBulletPoints(sectionBlocks.get(SectionType.ACHIEVEMENTS));
            profile.setAchievements(achievements);
        }

        // 10. Parse Activities & Leadership
        if (sectionBlocks.containsKey(SectionType.ACTIVITIES)) {
            List<String> activities = parseBulletPoints(sectionBlocks.get(SectionType.ACTIVITIES));
            profile.setActivities(activities);
        }

        // 11. Parse Soft Skills
        if (sectionBlocks.containsKey(SectionType.SOFT_SKILLS)) {
            List<String> soft = parseSoftSkills(sectionBlocks.get(SectionType.SOFT_SKILLS));
            profile.setSoftSkills(soft);
        }

        return profile;
    }

    private void extractContactInfo(String[] lines, String fullText, MasterResumeDto profile) {
        // Full Name: Usually first 1-3 non-empty lines
        int nameLineIdx = -1;
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("resume") || line.equalsIgnoreCase("curriculum vitae") || line.equalsIgnoreCase("cv")) {
                continue;
            }
            if (EMAIL_PATTERN.matcher(line).find() || PHONE_PATTERN.matcher(line).find() || URL_PATTERN.matcher(line).find()) {
                continue;
            }
            if (line.length() >= 3 && line.length() <= 50 && line.matches("^[A-Za-z\\s.'-]+$")) {
                profile.setFullName(line);
                nameLineIdx = i;
                break;
            }
        }

        // Headline / Title: often immediately follows full name (e.g. "Software Developer | Full Stack Developer")
        if (nameLineIdx >= 0 && nameLineIdx + 1 < lines.length) {
            String candidateHeadline = lines[nameLineIdx + 1].trim();
            if (!candidateHeadline.isEmpty()
                    && !EMAIL_PATTERN.matcher(candidateHeadline).find()
                    && !PHONE_PATTERN.matcher(candidateHeadline).find()
                    && (candidateHeadline.contains("|") || candidateHeadline.contains("·") || candidateHeadline.toLowerCase().contains("developer") || candidateHeadline.toLowerCase().contains("engineer"))) {
                profile.setHeadline(candidateHeadline);
            }
        }

        // Email
        Matcher emailMatcher = EMAIL_PATTERN.matcher(fullText);
        if (emailMatcher.find()) {
            profile.setEmail(emailMatcher.group());
        }

        // Phone
        Matcher phoneMatcher = PHONE_PATTERN.matcher(fullText);
        if (phoneMatcher.find()) {
            profile.setPhone(phoneMatcher.group());
        }

        // LinkedIn
        Matcher linkedinMatcher = LINKEDIN_PATTERN.matcher(fullText);
        if (linkedinMatcher.find()) {
            profile.setLinkedinUrl(linkedinMatcher.group());
        }

        // GitHub
        Matcher githubMatcher = GITHUB_PATTERN.matcher(fullText);
        if (githubMatcher.find()) {
            profile.setGithubUrl(githubMatcher.group());
        }

        // Location - check first 8 lines
        for (int i = 0; i < Math.min(lines.length, 8); i++) {
            Matcher locMatcher = LOCATION_PATTERN.matcher(lines[i]);
            if (locMatcher.find()) {
                String locCandidate = locMatcher.group(1).trim();
                // Ensure it's not a sentence
                if (!locCandidate.toLowerCase().contains("experience") && !locCandidate.toLowerCase().contains("developer")) {
                    profile.setLocation(locCandidate);
                    break;
                }
            }
        }
    }

    private Map<SectionType, List<String>> segmentSections(String[] lines) {
        Map<SectionType, List<String>> sections = new LinkedHashMap<>();
        SectionType currentSection = SectionType.UNKNOWN;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            SectionType detected = detectSectionHeader(line);
            if (detected != SectionType.UNKNOWN) {
                currentSection = detected;
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else if (currentSection != SectionType.UNKNOWN) {
                sections.get(currentSection).add(line);
            }
        }
        return sections;
    }

    private SectionType detectSectionHeader(String line) {
        String clean = line.toUpperCase().replaceAll("[^A-Z\\s&/]", "").trim();
        if (clean.length() > 40) {
            return SectionType.UNKNOWN;
        }

        if (clean.equals("SUMMARY") || clean.equals("PROFESSIONAL SUMMARY") || clean.equals("EXECUTIVE SUMMARY")
                || clean.equals("ABOUT ME") || clean.equals("CAREER OBJECTIVE") || clean.equals("PROFILE")) {
            return SectionType.SUMMARY;
        }
        if (clean.equals("EXPERIENCE") || clean.equals("WORK EXPERIENCE") || clean.equals("PROFESSIONAL EXPERIENCE")
                || clean.equals("EMPLOYMENT HISTORY") || clean.equals("WORK HISTORY") || clean.equals("CAREER HISTORY")) {
            return SectionType.EXPERIENCE;
        }
        if (clean.equals("SKILLS") || clean.equals("TECHNICAL SKILLS") || clean.equals("CORE COMPETENCIES")
                || clean.equals("SKILLS AND EXPERTISE") || clean.equals("SKILLS & TOOLS") || clean.equals("AREAS OF EXPERTISE")) {
            return SectionType.SKILLS;
        }
        if (clean.equals("EDUCATION") || clean.equals("ACADEMIC BACKGROUND") || clean.equals("ACADEMICS")
                || clean.equals("EDUCATION AND TRAINING") || clean.equals("DEGREES")) {
            return SectionType.EDUCATION;
        }
        if (clean.equals("PROJECTS") || clean.equals("PERSONAL PROJECTS") || clean.equals("KEY PROJECTS")
                || clean.equals("ACADEMIC PROJECTS") || clean.equals("TECHNICAL PROJECTS")) {
            return SectionType.PROJECTS;
        }
        if (clean.equals("CERTIFICATIONS") || clean.equals("CERTIFICATES") || clean.equals("LICENSES & CERTIFICATIONS")
                || clean.equals("PROFESSIONAL CERTIFICATIONS")) {
            return SectionType.CERTIFICATIONS;
        }
        if (clean.equals("ACHIEVEMENTS") || clean.equals("AWARDS") || clean.equals("HONORS & AWARDS")
                || clean.equals("PUBLICATIONS") || clean.equals("RECOGNITION")) {
            return SectionType.ACHIEVEMENTS;
        }
        if (clean.contains("ACTIVITIES") || clean.contains("LEADERSHIP") || clean.contains("COMMUNITY INVOLVEMENT")
                || clean.contains("EXTRA CURRICULAR") || clean.contains("EXTRACURRICULAR")) {
            return SectionType.ACTIVITIES;
        }
        if (clean.equals("SOFT SKILLS") || clean.equals("INTERPERSONAL SKILLS") || clean.equals("STRENGTHS")) {
            return SectionType.SOFT_SKILLS;
        }
        return SectionType.UNKNOWN;
    }

    private Map<String, List<String>> parseCategorizedSkills(List<String> lines) {
        Map<String, List<String>> categorized = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                String catName = parts[0].replaceAll("^[-*•·\\s]+", "").trim();
                String[] tokens = parts[1].split("[,|•·;\\t]+");
                List<String> skillsInCat = new ArrayList<>();
                for (String t : tokens) {
                    String s = t.trim().replaceAll("^[-*•·\\s]+", "");
                    if (s.length() >= 2 && !skillsInCat.contains(s)) {
                        skillsInCat.add(s);
                    }
                }
                if (!skillsInCat.isEmpty()) {
                    categorized.put(catName, skillsInCat);
                }
            }
        }
        return categorized;
    }

    private List<String> parseSoftSkills(List<String> lines) {
        List<String> soft = new ArrayList<>();
        for (String line : lines) {
            String[] tokens = line.split("[·|•,;]+");
            for (String tok : tokens) {
                String s = tok.trim().replaceAll("^[-*•·\\s]+", "");
                if (s.length() >= 2 && !soft.contains(s)) {
                    soft.add(s);
                }
            }
        }
        return soft;
    }

    private List<String> parseSkills(List<String> lines) {
        List<String> skills = new ArrayList<>();
        for (String line : lines) {
            // Remove common category prefixes like "Languages:", "Frameworks:", "Tools:"
            String cleaned = line.replaceFirst("^[A-Za-z\\s&/]+:\\s*", "");
            String[] tokens = cleaned.split("[,|•·;\\t]+");
            for (String token : tokens) {
                String skill = token.trim().replaceAll("^[-*•·\\s]+", "");
                if (skill.length() >= 2 && skill.length() <= 40 && !skills.contains(skill)) {
                    skills.add(skill);
                }
            }
        }
        return skills;
    }

    private List<WorkExperienceDto> parseExperience(List<String> lines) {
        List<WorkExperienceDto> list = new ArrayList<>();
        WorkExperienceDto current = null;

        for (String line : lines) {
            Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(line);
            boolean hasDate = dateMatcher.find();

            if (current != null && current.getStartDate() == null && hasDate && !isJobHeaderLine(line)) {
                // This line provides the dates for the current role
                current.setStartDate(dateMatcher.group(1));
                current.setEndDate(dateMatcher.group(2));
                if ("Present".equalsIgnoreCase(current.getEndDate()) || "Current".equalsIgnoreCase(current.getEndDate())) {
                    current.setCurrent(true);
                }
            } else if (hasDate || isJobHeaderLine(line)) {
                if (current != null && (current.getCompany() != null || current.getJobTitle() != null)) {
                    list.add(current);
                }
                current = new WorkExperienceDto();

                if (hasDate) {
                    current.setStartDate(dateMatcher.group(1));
                    current.setEndDate(dateMatcher.group(2));
                    if ("Present".equalsIgnoreCase(current.getEndDate()) || "Current".equalsIgnoreCase(current.getEndDate())) {
                        current.setCurrent(true);
                    }
                    // Strip the date portion to extract company/title
                    String remaining = line.substring(0, dateMatcher.start()) + line.substring(dateMatcher.end());
                    assignJobDetails(remaining.trim(), current);
                } else {
                    assignJobDetails(line, current);
                }
            } else if (current != null) {
                if (isBulletLine(line)) {
                    current.getBullets().add(cleanBullet(line));
                } else if (current.getBullets().isEmpty() && (current.getCompany() == null || current.getJobTitle() == null)) {
                    assignJobDetails(line, current);
                } else if (!current.getBullets().isEmpty()) {
                    // Append wrapped line to last bullet
                    int lastIdx = current.getBullets().size() - 1;
                    String merged = current.getBullets().get(lastIdx) + " " + line;
                    current.getBullets().set(lastIdx, merged);
                }
            }
        }

        if (current != null && (current.getCompany() != null || current.getJobTitle() != null || !current.getBullets().isEmpty())) {
            list.add(current);
        }

        return list;
    }

    private void assignJobDetails(String text, WorkExperienceDto exp) {
        String clean = text.replaceAll("[|•·,-]+$", "").trim();
        if (clean.contains(" at ")) {
            String[] parts = clean.split(" at ", 2);
            exp.setJobTitle(parts[0].trim());
            exp.setCompany(parts[1].trim());
        } else if (clean.contains(" - ")) {
            String[] parts = clean.split(" - ", 2);
            if (exp.getJobTitle() == null) exp.setJobTitle(parts[0].trim());
            if (exp.getCompany() == null) exp.setCompany(parts[1].trim());
        } else if (clean.contains(" | ")) {
            String[] parts = clean.split(" \\| ", 2);
            if (exp.getJobTitle() == null) exp.setJobTitle(parts[0].trim());
            if (exp.getCompany() == null) exp.setCompany(parts[1].trim());
        } else {
            if (exp.getCompany() == null) {
                exp.setCompany(clean);
            } else if (exp.getJobTitle() == null) {
                exp.setJobTitle(clean);
            }
        }
    }

    private boolean isJobHeaderLine(String line) {
        String trimmed = line.trim();
        // If the line consists only of dates, it is not a job title/company header
        if (DATE_RANGE_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        String stripped = DATE_RANGE_PATTERN.matcher(trimmed).replaceAll("").trim();
        if (stripped.isEmpty() || stripped.equals("-") || stripped.equals("–")) {
            return false;
        }
        return line.contains(" at ") || line.contains(" - ") || line.contains(" | ");
    }

    private List<EducationDto> parseEducation(List<String> lines) {
        List<EducationDto> list = new ArrayList<>();
        EducationDto current = null;

        for (String line : lines) {
            Matcher degreeMatcher = DEGREE_PATTERN.matcher(line);
            Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(line);
            boolean hasDegree = degreeMatcher.find();
            boolean hasDates = dateMatcher.find();

            if (hasDegree || line.toLowerCase().contains("university") || line.toLowerCase().contains("college") || line.toLowerCase().contains("institute")) {
                if (current != null) {
                    list.add(current);
                }
                current = new EducationDto();

                if (hasDegree) {
                    current.setDegree(degreeMatcher.group(1));
                    String afterDegree = line.substring(degreeMatcher.end()).trim();
                    if (afterDegree.toLowerCase().startsWith("in ")) {
                        String majorCandidate = afterDegree.substring(3).replaceAll("(?i)(-|–|at|from|,).*$", "").trim();
                        current.setFieldOfStudy(majorCandidate);
                    }
                }

                if (hasDates) {
                    current.setGraduationDate(dateMatcher.group(2) != null ? dateMatcher.group(2) : dateMatcher.group(1));
                }

                if (line.toLowerCase().contains("university") || line.toLowerCase().contains("college") || line.toLowerCase().contains("institute")) {
                    current.setInstitution(line.replaceAll("(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|\\d{4}|-|–).*", "").trim());
                }
            } else if (current != null) {
                if (line.toLowerCase().contains("gpa")) {
                    current.setGpa(line.replaceAll("(?i).*gpa\\s*[:=]?\\s*", "").trim());
                } else if (current.getInstitution() == null && (line.toLowerCase().contains("university") || line.toLowerCase().contains("college"))) {
                    current.setInstitution(line.trim());
                } else if (current.getFieldOfStudy() == null) {
                    current.setFieldOfStudy(line.trim());
                }
            }
        }

        if (current != null) {
            list.add(current);
        }
        return list;
    }

    private List<ProjectDto> parseProjects(List<String> lines) {
        List<ProjectDto> list = new ArrayList<>();
        ProjectDto current = null;

        for (String line : lines) {
            if (isBulletLine(line)) {
                if (current != null) {
                    current.getBullets().add(cleanBullet(line));
                }
            } else if (line.length() < 60 && !line.isBlank()) {
                if (current != null) {
                    list.add(current);
                }
                current = new ProjectDto();
                if (line.contains(" | ")) {
                    String[] parts = line.split(" \\| ", 2);
                    current.setName(parts[0].trim());
                    String techStr = parts[1].trim();
                    current.setTechnologies(Arrays.stream(techStr.split("[,;]+")).map(String::trim).collect(Collectors.toList()));
                } else if (line.contains(" - ")) {
                    String[] parts = line.split(" - ", 2);
                    current.setName(parts[0].trim());
                    current.setDescription(parts[1].trim());
                } else {
                    current.setName(line.trim());
                }
            } else if (current != null) {
                if (current.getDescription() == null) {
                    current.setDescription(line.trim());
                } else {
                    current.getBullets().add(line.trim());
                }
            }
        }
        if (current != null) {
            list.add(current);
        }
        return list;
    }

    private List<CertificationDto> parseCertifications(List<String> lines) {
        List<CertificationDto> list = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            String clean = cleanBullet(line);
            CertificationDto cert = new CertificationDto();
            if (clean.contains(" - ")) {
                String[] parts = clean.split(" - ", 2);
                cert.setName(parts[0].trim());
                cert.setIssuingOrganization(parts[1].trim());
            } else {
                cert.setName(clean);
            }
            list.add(cert);
        }
        return list;
    }

    private List<String> parseBulletPoints(List<String> lines) {
        List<String> bullets = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            bullets.add(cleanBullet(line));
        }
        return bullets;
    }

    private boolean isBulletLine(String line) {
        return line.matches("^\\s*[-*•·–]\\s+.*") || line.matches("^\\s*\\d+[.)]\\s+.*");
    }

    private String cleanBullet(String line) {
        return line.replaceAll("^\\s*[-*•·–]\\s+", "").replaceAll("^\\s*\\d+[.)]\\s+", "").trim();
    }
}
