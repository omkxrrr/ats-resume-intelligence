# ATS Resume Intelligence

> **Free-first, Truthful, ATS-Optimized Resume Tailoring System**  
> Built with Java 21, Spring Boot 3.3, Apache PDFBox, Apache POI, SQLite, and Thymeleaf.

---

## 🎯 Project Overview

**ATS Resume Intelligence** is a free-first, privacy-respecting resume optimization web application designed to tailor resumes specifically to target job descriptions while enforcing **Strict Fact Preservation**. 

Unlike conventional AI tools that fabricate metrics, hallucinate skills, or inject keyword-stuffing hacks that harm candidates, ATS Resume Intelligence treats your verified master resume as the **unshakeable ground truth**.

- **Zero Paid APIs / Subscriptions**: Runs 100% locally on your machine with zero cloud dependencies.
- **Strict Fact-Preservation Engine**: Never invents employment history, companies, dates, degrees, metrics ($X, X%), or unverified technologies.
- **Categorized Gap Analysis**: Missing skills from the job description are transparently classified as `MISSING` in a detailed gap report rather than fabricated onto your resume.
- **Transparent ATS Scoring Engine**: Clear weighted score based on configurable criteria, explaining exactly why and where keywords matched.
- **Dual-Engine Architecture**: Includes a high-speed, deterministic offline Java rule engine as the primary engine and fallback, with optional local LLM integration via **Ollama**.
- **Real File Generation**: Generates clean, single-column, text-selectable **PDF** and **DOCX** files formatted strictly according to modern ATS parsing standards.

---

## 🏗️ Architecture & Tech Stack

- **Java Version**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.4 (Spring MVC, Bean Validation, Spring JDBC)
- **Frontend**: Thymeleaf, HTML5, Modern SaaS CSS3, Vanilla JavaScript (zero node/npm build complexity)
- **PDF Extraction & Generation**: Apache PDFBox 3.0.3 (text-selectable, single-column layout, standard fonts)
- **DOCX Extraction & Generation**: Apache POI 5.3.0 (`poi-ooxml`)
- **Persistence**: SQLite (via `sqlite-jdbc` and `JdbcTemplate`) with in-memory session resilience
- **Optional Local AI**: Ollama HTTP integration (`http://localhost:11434`) with automated fallback
- **Build Tool**: Maven 3.9+
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc (20 passing tests)

---

## 📋 Project Structure

```
TailoedR AI/
├── pom.xml                               # Maven project configuration
├── mvn.cmd                               # Windows Maven execution helper
├── ats_resume.db                         # Local SQLite database (auto-created)
├── src/
│   ├── main/
│   │   ├── java/com/ats/intelligence/
│   │   │   ├── AtsResumeIntelligenceApplication.java  # Main application entry point
│   │   │   ├── config/
│   │   │   │   ├── AtsScoringProperties.java          # Configurable ATS category weights
│   │   │   │   └── OllamaProperties.java              # Ollama model and endpoint properties
│   │   │   ├── controller/
│   │   │   │   ├── WebController.java                 # Serves frontend UI
│   │   │   │   ├── ResumeApiController.java           # Complete REST API
│   │   │   │   └── GlobalExceptionHandler.java        # Centralized exception handling
│   │   │   ├── dto/                                   # Data transfer objects (Master, Tailored, Reports)
│   │   │   ├── model/
│   │   │   │   ├── SkillSupportLevel.java             # VERIFIED, PARTIALLY_SUPPORTED, MISSING
│   │   │   │   └── RequirementCategory.java           # REQUIRED, PREFERRED, RESPONSIBILITIES, etc.
│   │   │   ├── parser/
│   │   │   │   ├── ResumeDocumentParser.java          # Multi-format document parser & magic-byte validator
│   │   │   │   ├── PdfResumeParser.java               # PDFBox text extraction
│   │   │   │   ├── DocxResumeParser.java              # Apache POI docx extraction
│   │   │   │   ├── TextResumeExtractor.java           # Contact, section, and bullet regex parsing
│   │   │   │   └── JobDescriptionParser.java          # JD sectioning & keyword categorization
│   │   │   ├── engine/
│   │   │   │   ├── KeywordNormalizer.java             # Synonym canonicalization & token matching
│   │   │   │   ├── AtsScoringEngine.java              # Transparent weighted match calculation
│   │   │   │   └── FactPreservationValidator.java     # Hallucination guardrail & metrics checker
│   │   │   └── service/
│   │   │       ├── ResumeTailoringService.java        # Strategy interface for tailoring
│   │   │       ├── RuleBasedResumeTailoringService.java # Deterministic offline Java implementation
│   │   │       ├── OllamaResumeTailoringService.java    # Local LLM integration with fallback
│   │   │       ├── ResumeTailoringManager.java        # Engine orchestrator
│   │   │       ├── ResumeStorageService.java          # SQLite & in-memory session persistence
│   │   │       ├── PdfGeneratorService.java           # ATS single-column PDF generation
│   │   │       └── DocxGeneratorService.java          # Valid DOCX generation
│   │   └── resources/
│   │       ├── application.properties                 # App configuration & scoring weights
│   │       ├── static/
│   │       │   ├── css/style.css                      # Modern responsive SaaS theme
│   │       │   └── js/app.js                          # Client-side workflow controller
│   │       └── templates/
│   │           └── index.html                         # Thymeleaf 5-step dashboard
│   └── test/
│       └── java/com/ats/intelligence/
│           ├── controller/ResumeApiControllerTest.java      # End-to-end REST & download tests
│           ├── engine/AtsScoringEngineTest.java            # Transparent scoring & weight tests
│           ├── engine/FactPreservationValidatorTest.java   # Fact-guardrail & metric integrity tests
│           ├── parser/JobDescriptionParserTest.java        # JD categorization & synonyms tests
│           ├── parser/ResumeDocumentParserTest.java        # PDF, DOCX, and text extraction tests
│           ├── service/PdfAndDocxGeneratorServiceTest.java # PDF & DOCX text verification tests
│           └── service/RuleBasedResumeTailoringServiceTest.java # Action verbs & ranking tests
└── README.md
```

---

## ⚡ Prerequisites

To run this application on Windows (or macOS / Linux):

1. **Java Development Kit (JDK) 21**:
   - Verify installation: `java -version`
   - Download from [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) or [Eclipse Adoptium Temurin 21](https://adoptium.net/).
2. **Apache Maven 3.9+** (or use the included `mvn.cmd` wrapper).
3. **Hardware Requirements**:
   - Any standard PC with **8 GB RAM** and integrated graphics.
   - Low CPU and memory footprint: runs with standard heap (~150MB RSS).

---

## 🚀 Quick Start (Running on Windows)

### 1. Build and Run Tests
Open PowerShell or Command Prompt in the project folder and run:
```powershell
.\mvn.cmd test
```
*All 20 unit and integration tests will execute, validating document parsing, fact preservation, PDF/DOCX generation, and REST endpoints.*

### 2. Package the Application
```powershell
.\mvn.cmd package
```

### 3. Launch the Web Application
```powershell
java -jar target\ats-resume-intelligence-1.0.0.jar
```
*Alternatively, you can run directly with Maven:*
```powershell
.\mvn.cmd spring-boot:run
```

### 4. Access the Dashboard
Open your web browser and navigate to:
```
http://localhost:8080/
```

---

## 🔄 Core User Workflow

The application guides you through a seamless 5-step workflow:

```
[ Step 1: Upload Master Resume ]  (PDF, DOCX, TXT or Sample)
               │
               ▼
[ Step 2: Verify Master Profile ] (Confirm extracted facts, skills, bullets)
               │
               ▼
[ Step 3: Target Job Description ] (Paste JD, specify target role & options)
               │
               ▼
[ Step 4: Review Tailored Resume ] (Before/After comparison, live edit)
               │
               ▼
[ Step 5: ATS Match & Downloads ] (View match score, gap report, download PDF/DOCX)
```

### Step 1: Upload Master Resume
- Drag & drop your PDF, DOCX, or TXT resume into the upload zone.
- *Quick Demo*: Click **"Load Fictional Sample Resume (Alex Mercer)"** to instantly load a complete synthetic profile without needing an existing resume file!

### Step 2: Verify Master Profile
- The parser extracts your full name, email, phone, location, links, professional summary, skills, experience with bullet points, education, projects, and certifications.
- Review and correct any details.
- Click **"Confirm & Save Verified Profile"**. These facts are locked as your verified source of truth.

### Step 3: Target Job Description
- Paste any complete job description into the large text area.
- Optionally enter or adjust the target role and company.
- Select target length (Auto, 1-Page, 2-Page).
- Choose your Tailoring Engine:
  - **Deterministic Java Engine (Default)**: Fast, fully functional offline, 0 external calls.
  - **Local Ollama AI (Optional)**: Utilizes a local LLM if installed.
- Click **"Generate Tailored Resume & ATS Match Report"**.

### Step 4: Review Tailored Resume (Before & After)
- View side-by-side comparison of your original master resume versus the tailored version.
- Verified skills are reordered by relevance to the JD.
- Action verbs are strengthened (e.g., *"Responsible for developing..."* $\rightarrow$ *"Developed and implemented..."*).
- Bullet points are ranked by relevance to target responsibilities.
- You can directly edit the tailored summary and bullets before downloading.

### Step 5: ATS Match Intelligence & Downloads
- **Overall ATS Match Score**: Transparent gauge indicating alignment percentage.
- **Category Progress Bars**: Breakdown across Required Skills (40%), Preferred Skills (15%), Responsibilities (20%), Tools (15%), and Education/Experience (10%).
- **Quality & Truthfulness Checklist**: Verifies contact completeness, single-column formatting, and 0 hallucinated claims.
- **Requirement Alignment & Gap Table**:
  - Filter by `Matched`, `Partial`, or `Missing`.
  - Displays the exact requirement, support level badge, direct evidence snippet found in your resume, and a factual action recommendation.
- **Instant Downloads**: Click **"Download ATS PDF"** or **"Download ATS DOCX"** to get your ready-to-submit files.

---

## 🛡️ Strict Fact-Preservation Engine

The cornerstone of ATS Resume Intelligence is **ground-truth integrity**:
1. **Source of Truth**: The candidate's verified master resume cannot be overridden.
2. **Identity & Credentials Locked**: Name, email, phone, college, degree, employer names, job titles, and employment dates are never changed.
3. **Exact Metrics Preservation**: All numbers, revenue, dollar figures, percentages, and performance metrics are preserved verbatim. If an AI or user attempts to inject an unsupported metric (e.g. *"increased throughput by 92%"* when the original was *"increased throughput"*), the system flags it as an unsupported claim and reverts to the verified master bullet point.
4. **No Fabricated Skills**: Every skill on the tailored resume must have direct evidence in the master profile. Missing skills from the JD are reported in the Gap Report, never hallucinated.

---

## 📊 ATS Optimization & Transparent Scoring

ATS algorithms vary significantly between platforms (Workday, Taleo, Greenhouse, Lever, iCIMS). This application calculates a transparent, weighted heuristic match score:

$$\text{Category Score} = \frac{\text{Verified Matches} \times 1.0 + \text{Partial Matches} \times 0.5}{\text{Total Category Requirements}} \times 100$$

### Configurable Category Weights (`application.properties`):
| Category | Default Weight | Description |
|---|---|---|
| **Required Skills** | `40%` | Essential technical competencies explicitly required |
| **Preferred Skills** | `15%` | Nice-to-have or bonus qualifications |
| **Responsibilities** | `20%` | Core duties and operational expectations |
| **Tools & Platforms** | `15%` | Specific databases, cloud providers, and frameworks |
| **Education & Experience**| `10%` | Degrees, certifications, and years of experience |

> **Fair Scoring Guarantee**: If a job description does not specify items for a category (e.g., no preferred skills listed), its weight is **dynamically redistributed** across the remaining categories so the candidate is never penalized for an incomplete job posting.

---

## 🦙 Optional Local Ollama Integration

ATS Resume Intelligence works completely offline out of the box using its built-in rule engine. If you want to use local AI models, you can optionally connect **Ollama**:

### How it works:
1. Install [Ollama](https://ollama.com/) on Windows.
2. Pull a compact model suitable for 8 GB RAM systems:
   ```powershell
   ollama pull qwen2.5:1.5b
   ```
   *(Or `ollama pull llama3.2:3b`)*
3. Ollama runs on `http://localhost:11434`.
4. In `application.properties`:
   ```properties
   ats.ollama.enabled=true
   ats.ollama.model=qwen2.5:1.5b
   ats.ollama.timeout-seconds=25
   ```
5. When enabled, the web UI automatically detects the model and activates the "Local Ollama AI" option in the engine selector.
6. **Built-in Guardrails**:
   - All input documents are sanitized as passive data (ignores prompt injection attempts).
   - JSON Schema enforcement validates structured output.
   - If Ollama is offline, times out, or hallucinates an unsupported claim, the application **automatically and safely falls back** to the deterministic rule engine.

---

## ⚠️ Limitations & Honest Disclaimer

- **No ATS Guarantee**: This application uses transparent keyword and requirement heuristics based on standard industry guidelines. Because different ATS vendors (and different corporate recruiter filters) use proprietary parsing algorithms, **no score guarantees an interview or recruiter ranking**.
- **Human Review**: Resumes are ultimately read by hiring managers and human recruiters. Tailoring improves relevance and clarity, but candidates should always review generated text for personal voice and precision.
- **Image Scans**: The PDF parser relies on text-layer extraction via Apache PDFBox. Resumes saved as pure scanned raster images without OCR cannot be extracted.

---

## 🔒 Security & Privacy

- **Local-First Architecture**: Your resume data is processed strictly on your computer (`localhost`).
- **Zero Third-Party Cloud Transmission**: No resumes, names, or contact details are sent to OpenAI, Anthropic, Google, or any external service by default.
- **File Validation**: Enforces a 5 MB upload limit, checks file extensions, validates file magic bytes (`%PDF` and `PK` zip containers), and prevents path traversal.
- **Session Privacy**: A prominent **"Clear / Reset"** button allows you to delete all in-memory and local SQLite records at any time.

---

## 🧪 Automated Test Suite

Run the full automated test suite anytime with:
```powershell
.\mvn.cmd test
```

| Test Class | Focus Area | Status |
|---|---|---|
| `ResumeDocumentParserTest` | PDF, DOCX, TXT parsing, contact extraction, corrupted files | 7 Passed |
| `JobDescriptionParserTest` | JD parsing, section scopes, synonym normalization | 2 Passed |
| `AtsScoringEngineTest` | Transparent weighted match calculation, dynamic redistribution | 2 Passed |
| `FactPreservationValidatorTest` | Rejection of fabricated skills, metric protection, identity lock | 4 Passed |
| `RuleBasedResumeTailoringServiceTest`| Action verb enhancement, skill ranking, 0 hallucinations | 1 Passed |
| `PdfAndDocxGeneratorServiceTest` | PDFBox and POI generation, text extractability verification | 2 Passed |
| `ResumeApiControllerTest` | End-to-end REST API, profile update, PDF/DOCX downloads | 2 Passed |
| **Total** | | **20 / 20 Passed** |
