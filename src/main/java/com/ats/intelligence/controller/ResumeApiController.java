package com.ats.intelligence.controller;

import com.ats.intelligence.dto.JobDescriptionDto;
import com.ats.intelligence.dto.MasterResumeDto;
import com.ats.intelligence.dto.MatchReportDto;
import com.ats.intelligence.dto.OllamaStatusDto;
import com.ats.intelligence.dto.TailorRequestDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.parser.JobDescriptionParser;
import com.ats.intelligence.parser.ResumeDocumentParser;
import com.ats.intelligence.service.DocxGeneratorService;
import com.ats.intelligence.service.PdfGeneratorService;
import com.ats.intelligence.service.ResumeStorageService;
import com.ats.intelligence.service.ResumeTailoringManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResumeApiController {

    private static final Logger log = LoggerFactory.getLogger(ResumeApiController.class);

    private final ResumeDocumentParser documentParser;
    private final JobDescriptionParser jdParser;
    private final ResumeTailoringManager tailoringManager;
    private final ResumeStorageService storageService;
    private final PdfGeneratorService pdfGeneratorService;
    private final DocxGeneratorService docxGeneratorService;

    public ResumeApiController(ResumeDocumentParser documentParser,
                               JobDescriptionParser jdParser,
                               ResumeTailoringManager tailoringManager,
                               ResumeStorageService storageService,
                               PdfGeneratorService pdfGeneratorService,
                               DocxGeneratorService docxGeneratorService) {
        this.documentParser = documentParser;
        this.jdParser = jdParser;
        this.tailoringManager = tailoringManager;
        this.storageService = storageService;
        this.pdfGeneratorService = pdfGeneratorService;
        this.docxGeneratorService = docxGeneratorService;
    }

    /**
     * 1. Upload Master Resume (PDF, DOCX, or TXT)
     */
    @PostMapping(value = "/resume/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MasterResumeDto> uploadResume(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received resume upload: name='{}', size={} bytes", file.getOriginalFilename(), file.getSize());
        MasterResumeDto parsedProfile = documentParser.parseUploadedFile(file);
        storageService.saveMasterResume(parsedProfile);
        return ResponseEntity.ok(parsedProfile);
    }

    /**
     * Quick-load fictional sample master resume for instant testing and zero-effort demo
     */
    @PostMapping("/resume/sample")
    public ResponseEntity<MasterResumeDto> loadSampleProfile() {
        log.info("Loading synthetic demo master resume.");
        MasterResumeDto sample = storageService.createSyntheticSampleProfile();
        return ResponseEntity.ok(sample);
    }

    /**
     * 2. Get current Master Resume Profile
     */
    @GetMapping("/resume/profile")
    public ResponseEntity<MasterResumeDto> getMasterProfile() {
        MasterResumeDto profile = storageService.getMasterResume();
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(profile);
    }

    /**
     * 3. Update & Verify Master Resume Profile
     */
    @PutMapping("/resume/profile")
    public ResponseEntity<MasterResumeDto> updateMasterProfile(@RequestBody MasterResumeDto updatedProfile) {
        if (updatedProfile == null) {
            throw new IllegalArgumentException("Profile payload cannot be null");
        }
        updatedProfile.setVerified(true);
        storageService.saveMasterResume(updatedProfile);
        log.info("Updated and verified master resume for '{}'", updatedProfile.getFullName());
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * 4. Tailor Resume against Job Description
     */
    @PostMapping("/resume/tailor")
    public ResponseEntity<TailoredResumeDto> tailorResume(@Valid @RequestBody TailorRequestDto request) {
        MasterResumeDto master = storageService.getMasterResume();
        if (master == null) {
            throw new IllegalArgumentException("No master resume found. Please upload and verify your master resume first.");
        }

        // Parse Job Description
        JobDescriptionDto jd = jdParser.parse(
                request.getJobDescriptionText(),
                request.getTargetRole(),
                request.getTargetCompany(),
                request.getPreferredLength()
        );

        // Run tailoring workflow
        TailoredResumeDto tailored = tailoringManager.tailorResume(master, jd, request.isUseAi());
        storageService.saveTailoredResume(tailored);

        log.info("Generated tailored resume version: id='{}', score={}%",
                tailored.getVersionId(),
                tailored.getMatchReport() != null ? tailored.getMatchReport().getOverallScore() : 0);
        return ResponseEntity.ok(tailored);
    }

    /**
     * 5. Allow user to edit the tailored resume before downloading
     */
    @PutMapping("/resume/tailored")
    public ResponseEntity<TailoredResumeDto> updateTailoredResume(@RequestBody TailoredResumeDto updatedTailored) {
        if (updatedTailored == null || updatedTailored.getVersionId() == null) {
            throw new IllegalArgumentException("Tailored resume payload and versionId are required");
        }
        storageService.saveTailoredResume(updatedTailored);
        return ResponseEntity.ok(updatedTailored);
    }

    /**
     * 6. Get Latest Match Report
     */
    @GetMapping("/resume/match-report")
    public ResponseEntity<MatchReportDto> getMatchReport() {
        TailoredResumeDto tailored = storageService.getActiveTailoredResume();
        if (tailored == null || tailored.getMatchReport() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(tailored.getMatchReport());
    }

    /**
     * 7. Download PDF
     */
    @GetMapping("/resume/download/pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam(value = "versionId", required = false) String versionId) throws IOException {
        TailoredResumeDto tailored = versionId != null ?
                storageService.getTailoredResume(versionId) :
                storageService.getActiveTailoredResume();

        if (tailored == null) {
            throw new IllegalArgumentException("No tailored resume found to download. Please generate a tailored resume first.");
        }

        byte[] pdfBytes = pdfGeneratorService.generatePdf(tailored);
        String filename = sanitizeFilename(tailored.getFullName(), tailored.getTargetRole()) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdfBytes.length))
                .body(pdfBytes);
    }

    /**
     * 8. Download DOCX
     */
    @GetMapping("/resume/download/docx")
    public ResponseEntity<byte[]> downloadDocx(@RequestParam(value = "versionId", required = false) String versionId) throws IOException {
        TailoredResumeDto tailored = versionId != null ?
                storageService.getTailoredResume(versionId) :
                storageService.getActiveTailoredResume();

        if (tailored == null) {
            throw new IllegalArgumentException("No tailored resume found to download. Please generate a tailored resume first.");
        }

        byte[] docxBytes = docxGeneratorService.generateDocx(tailored);
        String filename = sanitizeFilename(tailored.getFullName(), tailored.getTargetRole()) + ".docx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(docxBytes.length))
                .body(docxBytes);
    }

    /**
     * 9. Reset / Clear all stored data
     */
    @DeleteMapping("/resume/profile")
    public ResponseEntity<Map<String, Object>> clearProfile() {
        storageService.clearAll();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Master resume and tailored versions successfully cleared.");
        return ResponseEntity.ok(resp);
    }

    /**
     * 10. Check Ollama Local AI Status
     */
    @GetMapping("/ollama/status")
    public ResponseEntity<OllamaStatusDto> getOllamaStatus() {
        return ResponseEntity.ok(tailoringManager.getOllamaStatus());
    }

    /**
     * 11. List all tailored versions
     */
    @GetMapping("/resume/versions")
    public ResponseEntity<List<TailoredResumeDto>> getTailoredVersions() {
        return ResponseEntity.ok(storageService.getAllTailoredVersions());
    }

    private String sanitizeFilename(String candidateName, String role) {
        String namePart = (candidateName != null && !candidateName.isBlank()) ? candidateName.trim() : "Resume";
        String rolePart = (role != null && !role.isBlank()) ? ("_" + role.trim()) : "";
        return (namePart + rolePart + "_Tailored").replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
