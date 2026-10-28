package com.ats.intelligence.dto;

import com.ats.intelligence.model.RequirementCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchReportDto {
    private double overallScore; // 0 to 100
    private Map<RequirementCategory, Double> categoryScores = new HashMap<>();
    private Map<RequirementCategory, Integer> categoryWeights = new HashMap<>();
    private List<RequirementMatchDto> requirementMatches = new ArrayList<>();

    private int matchedCount;
    private int partiallyMatchedCount;
    private int missingCount;
    private int totalRequirements;

    private String scoreExplanation;
    private String disclaimer = "This ATS score is calculated using transparent keyword coverage and requirement alignment heuristics. ATS algorithms vary widely by vendor; no score provides a guarantee of passing an automated screening or receiving an interview.";

    public MatchReportDto() {
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public Map<RequirementCategory, Double> getCategoryScores() {
        return categoryScores;
    }

    public void setCategoryScores(Map<RequirementCategory, Double> categoryScores) {
        this.categoryScores = categoryScores != null ? categoryScores : new HashMap<>();
    }

    public Map<RequirementCategory, Integer> getCategoryWeights() {
        return categoryWeights;
    }

    public void setCategoryWeights(Map<RequirementCategory, Integer> categoryWeights) {
        this.categoryWeights = categoryWeights != null ? categoryWeights : new HashMap<>();
    }

    public List<RequirementMatchDto> getRequirementMatches() {
        return requirementMatches;
    }

    public void setRequirementMatches(List<RequirementMatchDto> requirementMatches) {
        this.requirementMatches = requirementMatches != null ? requirementMatches : new ArrayList<>();
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(int matchedCount) {
        this.matchedCount = matchedCount;
    }

    public int getPartiallyMatchedCount() {
        return partiallyMatchedCount;
    }

    public void setPartiallyMatchedCount(int partiallyMatchedCount) {
        this.partiallyMatchedCount = partiallyMatchedCount;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public int getTotalRequirements() {
        return totalRequirements;
    }

    public void setTotalRequirements(int totalRequirements) {
        this.totalRequirements = totalRequirements;
    }

    public String getScoreExplanation() {
        return scoreExplanation;
    }

    public void setScoreExplanation(String scoreExplanation) {
        this.scoreExplanation = scoreExplanation;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
