package com.ats.intelligence.dto;

public class ValidationCheckDto {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    private Severity severity;
    private String ruleName;
    private String message;
    private boolean passed;

    public ValidationCheckDto() {
    }

    public ValidationCheckDto(Severity severity, String ruleName, String message, boolean passed) {
        this.severity = severity;
        this.ruleName = ruleName;
        this.message = message;
        this.passed = passed;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
