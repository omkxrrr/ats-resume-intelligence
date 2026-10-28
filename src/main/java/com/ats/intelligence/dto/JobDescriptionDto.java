package com.ats.intelligence.dto;

import java.util.ArrayList;
import java.util.List;

public class JobDescriptionDto {
    private String rawText;
    private String targetRole;
    private String targetCompany;
    private String preferredLength = "AUTO"; // ONE_PAGE, TWO_PAGES, AUTO

    private List<String> requiredSkills = new ArrayList<>();
    private List<String> preferredSkills = new ArrayList<>();
    private List<String> responsibilities = new ArrayList<>();
    private List<String> toolsAndPlatforms = new ArrayList<>();
    private List<String> educationAndExperience = new ArrayList<>();
    private List<String> domainTerms = new ArrayList<>();

    public JobDescriptionDto() {
    }

    public JobDescriptionDto(String rawText, String targetRole, String targetCompany, String preferredLength) {
        this.rawText = rawText;
        this.targetRole = targetRole;
        this.targetCompany = targetCompany;
        if (preferredLength != null && !preferredLength.isBlank()) {
            this.preferredLength = preferredLength;
        }
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getTargetCompany() {
        return targetCompany;
    }

    public void setTargetCompany(String targetCompany) {
        this.targetCompany = targetCompany;
    }

    public String getPreferredLength() {
        return preferredLength;
    }

    public void setPreferredLength(String preferredLength) {
        this.preferredLength = preferredLength;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
    }

    public List<String> getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(List<String> preferredSkills) {
        this.preferredSkills = preferredSkills != null ? preferredSkills : new ArrayList<>();
    }

    public List<String> getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities != null ? responsibilities : new ArrayList<>();
    }

    public List<String> getToolsAndPlatforms() {
        return toolsAndPlatforms;
    }

    public void setToolsAndPlatforms(List<String> toolsAndPlatforms) {
        this.toolsAndPlatforms = toolsAndPlatforms != null ? toolsAndPlatforms : new ArrayList<>();
    }

    public List<String> getEducationAndExperience() {
        return educationAndExperience;
    }

    public void setEducationAndExperience(List<String> educationAndExperience) {
        this.educationAndExperience = educationAndExperience != null ? educationAndExperience : new ArrayList<>();
    }

    public List<String> getDomainTerms() {
        return domainTerms;
    }

    public void setDomainTerms(List<String> domainTerms) {
        this.domainTerms = domainTerms != null ? domainTerms : new ArrayList<>();
    }
}
