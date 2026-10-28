package com.ats.intelligence.model;

public enum SkillSupportLevel {
    VERIFIED("Verified", "Direct evidence found in verified master profile"),
    PARTIALLY_SUPPORTED("Partially Supported", "Related or foundational experience found, but not explicit"),
    MISSING("Missing", "No supporting evidence found in master profile. Not added to resume to preserve truthfulness.");

    private final String displayName;
    private final String description;

    SkillSupportLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
