package com.ats.intelligence.model;

public enum RequirementCategory {
    REQUIRED_SKILL("Required Skills", "Essential technical skills explicitly required for the role"),
    PREFERRED_SKILL("Preferred Skills", "Nice-to-have or preferred technical competencies"),
    RESPONSIBILITY("Key Responsibilities", "Core duties and operational expectations"),
    TOOL_PLATFORM("Tools & Platforms", "Specific frameworks, libraries, platforms, or databases"),
    EDUCATION_EXPERIENCE("Education & Experience", "Academic degrees, certifications, or years of experience"),
    DOMAIN_TERM("Domain Terminology", "Industry-specific terminology and methodology");

    private final String title;
    private final String description;

    RequirementCategory(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
