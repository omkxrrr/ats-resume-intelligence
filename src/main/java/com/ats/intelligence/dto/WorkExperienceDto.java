package com.ats.intelligence.dto;

import java.util.ArrayList;
import java.util.List;

public class WorkExperienceDto {
    private String id;
    private String company;
    private String jobTitle;
    private String location;
    private String startDate;
    private String endDate;
    private boolean current;
    private List<String> bullets = new ArrayList<>();
    private List<String> technologies = new ArrayList<>();

    public WorkExperienceDto() {
    }

    public WorkExperienceDto(String company, String jobTitle, String location, String startDate, String endDate, boolean current, List<String> bullets, List<String> technologies) {
        this.company = company;
        this.jobTitle = jobTitle;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.current = current;
        if (bullets != null) this.bullets = new ArrayList<>(bullets);
        if (technologies != null) this.technologies = new ArrayList<>(technologies);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public List<String> getBullets() {
        return bullets;
    }

    public void setBullets(List<String> bullets) {
        this.bullets = bullets != null ? bullets : new ArrayList<>();
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies != null ? technologies : new ArrayList<>();
    }
}
