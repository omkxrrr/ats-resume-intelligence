package com.ats.intelligence.engine;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class KeywordNormalizer {

    private final Map<String, String> canonicalSynonyms = new HashMap<>();

    public KeywordNormalizer() {
        initSynonyms();
    }

    private void initSynonyms() {
        // Programming Languages
        register("js", "javascript");
        register("ts", "typescript");
        register("py", "python");
        register("golang", "go");

        // Frameworks & Libraries
        register("react.js", "react");
        register("reactjs", "react");
        register("vue.js", "vue");
        register("vuejs", "vue");
        register("angular.js", "angular");
        register("angularjs", "angular");
        register("node.js", "node");
        register("nodejs", "node");
        register("spring-boot", "spring boot");
        register("springboot", "spring boot");

        // Databases & Storage
        register("postgres", "postgresql");
        register("mongo", "mongodb");
        register("ms sql", "sql server");
        register("mssql", "sql server");

        // Cloud & DevOps
        register("k8s", "kubernetes");
        register("aws", "amazon web services");
        register("amazon web services", "aws");
        register("gcp", "google cloud platform");
        register("google cloud", "google cloud platform");
        register("ci/cd", "continuous integration");
        register("cicd", "continuous integration");

        // Architectural & Methodologies
        register("restful", "rest");
        register("rest api", "rest");
        register("restful api", "rest");
        register("microservices", "microservice");
        register("tdd", "test driven development");
        register("oop", "object oriented programming");
    }

    private void register(String alias, String canonical) {
        canonicalSynonyms.put(normalizeString(alias), normalizeString(canonical));
    }

    public String normalize(String term) {
        if (term == null) return "";
        String base = normalizeString(term);
        return canonicalSynonyms.getOrDefault(base, base);
    }

    public String normalizeString(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s+#/.-]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Checks if a target text matches or contains evidence of the required keyword.
     * Supports exact match, normalized synonym match, and boundary-checked phrase matching.
     */
    public boolean matches(String keyword, String targetText) {
        if (keyword == null || targetText == null) return false;
        String normKeyword = normalize(keyword);
        String normTarget = normalizeString(targetText);

        if (normKeyword.isBlank() || normTarget.isBlank()) return false;

        // 1. Direct contains check
        if (normTarget.contains(normKeyword)) {
            return true;
        }

        // 2. Canonical synonym check
        String canonical = canonicalSynonyms.get(normKeyword);
        if (canonical != null && normTarget.contains(canonical)) {
            return true;
        }

        // 3. Word boundary regex match for short words (e.g., "go", "c", "r", "sql")
        if (keyword.length() <= 3) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b");
            return pattern.matcher(targetText.toLowerCase()).find();
        }

        return false;
    }

    public Map<String, String> getCanonicalSynonyms() {
        return Collections.unmodifiableMap(canonicalSynonyms);
    }
}
