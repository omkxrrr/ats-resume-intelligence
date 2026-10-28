package com.ats.intelligence.dto;

import com.ats.intelligence.model.RequirementCategory;
import com.ats.intelligence.model.SkillSupportLevel;

public class RequirementMatchDto {
    private String requirement;
    private RequirementCategory category;
    private String importance; // "Required", "Preferred", "High", "Medium", "Nice-to-have"
    private SkillSupportLevel status;
    private String evidence;
    private String suggestedAction;

    public RequirementMatchDto() {
    }

    public RequirementMatchDto(String requirement, RequirementCategory category, String importance, SkillSupportLevel status, String evidence, String suggestedAction) {
        this.requirement = requirement;
        this.category = category;
        this.importance = importance;
        this.status = status;
        this.evidence = evidence;
        this.suggestedAction = suggestedAction;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public RequirementCategory getCategory() {
        return category;
    }

    public void setCategory(RequirementCategory category) {
        this.category = category;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public SkillSupportLevel getStatus() {
        return status;
    }

    public void setStatus(SkillSupportLevel status) {
        this.status = status;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
}
