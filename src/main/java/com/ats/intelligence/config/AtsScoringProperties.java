package com.ats.intelligence.config;

import com.ats.intelligence.model.RequirementCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ats.scoring")
public class AtsScoringProperties {

    private Weights weights = new Weights();

    public Weights getWeights() {
        return weights;
    }

    public void setWeights(Weights weights) {
        this.weights = weights;
    }

    public Map<RequirementCategory, Integer> getWeightMap() {
        Map<RequirementCategory, Integer> map = new EnumMap<>(RequirementCategory.class);
        map.put(RequirementCategory.REQUIRED_SKILL, weights.getRequiredSkills());
        map.put(RequirementCategory.PREFERRED_SKILL, weights.getPreferredSkills());
        map.put(RequirementCategory.RESPONSIBILITY, weights.getResponsibilities());
        map.put(RequirementCategory.TOOL_PLATFORM, weights.getToolsTechnologies());
        map.put(RequirementCategory.EDUCATION_EXPERIENCE, weights.getEducationExperience());
        // Domain terms share weight or act as bonus/relevance indicator
        map.put(RequirementCategory.DOMAIN_TERM, 10);
        return map;
    }

    public static class Weights {
        private int requiredSkills = 40;
        private int preferredSkills = 15;
        private int responsibilities = 20;
        private int toolsTechnologies = 15;
        private int educationExperience = 10;

        public int getRequiredSkills() {
            return requiredSkills;
        }

        public void setRequiredSkills(int requiredSkills) {
            this.requiredSkills = requiredSkills;
        }

        public int getPreferredSkills() {
            return preferredSkills;
        }

        public void setPreferredSkills(int preferredSkills) {
            this.preferredSkills = preferredSkills;
        }

        public int getResponsibilities() {
            return responsibilities;
        }

        public void setResponsibilities(int responsibilities) {
            this.responsibilities = responsibilities;
        }

        public int getToolsTechnologies() {
            return toolsTechnologies;
        }

        public void setToolsTechnologies(int toolsTechnologies) {
            this.toolsTechnologies = toolsTechnologies;
        }

        public int getEducationExperience() {
            return educationExperience;
        }

        public void setEducationExperience(int educationExperience) {
            this.educationExperience = educationExperience;
        }
    }
}
