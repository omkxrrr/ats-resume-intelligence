package com.ats.intelligence.engine;

import com.ats.intelligence.config.AtsScoringProperties;
import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.MatchReportDto;
import com.ats.intelligence.dto.RequirementMatchDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import com.ats.intelligence.model.RequirementCategory;
import com.ats.intelligence.model.SkillSupportLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AtsScoringEngine {

    private final KeywordNormalizer normalizer;
    private final AtsScoringProperties scoringProperties;

    public AtsScoringEngine(KeywordNormalizer normalizer, AtsScoringProperties scoringProperties) {
        this.normalizer = normalizer;
        this.scoringProperties = scoringProperties;
    }

    public MatchReportDto evaluateMatch(MasterResumeDto master, JobDescriptionDto jd) {
        MatchReportDto report = new MatchReportDto();
        List<RequirementMatchDto> matches = new ArrayList<>();

        String masterEvidenceText = master.getAllEvidenceText();
        Set<String> explicitSkills = master.getAllExplicitSkills();

        // 1. Evaluate Required Skills
        evaluateCategory(jd.getRequiredSkills(), RequirementCategory.REQUIRED_SKILL, "High (Required)",
                master, masterEvidenceText, explicitSkills, matches);

        // 2. Evaluate Preferred Skills
        evaluateCategory(jd.getPreferredSkills(), RequirementCategory.PREFERRED_SKILL, "Medium (Preferred)",
                master, masterEvidenceText, explicitSkills, matches);

        // 3. Evaluate Key Responsibilities
        evaluateCategory(jd.getResponsibilities(), RequirementCategory.RESPONSIBILITY, "High (Responsibility)",
                master, masterEvidenceText, explicitSkills, matches);

        // 4. Evaluate Tools & Platforms
        evaluateCategory(jd.getToolsAndPlatforms(), RequirementCategory.TOOL_PLATFORM, "Medium (Tool)",
                master, masterEvidenceText, explicitSkills, matches);

        // 5. Evaluate Education & Experience
        evaluateCategory(jd.getEducationAndExperience(), RequirementCategory.EDUCATION_EXPERIENCE, "Required (Eligibility)",
                master, masterEvidenceText, explicitSkills, matches);

        // 6. Evaluate Domain Terms
        evaluateCategory(jd.getDomainTerms(), RequirementCategory.DOMAIN_TERM, "Nice-to-have (Domain)",
                master, masterEvidenceText, explicitSkills, matches);

        report.setRequirementMatches(matches);

        // Calculate counts
        int verifiedCount = 0;
        int partialCount = 0;
        int missingCount = 0;

        for (RequirementMatchDto m : matches) {
            if (m.getStatus() == SkillSupportLevel.VERIFIED) {
                verifiedCount++;
            } else if (m.getStatus() == SkillSupportLevel.PARTIALLY_SUPPORTED) {
                partialCount++;
            } else {
                missingCount++;
            }
        }

        report.setMatchedCount(verifiedCount);
        report.setPartiallyMatchedCount(partialCount);
        report.setMissingCount(missingCount);
        report.setTotalRequirements(matches.size());

        // Category breakdown & weighted score calculation
        Map<RequirementCategory, Integer> configWeights = scoringProperties.getWeightMap();
        report.setCategoryWeights(configWeights);

        Map<RequirementCategory, Double> categoryScores = new EnumMap<>(RequirementCategory.class);
        Map<RequirementCategory, Integer> categoryTotals = new EnumMap<>(RequirementCategory.class);
        Map<RequirementCategory, Double> categoryPoints = new EnumMap<>(RequirementCategory.class);

        for (RequirementMatchDto m : matches) {
            categoryTotals.merge(m.getCategory(), 1, Integer::sum);
            double points = m.getStatus() == SkillSupportLevel.VERIFIED ? 1.0 :
                    (m.getStatus() == SkillSupportLevel.PARTIALLY_SUPPORTED ? 0.5 : 0.0);
            categoryPoints.merge(m.getCategory(), points, Double::sum);
        }

        double totalWeightedScore = 0.0;
        double totalApplicableWeight = 0.0;
        StringBuilder explanation = new StringBuilder();
        explanation.append("ATS Match Score Calculation Breakdown:\n");

        for (RequirementCategory cat : RequirementCategory.values()) {
            int count = categoryTotals.getOrDefault(cat, 0);
            int weight = configWeights.getOrDefault(cat, 0);

            if (count > 0 && weight > 0) {
                double pts = categoryPoints.getOrDefault(cat, 0.0);
                double catScore = (pts / count) * 100.0;
                categoryScores.put(cat, Math.round(catScore * 10.0) / 10.0);

                totalWeightedScore += (catScore * weight);
                totalApplicableWeight += weight;

                explanation.append(String.format(Locale.US,
                        " - %s (Weight %d%%): %.1f%% (%d items: %d verified, %d partial)\n",
                        cat.getTitle(), weight, catScore, count,
                        (int) pts, (int) Math.round((pts % 1.0) * 2)));
            } else if (count == 0) {
                // Not in JD; weight redistributed dynamically
                categoryScores.put(cat, 100.0); // Neutral
            }
        }

        double finalScore = totalApplicableWeight > 0 ? (totalWeightedScore / totalApplicableWeight) : 0.0;
        finalScore = Math.max(0.0, Math.min(100.0, finalScore));
        report.setOverallScore(Math.round(finalScore * 10.0) / 10.0);
        report.setCategoryScores(categoryScores);

        explanation.append(String.format(Locale.US,
                "\nOverall Calculated Score: %.1f%% across %d applicable categories (redistributed weight: %.0f%%).",
                report.getOverallScore(), categoryTotals.size(), totalApplicableWeight));
        report.setScoreExplanation(explanation.toString());

        return report;
    }

    private void evaluateCategory(List<String> items, RequirementCategory category, String importance,
                                  MasterResumeDto master, String evidenceText, Set<String> explicitSkills,
                                  List<RequirementMatchDto> matches) {
        if (items == null) return;

        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            String req = item.trim();

            RequirementMatchDto match = new RequirementMatchDto();
            match.setRequirement(req);
            match.setCategory(category);
            match.setImportance(importance);

            // Determine match status and locate evidence
            String evidenceSnippet = findEvidenceSnippet(req, master, evidenceText, explicitSkills);

            if (evidenceSnippet != null) {
                match.setStatus(SkillSupportLevel.VERIFIED);
                match.setEvidence(evidenceSnippet);
                match.setSuggestedAction("Prioritize in targeted bullet points and relevant skills section.");
            } else if (isPartiallySupported(req, evidenceText)) {
                match.setStatus(SkillSupportLevel.PARTIALLY_SUPPORTED);
                match.setEvidence("Found related contextual mentions in profile.");
                match.setSuggestedAction("Highlight adjacent experience during interview; do not claim unverified deep expertise.");
            } else {
                match.setStatus(SkillSupportLevel.MISSING);
                match.setEvidence("No direct or related evidence found in master profile.");
                match.setSuggestedAction("Identified as genuine skill gap. Do NOT fabricate experience; consider acquiring this skill or noting in cover letter.");
            }

            matches.add(match);
        }
    }

    private String findEvidenceSnippet(String requirement, MasterResumeDto master, String evidenceText, Set<String> explicitSkills) {
        // 1. Check explicit skills list first
        for (String skill : explicitSkills) {
            if (normalizer.matches(requirement, skill)) {
                return "Explicitly listed in Skills: '" + skill + "'";
            }
        }

        // 2. Check Work Experience bullets
        if (master.getWorkExperiences() != null) {
            for (WorkExperienceDto exp : master.getWorkExperiences()) {
                if (exp.getBullets() != null) {
                    for (String bullet : exp.getBullets()) {
                        if (normalizer.matches(requirement, bullet)) {
                            String comp = exp.getCompany() != null ? exp.getCompany() : "Experience";
                            return comp + ": \"" + truncate(bullet, 90) + "\"";
                        }
                    }
                }
            }
        }

        // 3. Check Projects
        if (master.getProjects() != null) {
            for (var proj : master.getProjects()) {
                if (proj.getBullets() != null) {
                    for (String bullet : proj.getBullets()) {
                        if (normalizer.matches(requirement, bullet)) {
                            return "Project '" + proj.getName() + "': \"" + truncate(bullet, 80) + "\"";
                        }
                    }
                }
            }
        }

        // 4. Check entire evidence text
        if (normalizer.matches(requirement, evidenceText)) {
            return "Referenced in master resume profile";
        }

        return null;
    }

    private boolean isPartiallySupported(String requirement, String evidenceText) {
        String normReq = normalizer.normalize(requirement);
        String[] tokens = normReq.split("\\s+");
        if (tokens.length >= 2) {
            int matchedTokens = 0;
            for (String tok : tokens) {
                if (tok.length() > 3 && normalizer.matches(tok, evidenceText)) {
                    matchedTokens++;
                }
            }
            return matchedTokens >= (tokens.length / 2) && matchedTokens > 0;
        }
        return false;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
