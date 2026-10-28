package com.ats.intelligence.controller;

import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.TailorRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEndToEndWorkflow() throws Exception {
        // 1. Clear previous session state
        mockMvc.perform(delete("/api/resume/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 2. Load synthetic sample resume
        mockMvc.perform(post("/api/resume/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Alex Mercer")))
                .andExpect(jsonPath("$.email", is("alex.mercer.dev@example.com")))
                .andExpect(jsonPath("$.verified", is(true)));

        // 3. Retrieve verified profile
        mockMvc.perform(get("/api/resume/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Alex Mercer")));

        // 4. Update profile
        MasterResumeDto updated = new MasterResumeDto();
        updated.setFullName("Omkar Mede, Full Stack Engineer");
        updated.setEmail("omkarmede@gmail.com");
        updated.setSkills(java.util.List.of("React", "Node.js", "PostgreSQL", "Next.js"));

        mockMvc.perform(put("/api/resume/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Omkar Mede, Full Stack Engineer")))
                .andExpect(jsonPath("$.verified", is(true)));

        // 5. Tailor against Job Description
        TailorRequestDto request = new TailorRequestDto();
        request.setTargetRole("Senior Full Stack Developer");
        request.setTargetCompany("FinTech Next");
        request.setJobDescriptionText("""
                Role: Senior Full Stack Developer
                Company: FinTech Next
                Requirements:
                - Strong expertise in React, Node.js, and PostgreSQL.
                - Experience building high-performance web applications.
                """);
        request.setUseAi(false); // Deterministic offline mode

        mockMvc.perform(post("/api/resume/tailor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Omkar Mede, Full Stack Engineer")))
                .andExpect(jsonPath("$.targetRole", is("Senior Full Stack Developer")))
                .andExpect(jsonPath("$.matchReport.overallScore", greaterThan(0.0)));

        // 6. Download PDF
        mockMvc.perform(get("/api/resume/download/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")));

        // 7. Download DOCX
        mockMvc.perform(get("/api/resume/download/docx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".docx")));

        // 8. Ollama status endpoint
        mockMvc.perform(get("/api/ollama/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl", notNullValue()));
    }

    @Test
    void testTailorRejectsEmptyJobDescription() throws Exception {
        TailorRequestDto emptyReq = new TailorRequestDto();
        emptyReq.setJobDescriptionText("");

        mockMvc.perform(post("/api/resume/tailor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isBadRequest());
    }
}
