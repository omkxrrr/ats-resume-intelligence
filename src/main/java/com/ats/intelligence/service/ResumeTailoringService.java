package com.ats.intelligence.service;

import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.TailoredResumeDto;

public interface ResumeTailoringService {
    TailoredResumeDto tailor(MasterResumeDto master, JobDescriptionDto jd);
    String getEngineName();
}
