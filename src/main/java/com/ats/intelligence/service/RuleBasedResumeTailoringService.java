package com.ats.intelligence.service;

import com.ats.intelligence.dto.EducationDto;
import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.MatchReportDto;
import com.ats.intelligence.dto.ProjectDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import com.ats.intelligence.engine.AtsScoringEngine;
import com.ats.intelligence.engine.FactPreservationValidator;
import com.ats.intelligence.engine.KeywordNormalizer;
import com.ats.intelligence.model.SkillSupportLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service("ruleBasedResumeTailoringService")
public class RuleBasedResumeTailoringService implements ResumeTailoringService {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedResumeTailoringService.class);

    private final KeywordNormalizer normalizer;
    private final AtsScoringEngine scoringEngine;
    private final FactPreservationValidator factValidator;

    private static final Map<Pattern, String> ACTION_VERB_REPLACEMENTS = new HashMap<>();

    static {
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:responsible for developing|responsible for building)\\s+"), "Developed and implemented ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:responsible for managing|responsible for leading)\\s+"), "Spearheaded and managed ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:responsible for designing|responsible for creating)\\s+"), "Architected and designed ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:responsible for)\\s+"), "Engineered and executed ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:worked on building|worked on developing)\\s+"), "Engineered ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:worked on)\\s+"), "Developed and enhanced ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:helped with|helped to)\\s+"), "Collaborated to implement ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:assisted in|assisted with)\\s+"), "Facilitated and contributed to ");
        ACTION_VERB_REPLACEMENTS.put(Pattern.compile("(?i)^\\s*(?:handled)\\s+"), "Managed and maintained ");
    }

    public RuleBasedResumeTailoringService(KeywordNormalizer normalizer,
                                           AtsScoringEngine scoringEngine,
                                           FactPreservationValidator factValidator) {
        this.normalizer = normalizer;
        this.scoringEngine = scoringEngine;
        this.factValidator = factValidator;
    }

    @Override
    public String getEngineName() {
        return "RULE_BASED";
    }

    @Override
    public TailoredResumeDto tailor(MasterResumeDto master, JobDescriptionDto jd) {
        log.info("Starting deterministic rule-based tailoring for candidate: '{}', target role: '{}'",
                master.getFullName(), jd.getTargetRole());

        // 1. Evaluate ATS Match Report first to identify matched & missing requirements
        MatchReportDto matchReport = scoringEngine.evaluateMatch(master, jd);

        TailoredResumeDto tailored = new TailoredResumeDto();
        tailored.setVersionId(UUID.randomUUID().toString());
        tailored.setCreatedAt(LocalDateTime.now().toString());
        tailored.setGeneratorEngine(getEngineName());
        tailored.setJobDescription(jd);
        tailored.setMatchReport(matchReport);

        // 2. Strict Contact Information Preservation
        tailored.setFullName(master.getFullName());
        tailored.setHeadline(master.getHeadline());
        tailored.setEmail(master.getEmail());
        tailored.setPhone(master.getPhone());
        tailored.setLocation(master.getLocation());
        tailored.setLinkedinUrl(master.getLinkedinUrl());
        tailored.setGithubUrl(master.getGithubUrl());
        tailored.setPortfolioUrl(master.getPortfolioUrl());
        tailored.setTargetRole(jd.getTargetRole());
        tailored.setTargetCompany(jd.getTargetCompany());

        // 3. Find Verified Skills matching JD
        Set<String> verifiedMatchedSkills = new LinkedHashSet<>();
        matchReport.getRequirementMatches().stream()
                .filter(m -> m.getStatus() == SkillSupportLevel.VERIFIED)
                .forEach(m -> verifiedMatchedSkills.add(m.getRequirement()));

        // 4. Tailor Technical Skills Section (both flat and categorized)
        tailored.setTailoredSkills(tailorSkills(master.getSkills(), verifiedMatchedSkills));
        tailored.setTailoredCategorizedSkills(tailorCategorizedSkills(master.getCategorizedSkills(), verifiedMatchedSkills));

        // 5. Tailor Professional Summary
        tailored.setTailoredSummary(tailorSummary(master, jd, verifiedMatchedSkills));

        // 6. Tailor Work Experience (Re-order bullets by JD keyword score & strengthen action verbs)
        tailored.setTailoredExperiences(tailorExperiences(master.getWorkExperiences(), jd));

        // 7. Tailor Projects (Prioritize projects utilizing JD technologies)
        tailored.setTailoredProjects(tailorProjects(master.getProjects(), jd));

        // 8. Copy Verified Credentials & Sections Exactly
        tailored.setEducation(master.getEducation() != null ? new ArrayList<>(master.getEducation()) : new ArrayList<>());
        tailored.setCertifications(master.getCertifications() != null ? new ArrayList<>(master.getCertifications()) : new ArrayList<>());
        tailored.setAchievements(master.getAchievements() != null ? new ArrayList<>(master.getAchievements()) : new ArrayList<>());
        tailored.setActivities(master.getActivities() != null ? new ArrayList<>(master.getActivities()) : new ArrayList<>());
        tailored.setSoftSkills(master.getSoftSkills() != null ? new ArrayList<>(master.getSoftSkills()) : new ArrayList<>());

        // 9. Run Strict Fact-Preservation Validation & Sanitization
        factValidator.validateAndSanitize(master, tailored);

        log.info("Rule-based tailoring complete. ATS score: {}%, 0 hallucinations permitted.", matchReport.getOverallScore());
        return tailored;
    }

    private List<String> tailorSkills(List<String> masterSkills, Set<String> verifiedMatchedSkills) {
        if (masterSkills == null) return new ArrayList<>();

        List<String> prioritized = new ArrayList<>();
        List<String> secondary = new ArrayList<>();

        // Group master skills: those matching JD verified skills come first
        for (String skill : masterSkills) {
            boolean matched = false;
            for (String vm : verifiedMatchedSkills) {
                if (normalizer.matches(vm, skill)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                if (!prioritized.contains(skill)) prioritized.add(skill);
            } else {
                if (!secondary.contains(skill)) secondary.add(skill);
            }
        }

        List<String> combined = new ArrayList<>(prioritized);
        combined.addAll(secondary);
        return combined;
    }

    private java.util.Map<String, List<String>> tailorCategorizedSkills(java.util.Map<String, List<String>> masterCategorized, Set<String> verifiedMatchedSkills) {
        if (masterCategorized == null || masterCategorized.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }
        java.util.Map<String, List<String>> tailoredMap = new java.util.LinkedHashMap<>();
        for (var entry : masterCategorized.entrySet()) {
            List<String> prioritized = tailorSkills(entry.getValue(), verifiedMatchedSkills);
            tailoredMap.put(entry.getKey(), prioritized);
        }
        return tailoredMap;
    }

    private String tailorSummary(MasterResumeDto master, JobDescriptionDto jd, Set<String> verifiedMatchedSkills) {
        String baseTitle = "Experienced Professional";
        if (master.getWorkExperiences() != null && !master.getWorkExperiences().isEmpty()) {
            String recentTitle = master.getWorkExperiences().get(0).getJobTitle();
            if (recentTitle != null && !recentTitle.isBlank()) {
                baseTitle = recentTitle;
            }
        }

        List<String> topSkills = new ArrayList<>(verifiedMatchedSkills);
        String skillsHighlight = topSkills.isEmpty() ?
                (master.getSkills() != null && !master.getSkills().isEmpty() ? String.join(", ", master.getSkills().subList(0, Math.min(3, master.getSkills().size()))) : "software engineering") :
                String.join(", ", topSkills.subList(0, Math.min(4, topSkills.size())));

        StringBuilder sb = new StringBuilder();
        if (master.getSummary() != null && !master.getSummary().isBlank()) {
            // Enhance existing summary with target alignment
            sb.append(master.getSummary().trim());
            if (!sb.toString().endsWith(".")) {
                sb.append(".");
            }
            sb.append(String.format(" Targeted for the %s role, bringing verified proficiencies in %s.",
                    jd.getTargetRole() != null ? jd.getTargetRole() : baseTitle, skillsHighlight));
        } else {
            // Generate factual summary
            sb.append(String.format("%s with a proven track record of designing, building, and maintaining robust applications.", baseTitle));
            sb.append(String.format(" Demonstrated hands-on expertise in %s.", skillsHighlight));
            if (jd.getTargetCompany() != null && !jd.getTargetCompany().isBlank()) {
                sb.append(String.format(" Committed to delivering measurable impact and engineering excellence at %s.", jd.getTargetCompany()));
            }
        }

        return sb.toString();
    }

    private List<WorkExperienceDto> tailorExperiences(List<WorkExperienceDto> masterExps, JobDescriptionDto jd) {
        if (masterExps == null) return new ArrayList<>();

        List<WorkExperienceDto> tailored = new ArrayList<>();
        List<String> allJdKeywords = extractAllJdKeywords(jd);

        for (WorkExperienceDto exp : masterExps) {
            WorkExperienceDto tExp = new WorkExperienceDto();
            tExp.setId(exp.getId());
            tExp.setCompany(exp.getCompany());
            tExp.setJobTitle(exp.getJobTitle());
            tExp.setLocation(exp.getLocation());
            tExp.setStartDate(exp.getStartDate());
            tExp.setEndDate(exp.getEndDate());
            tExp.setCurrent(exp.isCurrent());
            tExp.setTechnologies(exp.getTechnologies() != null ? new ArrayList<>(exp.getTechnologies()) : new ArrayList<>());

            List<String> bullets = exp.getBullets() != null ? new ArrayList<>(exp.getBullets()) : new ArrayList<>();
            // Enhance action verbs
            List<String> enhancedBullets = new ArrayList<>();
            for (String b : bullets) {
                enhancedBullets.add(enhanceActionVerb(b));
            }

            // Score and sort bullets by relevance to JD
            enhancedBullets.sort(Comparator.comparingInt(b -> -scoreBulletRelevance(b, allJdKeywords)));
            tExp.setBullets(enhancedBullets);
            tailored.add(tExp);
        }

        return tailored;
    }

    private List<ProjectDto> tailorProjects(List<ProjectDto> masterProjects, JobDescriptionDto jd) {
        if (masterProjects == null) return new ArrayList<>();

        List<ProjectDto> tailored = new ArrayList<>();
        List<String> allJdKeywords = extractAllJdKeywords(jd);

        for (ProjectDto proj : masterProjects) {
            ProjectDto tProj = new ProjectDto();
            tProj.setId(proj.getId());
            tProj.setName(proj.getName());
            tProj.setRole(proj.getRole());
            tProj.setDescription(proj.getDescription());
            tProj.setUrl(proj.getUrl());
            tProj.setTechnologies(proj.getTechnologies() != null ? new ArrayList<>(proj.getTechnologies()) : new ArrayList<>());

            List<String> bullets = proj.getBullets() != null ? new ArrayList<>(proj.getBullets()) : new ArrayList<>();
            List<String> enhanced = new ArrayList<>();
            for (String b : bullets) {
                enhanced.add(enhanceActionVerb(b));
            }
            enhanced.sort(Comparator.comparingInt(b -> -scoreBulletRelevance(b, allJdKeywords)));
            tProj.setBullets(enhanced);
            tailored.add(tProj);
        }

        // Sort projects by overall keyword relevance
        tailored.sort(Comparator.comparingInt(p -> -scoreProjectRelevance(p, allJdKeywords)));
        return tailored;
    }

    private String enhanceActionVerb(String bullet) {
        if (bullet == null) return "";
        String trimmed = bullet.trim();
        for (Map.Entry<Pattern, String> entry : ACTION_VERB_REPLACEMENTS.entrySet()) {
            if (entry.getKey().matcher(trimmed).find()) {
                return entry.getKey().matcher(trimmed).replaceFirst(entry.getValue());
            }
        }
        return trimmed;
    }

    private int scoreBulletRelevance(String bullet, List<String> jdKeywords) {
        int score = 0;
        for (String kw : jdKeywords) {
            if (normalizer.matches(kw, bullet)) {
                score += 3;
            }
        }
        return score;
    }

    private int scoreProjectRelevance(ProjectDto project, List<String> jdKeywords) {
        int score = 0;
        if (project.getTechnologies() != null) {
            for (String t : project.getTechnologies()) {
                for (String kw : jdKeywords) {
                    if (normalizer.matches(kw, t)) score += 5;
                }
            }
        }
        if (project.getDescription() != null) {
            for (String kw : jdKeywords) {
                if (normalizer.matches(kw, project.getDescription())) score += 2;
            }
        }
        return score;
    }

    private List<String> extractAllJdKeywords(JobDescriptionDto jd) {
        List<String> list = new ArrayList<>();
        if (jd.getRequiredSkills() != null) list.addAll(jd.getRequiredSkills());
        if (jd.getPreferredSkills() != null) list.addAll(jd.getPreferredSkills());
        if (jd.getToolsAndPlatforms() != null) list.addAll(jd.getToolsAndPlatforms());
        if (jd.getDomainTerms() != null) list.addAll(jd.getDomainTerms());
        return list;
    }
}
