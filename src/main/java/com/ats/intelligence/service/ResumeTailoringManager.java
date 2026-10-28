package com.ats.intelligence.service;

import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.OllamaStatusDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ResumeTailoringManager {

    private static final Logger log = LoggerFactory.getLogger(ResumeTailoringManager.class);

    private final RuleBasedResumeTailoringService ruleBasedService;
    private final OllamaResumeTailoringService ollamaService;

    public ResumeTailoringManager(@Qualifier("ruleBasedResumeTailoringService") RuleBasedResumeTailoringService ruleBasedService,
                                  @Qualifier("ollamaResumeTailoringService") OllamaResumeTailoringService ollamaService) {
        this.ruleBasedService = ruleBasedService;
        this.ollamaService = ollamaService;
    }

    public TailoredResumeDto tailorResume(MasterResumeDto master, JobDescriptionDto jd, boolean useAi) {
        if (useAi) {
            log.info("Tailoring requested with AI provider (Ollama).");
            return ollamaService.tailor(master, jd);
        } else {
            log.info("Tailoring requested with Deterministic Rule-Based Engine.");
            return ruleBasedService.tailor(master, jd);
        }
    }

    public OllamaStatusDto getOllamaStatus() {
        return ollamaService.checkStatus();
    }
}
