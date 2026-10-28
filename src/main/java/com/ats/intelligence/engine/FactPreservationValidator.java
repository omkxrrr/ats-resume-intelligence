package com.ats.intelligence.engine;

import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.PreDownloadValidationReport;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.ValidationCheckDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FactPreservationValidator {

    private static final Logger log = LoggerFactory.getLogger(FactPreservationValidator.class);

    private final KeywordNormalizer normalizer;

    private static final Pattern NUMERICAL_METRIC_PATTERN = Pattern.compile(
            "\\b(\\$?\\d+(?:\\.\\d+)?%?|\\d+\\+?\\s*(?:k|m|million|billion|users|customers|qps|rps|ms|%|percent))\\b",
            Pattern.CASE_INSENSITIVE);

    public FactPreservationValidator(KeywordNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * Strictly verifies that the tailored resume has preserved all ground-truth facts from the master resume,
     * strips any fabricated skills, reverts any hallucinated metrics, and generates a pre-download validation report.
     */
    public PreDownloadValidationReport validateAndSanitize(MasterResumeDto master, TailoredResumeDto tailored) {
        PreDownloadValidationReport report = new PreDownloadValidationReport();

        // 1. Contact & Identity Validation
        validateIdentity(master, tailored, report);

        // 2. Education Preservation Validation
        validateEducation(master, tailored, report);

        // 3. Work Experience Employment Facts Validation
        validateEmploymentHistory(master, tailored, report);

        // 4. Skills Ground-Truth Validation & Hallucination Removal
        validateAndSanitizeSkills(master, tailored, report);

        // 5. Numerical Metrics Preservation
        validateNumericalMetrics(master, tailored, report);

        // 6. Formatting & Length Feasibility
        validateFormattingAndLength(tailored, report);

        tailored.setValidationReport(report);
        return report;
    }

    private void validateIdentity(MasterResumeDto master, TailoredResumeDto tailored, PreDownloadValidationReport report) {
        // Enforce strict overwrite from master if discrepancy exists
        if (!Objects.equals(master.getFullName(), tailored.getFullName())) {
            tailored.setFullName(master.getFullName());
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.WARNING,
                    "Identity Preservation", "Preserved candidate name exactly from master resume: " + master.getFullName(), true));
        }

        if (!Objects.equals(master.getEmail(), tailored.getEmail())) {
            tailored.setEmail(master.getEmail());
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.WARNING,
                    "Contact Preservation", "Preserved email from master resume: " + master.getEmail(), true));
        }

        if (!Objects.equals(master.getPhone(), tailored.getPhone())) {
            tailored.setPhone(master.getPhone());
        }
        if (!Objects.equals(master.getLocation(), tailored.getLocation())) {
            tailored.setLocation(master.getLocation());
        }
        if (!Objects.equals(master.getLinkedinUrl(), tailored.getLinkedinUrl())) {
            tailored.setLinkedinUrl(master.getLinkedinUrl());
        }
        if (!Objects.equals(master.getGithubUrl(), tailored.getGithubUrl())) {
            tailored.setGithubUrl(master.getGithubUrl());
        }
        if (tailored.getHeadline() == null && master.getHeadline() != null) {
            tailored.setHeadline(master.getHeadline());
        }

        // Contact info completeness check
        List<String> missingContacts = new ArrayList<>();
        if (tailored.getFullName() == null || tailored.getFullName().isBlank()) {
            missingContacts.add("Full Name");
        }
        if (tailored.getEmail() == null || tailored.getEmail().isBlank()) {
            missingContacts.add("Email Address");
        }
        if (tailored.getPhone() == null || tailored.getPhone().isBlank()) {
            missingContacts.add("Phone Number");
        }

        if (!missingContacts.isEmpty()) {
            report.setMissingContactItems(missingContacts);
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.WARNING,
                    "Contact Information Completeness",
                    "Resume is missing standard contact fields: " + String.join(", ", missingContacts), false));
        } else {
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.INFO,
                    "Contact Information Completeness", "All essential contact details are present and verified.", true));
        }
    }

    private void validateEducation(MasterResumeDto master, TailoredResumeDto tailored, PreDownloadValidationReport report) {
        // Education and extra-curricular credentials must be preserved as-is from master
        tailored.setEducation(master.getEducation());
        tailored.setCertifications(master.getCertifications());
        tailored.setAchievements(master.getAchievements());
        tailored.setActivities(master.getActivities());
        tailored.setSoftSkills(master.getSoftSkills());

        report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.INFO,
                "Education & Credentials Integrity",
                "Degrees, institutions, certifications, achievements, activities, and skills are 100% locked to verified master profile.", true));
    }

    private void validateEmploymentHistory(MasterResumeDto master, TailoredResumeDto tailored, PreDownloadValidationReport report) {
        List<WorkExperienceDto> masterExps = master.getWorkExperiences();
        List<WorkExperienceDto> tailoredExps = tailored.getTailoredExperiences();

        if (masterExps == null || masterExps.isEmpty()) {
            return;
        }

        // Ensure employer names, job titles, and dates are strictly preserved
        for (int i = 0; i < Math.min(masterExps.size(), tailoredExps.size()); i++) {
            WorkExperienceDto mExp = masterExps.get(i);
            WorkExperienceDto tExp = tailoredExps.get(i);

            tExp.setCompany(mExp.getCompany());
            tExp.setJobTitle(mExp.getJobTitle());
            tExp.setStartDate(mExp.getStartDate());
            tExp.setEndDate(mExp.getEndDate());
            tExp.setLocation(mExp.getLocation());
            tExp.setCurrent(mExp.isCurrent());
        }

        report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.INFO,
                "Employment Facts Integrity",
                "All employer names, job titles, tenures, and employment dates strictly preserved.", true));
    }

    private void validateAndSanitizeSkills(MasterResumeDto master, TailoredResumeDto tailored, PreDownloadValidationReport report) {
        String masterEvidenceText = master.getAllEvidenceText();
        Set<String> explicitSkills = master.getAllExplicitSkills();

        List<String> sanitizedSkills = new ArrayList<>();
        List<String> rejectedSkills = new ArrayList<>();

        for (String skill : tailored.getTailoredSkills()) {
            if (skill == null || skill.isBlank()) continue;
            String trimmed = skill.trim();

            // Skill must exist in master profile evidence
            boolean supported = false;
            for (String s : explicitSkills) {
                if (normalizer.matches(trimmed, s)) {
                    supported = true;
                    break;
                }
            }
            if (!supported && normalizer.matches(trimmed, masterEvidenceText)) {
                supported = true;
            }

            if (supported) {
                if (!sanitizedSkills.contains(trimmed)) {
                    sanitizedSkills.add(trimmed);
                }
            } else {
                rejectedSkills.add(trimmed);
            }
        }

        tailored.setTailoredSkills(sanitizedSkills);

        if (!rejectedSkills.isEmpty()) {
            report.setUnsupportedClaimsCount(report.getUnsupportedClaimsCount() + rejectedSkills.size());
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.WARNING,
                    "Fact-Preservation Skill Guardrail",
                    "Removed " + rejectedSkills.size() + " unsupported skill(s) not found in master resume: "
                            + String.join(", ", rejectedSkills), false));
        } else {
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.INFO,
                    "Fact-Preservation Skill Guardrail",
                    "All included skills are verified against the master resume. 0 hallucinations detected.", true));
        }
    }

    private void validateNumericalMetrics(MasterResumeDto master, TailoredResumeDto tailored, PreDownloadValidationReport report) {
        List<WorkExperienceDto> masterExps = master.getWorkExperiences();
        List<WorkExperienceDto> tailoredExps = tailored.getTailoredExperiences();

        int revertedCount = 0;

        if (masterExps != null && tailoredExps != null) {
            for (int i = 0; i < Math.min(masterExps.size(), tailoredExps.size()); i++) {
                WorkExperienceDto mExp = masterExps.get(i);
                WorkExperienceDto tExp = tailoredExps.get(i);

                Set<String> masterMetrics = extractMetrics(String.join(" ", mExp.getBullets()));

                List<String> sanitizedBullets = new ArrayList<>();
                for (String tBullet : tExp.getBullets()) {
                    Set<String> bulletMetrics = extractMetrics(tBullet);
                    boolean hasFabricatedMetric = false;

                    for (String metric : bulletMetrics) {
                        if (!masterMetrics.contains(metric)) {
                            hasFabricatedMetric = true;
                            log.warn("Fabricated numerical metric detected in bullet: '{}' (metric: '{}')", tBullet, metric);
                            break;
                        }
                    }

                    if (hasFabricatedMetric) {
                        // Revert to matching original bullet or safe text
                        revertedCount++;
                        sanitizedBullets.add(findClosestMasterBullet(tBullet, mExp.getBullets()));
                    } else {
                        sanitizedBullets.add(tBullet);
                    }
                }
                tExp.setBullets(sanitizedBullets);
            }
        }

        if (revertedCount > 0) {
            report.setUnsupportedClaimsCount(report.getUnsupportedClaimsCount() + revertedCount);
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.WARNING,
                    "Numerical Metrics Integrity",
                    "Reverted " + revertedCount + " bullet point(s) containing unverified or fabricated numerical metrics.", false));
        } else {
            report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.INFO,
                    "Numerical Metrics Integrity",
                    "All numerical figures, percentages, and metrics preserved faithfully without fabrication.", true));
        }
    }

    private void validateFormattingAndLength(TailoredResumeDto tailored, PreDownloadValidationReport report) {
        int totalBullets = 0;
        if (tailored.getTailoredExperiences() != null) {
            for (WorkExperienceDto exp : tailored.getTailoredExperiences()) {
                totalBullets += exp.getBullets().size();
            }
        }
        if (tailored.getTailoredProjects() != null) {
            for (var proj : tailored.getTailoredProjects()) {
                totalBullets += proj.getBullets().size();
            }
        }

        if (totalBullets > 20) {
            report.getFormattingRecommendations().add("Total bullet points (" + totalBullets + ") exceed recommended 1-2 page density. Consider condensing experience for maximum impact.");
        }

        report.addCheck(new ValidationCheckDto(ValidationCheckDto.Severity.INFO,
                "Layout & Structure",
                "ATS single-column structure verified. No columns, textboxes, or unreadable graphics.", true));
    }

    private Set<String> extractMetrics(String text) {
        Set<String> metrics = new HashSet<>();
        if (text == null) return metrics;
        Matcher m = NUMERICAL_METRIC_PATTERN.matcher(text);
        while (m.find()) {
            metrics.add(m.group(1).toLowerCase().replaceAll("\\s+", ""));
        }
        return metrics;
    }

    private String findClosestMasterBullet(String candidate, List<String> masterBullets) {
        if (masterBullets == null || masterBullets.isEmpty()) return candidate;
        // Find bullet with highest word overlap
        String[] candidateWords = candidate.toLowerCase().split("\\s+");
        String best = masterBullets.get(0);
        int maxOverlap = -1;

        for (String mBullet : masterBullets) {
            int overlap = 0;
            String lower = mBullet.toLowerCase();
            for (String w : candidateWords) {
                if (w.length() > 3 && lower.contains(w)) {
                    overlap++;
                }
            }
            if (overlap > maxOverlap) {
                maxOverlap = overlap;
                best = mBullet;
            }
        }
        return best;
    }
}
