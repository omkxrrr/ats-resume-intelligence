package com.ats.intelligence.dto;

import java.util.ArrayList;
import java.util.List;

public class PreDownloadValidationReport {
    private boolean readyForDownload = true;
    private int unsupportedClaimsCount = 0;
    private List<ValidationCheckDto> checks = new ArrayList<>();
    private List<String> missingContactItems = new ArrayList<>();
    private List<String> formattingRecommendations = new ArrayList<>();

    public PreDownloadValidationReport() {
    }

    public void addCheck(ValidationCheckDto check) {
        if (checks == null) {
            checks = new ArrayList<>();
        }
        checks.add(check);
        if (check.getSeverity() == ValidationCheckDto.Severity.ERROR && !check.isPassed()) {
            this.readyForDownload = false;
        }
    }

    public boolean isReadyForDownload() {
        return readyForDownload;
    }

    public void setReadyForDownload(boolean readyForDownload) {
        this.readyForDownload = readyForDownload;
    }

    public int getUnsupportedClaimsCount() {
        return unsupportedClaimsCount;
    }

    public void setUnsupportedClaimsCount(int unsupportedClaimsCount) {
        this.unsupportedClaimsCount = unsupportedClaimsCount;
    }

    public List<ValidationCheckDto> getChecks() {
        return checks;
    }

    public void setChecks(List<ValidationCheckDto> checks) {
        this.checks = checks != null ? checks : new ArrayList<>();
    }

    public List<String> getMissingContactItems() {
        return missingContactItems;
    }

    public void setMissingContactItems(List<String> missingContactItems) {
        this.missingContactItems = missingContactItems != null ? missingContactItems : new ArrayList<>();
    }

    public List<String> getFormattingRecommendations() {
        return formattingRecommendations;
    }

    public void setFormattingRecommendations(List<String> formattingRecommendations) {
        this.formattingRecommendations = formattingRecommendations != null ? formattingRecommendations : new ArrayList<>();
    }
}
