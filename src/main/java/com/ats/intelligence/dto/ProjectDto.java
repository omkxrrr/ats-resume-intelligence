package com.ats.intelligence.dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectDto {
    private String id;
    private String name;
    private String role;
    private String description;
    private List<String> bullets = new ArrayList<>();
    private List<String> technologies = new ArrayList<>();
    private String url;

    public ProjectDto() {
    }

    public ProjectDto(String name, String role, String description, List<String> bullets, List<String> technologies, String url) {
        this.name = name;
        this.role = role;
        this.description = description;
        if (bullets != null) this.bullets = new ArrayList<>(bullets);
        if (technologies != null) this.technologies = new ArrayList<>(technologies);
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
