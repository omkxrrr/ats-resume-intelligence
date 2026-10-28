package com.ats.intelligence.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TailoredResumeDto {
    private String versionId = UUID.randomUUID().toString();
    private String createdAt = LocalDateTime.now().toString();
    private String generatorEngine; // "RULE_BASED" or "OLLAMA_AI"

    // Personal & Contact Info (Strictly preserved from verified Master)
    private String fullName;
    private String headline;
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;

    // Tailored sections
    private String targetRole;
    private String targetCompany;
    private String tailoredSummary;
    private List<String> tailoredSkills = new ArrayList<>();
    private java.util.Map<String, List<String>> tailoredCategorizedSkills = new java.util.LinkedHashMap<>();
    private List<WorkExperienceDto> tailoredExperiences = new ArrayList<>();
    private List<ProjectDto> tailoredProjects = new ArrayList<>();

    // Preserved sections
    private List<EducationDto> education = new ArrayList<>();
    private List<CertificationDto> certifications = new ArrayList<>();
    private List<String> achievements = new ArrayList<>();
    private List<String> activities = new ArrayList<>();
    private List<String> softSkills = new ArrayList<>();

    // Associated JD and Reports
    private JobDescriptionDto jobDescription;
    private MatchReportDto matchReport;
    private PreDownloadValidationReport validationReport;

    public TailoredResumeDto() {
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getGeneratorEngine() {
        return generatorEngine;
    }

    public void setGeneratorEngine(String generatorEngine) {
        this.generatorEngine = generatorEngine;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
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

    public String getTailoredSummary() {
        return tailoredSummary;
    }

    public void setTailoredSummary(String tailoredSummary) {
        this.tailoredSummary = tailoredSummary;
    }

    public List<String> getTailoredSkills() {
        return tailoredSkills;
    }

    public void setTailoredSkills(List<String> tailoredSkills) {
        this.tailoredSkills = tailoredSkills != null ? tailoredSkills : new ArrayList<>();
    }

    public List<WorkExperienceDto> getTailoredExperiences() {
        return tailoredExperiences;
    }

    public void setTailoredExperiences(List<WorkExperienceDto> tailoredExperiences) {
        this.tailoredExperiences = tailoredExperiences != null ? tailoredExperiences : new ArrayList<>();
    }

    public List<ProjectDto> getTailoredProjects() {
        return tailoredProjects;
    }

    public void setTailoredProjects(List<ProjectDto> tailoredProjects) {
        this.tailoredProjects = tailoredProjects != null ? tailoredProjects : new ArrayList<>();
    }

    public List<EducationDto> getEducation() {
        return education;
    }

    public void setEducation(List<EducationDto> education) {
        this.education = education != null ? education : new ArrayList<>();
    }

    public List<CertificationDto> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<CertificationDto> certifications) {
        this.certifications = certifications != null ? certifications : new ArrayList<>();
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements != null ? achievements : new ArrayList<>();
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public java.util.Map<String, List<String>> getTailoredCategorizedSkills() {
        return tailoredCategorizedSkills;
    }

    public void setTailoredCategorizedSkills(java.util.Map<String, List<String>> tailoredCategorizedSkills) {
        this.tailoredCategorizedSkills = tailoredCategorizedSkills != null ? tailoredCategorizedSkills : new java.util.LinkedHashMap<>();
    }

    public List<String> getActivities() {
        return activities;
    }

    public void setActivities(List<String> activities) {
        this.activities = activities != null ? activities : new ArrayList<>();
    }

    public List<String> getSoftSkills() {
        return softSkills;
    }

    public void setSoftSkills(List<String> softSkills) {
        this.softSkills = softSkills != null ? softSkills : new ArrayList<>();
    }

    public JobDescriptionDto getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescriptionDto jobDescription) {
        this.jobDescription = jobDescription;
    }

    public MatchReportDto getMatchReport() {
        return matchReport;
    }

    public void setMatchReport(MatchReportDto matchReport) {
        this.matchReport = matchReport;
    }

    public PreDownloadValidationReport getValidationReport() {
        return validationReport;
    }

    public void setValidationReport(PreDownloadValidationReport validationReport) {
        this.validationReport = validationReport;
    }
}
