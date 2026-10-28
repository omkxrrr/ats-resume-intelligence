package com.ats.intelligence.dto;

import jakarta.validation.constraints.NotBlank;

public class TailorRequestDto {
    @NotBlank(message = "Job description text is required")
    private String jobDescriptionText;

    private String targetRole;
    private String targetCompany;
    private String preferredLength = "AUTO"; // ONE_PAGE, TWO_PAGES, AUTO

    private boolean useAi = false;
    private String aiModel;

    public TailorRequestDto() {
    }

    public String getJobDescriptionText() {
        return jobDescriptionText;
    }

    public void setJobDescriptionText(String jobDescriptionText) {
        this.jobDescriptionText = jobDescriptionText;
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

    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }
}
