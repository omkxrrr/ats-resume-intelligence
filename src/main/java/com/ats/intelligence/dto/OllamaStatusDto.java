package com.ats.intelligence.dto;

import java.util.ArrayList;
import java.util.List;

public class OllamaStatusDto {
    private boolean available;
    private String baseUrl;
    private String currentModel;
    private List<String> availableModels = new ArrayList<>();
    private String statusMessage;

    public OllamaStatusDto() {
    }

    public OllamaStatusDto(boolean available, String baseUrl, String currentModel, List<String> availableModels, String statusMessage) {
        this.available = available;
        this.baseUrl = baseUrl;
        this.currentModel = currentModel;
        if (availableModels != null) this.availableModels = new ArrayList<>(availableModels);
        this.statusMessage = statusMessage;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public List<String> getAvailableModels() {
        return availableModels;
    }

    public void setAvailableModels(List<String> availableModels) {
        this.availableModels = availableModels != null ? availableModels : new ArrayList<>();
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
