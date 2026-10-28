package com.ats.intelligence.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterResumeDto {
    private String id;
    private String originalFileName;
    private boolean verified = false;

    // Personal & Contact Info
    private String fullName;
    private String headline; // e.g. "Software Developer | Full Stack Developer (React · Node.js · PostgreSQL)"
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;

    // Professional Summary
    private String summary;

    // Categorized / Raw Skills
    private List<String> skills = new ArrayList<>();
    private java.util.Map<String, List<String>> categorizedSkills = new java.util.LinkedHashMap<>();

    // Detailed Sections
    private List<WorkExperienceDto> workExperiences = new ArrayList<>();
    private List<EducationDto> education = new ArrayList<>();
    private List<ProjectDto> projects = new ArrayList<>();
    private List<CertificationDto> certifications = new ArrayList<>();
    private List<String> achievements = new ArrayList<>();
    private List<String> activities = new ArrayList<>();
    private List<String> softSkills = new ArrayList<>();

    public MasterResumeDto() {
    }

    /**
     * Aggregates all text content in the master resume for factual evidence verification.
     */
    public String getAllEvidenceText() {
        StringBuilder sb = new StringBuilder();
        if (summary != null) sb.append(summary).append("\n");
        if (skills != null) sb.append(String.join(", ", skills)).append("\n");

        if (workExperiences != null) {
            for (WorkExperienceDto exp : workExperiences) {
                if (exp.getCompany() != null) sb.append(exp.getCompany()).append(" ");
                if (exp.getJobTitle() != null) sb.append(exp.getJobTitle()).append(" ");
                if (exp.getBullets() != null) sb.append(String.join(" ", exp.getBullets())).append(" ");
                if (exp.getTechnologies() != null) sb.append(String.join(" ", exp.getTechnologies())).append(" ");
                sb.append("\n");
            }
        }

        if (projects != null) {
            for (ProjectDto proj : projects) {
                if (proj.getName() != null) sb.append(proj.getName()).append(" ");
                if (proj.getDescription() != null) sb.append(proj.getDescription()).append(" ");
                if (proj.getBullets() != null) sb.append(String.join(" ", proj.getBullets())).append(" ");
                if (proj.getTechnologies() != null) sb.append(String.join(" ", proj.getTechnologies())).append(" ");
                sb.append("\n");
            }
        }

        if (education != null) {
            for (EducationDto edu : education) {
                if (edu.getInstitution() != null) sb.append(edu.getInstitution()).append(" ");
                if (edu.getDegree() != null) sb.append(edu.getDegree()).append(" ");
                if (edu.getFieldOfStudy() != null) sb.append(edu.getFieldOfStudy()).append(" ");
                sb.append("\n");
            }
        }

        if (certifications != null) {
            for (CertificationDto cert : certifications) {
                if (cert.getName() != null) sb.append(cert.getName()).append(" ");
                if (cert.getIssuingOrganization() != null) sb.append(cert.getIssuingOrganization()).append(" ");
                sb.append("\n");
            }
        }

        if (achievements != null) {
            sb.append(String.join(" ", achievements)).append("\n");
        }
        if (activities != null) {
            sb.append(String.join(" ", activities)).append("\n");
        }
        if (softSkills != null) {
            sb.append(String.join(" ", softSkills)).append("\n");
        }

        return sb.toString();
    }

    public Set<String> getAllExplicitSkills() {
        Set<String> set = new HashSet<>();
        if (skills != null) {
            for (String s : skills) {
                if (s != null && !s.isBlank()) set.add(s.trim());
            }
        }
        if (workExperiences != null) {
            for (WorkExperienceDto exp : workExperiences) {
                if (exp.getTechnologies() != null) {
                    for (String t : exp.getTechnologies()) {
                        if (t != null && !t.isBlank()) set.add(t.trim());
                    }
                }
            }
        }
        if (projects != null) {
            for (ProjectDto p : projects) {
                if (p.getTechnologies() != null) {
                    for (String t : p.getTechnologies()) {
                        if (t != null && !t.isBlank()) set.add(t.trim());
                    }
                }
            }
        }
        return set;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public List<WorkExperienceDto> getWorkExperiences() {
        return workExperiences;
    }

    public void setWorkExperiences(List<WorkExperienceDto> workExperiences) {
        this.workExperiences = workExperiences != null ? workExperiences : new ArrayList<>();
    }

    public List<EducationDto> getEducation() {
        return education;
    }

    public void setEducation(List<EducationDto> education) {
        this.education = education != null ? education : new ArrayList<>();
    }

    public List<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectDto> projects) {
        this.projects = projects != null ? projects : new ArrayList<>();
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

    public java.util.Map<String, List<String>> getCategorizedSkills() {
        return categorizedSkills;
    }

    public void setCategorizedSkills(java.util.Map<String, List<String>> categorizedSkills) {
        this.categorizedSkills = categorizedSkills != null ? categorizedSkills : new java.util.LinkedHashMap<>();
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
}
