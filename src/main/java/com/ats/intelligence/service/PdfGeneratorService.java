package com.ats.intelligence.service;

import com.ats.intelligence.dto.CertificationDto;
import com.ats.intelligence.dto.EducationDto;
import com.ats.intelligence.dto.ProjectDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PdfGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorService.class);

    private static final float MARGIN_LEFT = 36f;
    private static final float MARGIN_RIGHT = 36f;
    private static final float MARGIN_TOP = 40f;
    private static final float MARGIN_BOTTOM = 36f;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float USABLE_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    public byte[] generatePdf(TailoredResumeDto resume) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            PdfContext ctx = new PdfContext(document, fontRegular, fontBold, fontItalic);
            ctx.newPage();

            // 1. Centered Header: Name, Headline, Contact details
            renderHeader(ctx, resume);

            // 2. Professional Summary
            if (resume.getTailoredSummary() != null && !resume.getTailoredSummary().isBlank()) {
                renderSectionHeading(ctx, "PROFESSIONAL SUMMARY");
                renderParagraph(ctx, resume.getTailoredSummary(), ctx.fontRegular, 9.8f, 13.0f);
                ctx.cursorY -= 4f;
            }

            // 3. Work Experience
            if (resume.getTailoredExperiences() != null && !resume.getTailoredExperiences().isEmpty()) {
                renderSectionHeading(ctx, "EXPERIENCE");
                for (WorkExperienceDto exp : resume.getTailoredExperiences()) {
                    renderExperienceEntry(ctx, exp);
                }
            }

            // 4. Projects
            if (resume.getTailoredProjects() != null && !resume.getTailoredProjects().isEmpty()) {
                renderSectionHeading(ctx, "PROJECTS");
                for (ProjectDto proj : resume.getTailoredProjects()) {
                    renderProjectEntry(ctx, proj);
                }
            }

            // 5. Technical Skills (Categorized with bold category prefixes)
            renderTechnicalSkills(ctx, resume);

            // Check if remaining sections will fit on Page 1.
            // If they will overflow into a 2nd page, break cleanly before EDUCATION
            // so Page 2 starts with EDUCATION, exactly like Omkar Mede's balanced template.
            float remainingEstimate = estimateRemainingHeight(resume);
            if (ctx.cursorY - remainingEstimate < MARGIN_BOTTOM && ctx.cursorY < (PAGE_HEIGHT - MARGIN_TOP - 300f)) {
                ctx.newPage();
            }

            // 6. Education
            if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
                renderSectionHeading(ctx, "EDUCATION");
                for (EducationDto edu : resume.getEducation()) {
                    renderEducationEntry(ctx, edu);
                }
            }

            // 7. Achievements
            if (resume.getAchievements() != null && !resume.getAchievements().isEmpty()) {
                renderSectionHeading(ctx, "ACHIEVEMENTS");
                for (String ach : resume.getAchievements()) {
                    renderBulletPoint(ctx, ach);
                }
                ctx.cursorY -= 4f;
            }

            // 8. Additional Activities & Leadership
            if (resume.getActivities() != null && !resume.getActivities().isEmpty()) {
                renderSectionHeading(ctx, "ADDITIONAL ACTIVITIES & LEADERSHIP");
                for (String act : resume.getActivities()) {
                    renderBulletPoint(ctx, act);
                }
                ctx.cursorY -= 4f;
            }

            // 9. Soft Skills
            if (resume.getSoftSkills() != null && !resume.getSoftSkills().isEmpty()) {
                renderSectionHeading(ctx, "SOFT SKILLS");
                String line = String.join("  ·  ", resume.getSoftSkills());
                renderParagraph(ctx, line, ctx.fontRegular, 9.8f, 13.5f);
                ctx.cursorY -= 4f;
            }

            // 10. Certifications (if present)
            if (resume.getCertifications() != null && !resume.getCertifications().isEmpty()) {
                renderSectionHeading(ctx, "CERTIFICATIONS");
                for (CertificationDto cert : resume.getCertifications()) {
                    renderCertificationEntry(ctx, cert);
                }
            }

            ctx.closeCurrentStream();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            log.info("Generated ATS-compliant PDF resume ({} pages, {} bytes)", document.getNumberOfPages(), baos.size());
            return baos.toByteArray();
        }
    }

    private float estimateRemainingHeight(TailoredResumeDto resume) {
        float h = 0f;
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
            h += 35f + (resume.getEducation().size() * 32f);
        }
        if (resume.getAchievements() != null && !resume.getAchievements().isEmpty()) {
            h += 35f + (resume.getAchievements().size() * 18f);
        }
        if (resume.getActivities() != null && !resume.getActivities().isEmpty()) {
            h += 35f + (resume.getActivities().size() * 18f);
        }
        if (resume.getSoftSkills() != null && !resume.getSoftSkills().isEmpty()) {
            h += 45f;
        }
        if (resume.getCertifications() != null && !resume.getCertifications().isEmpty()) {
            h += 35f + (resume.getCertifications().size() * 18f);
        }
        return h;
    }

    private void renderHeader(PdfContext ctx, TailoredResumeDto resume) throws IOException {
        String name = resume.getFullName() != null ? resume.getFullName().toUpperCase() : "CANDIDATE NAME";
        ctx.ensureSpace(60f);

        // 1. Centered Candidate Name (20pt Bold)
        float nameSize = 20f;
        float nameWidth = ctx.fontBold.getStringWidth(sanitizeText(name)) / 1000f * nameSize;
        float startXName = (PAGE_WIDTH - nameWidth) / 2f;

        ctx.stream.beginText();
        ctx.stream.setFont(ctx.fontBold, nameSize);
        ctx.stream.newLineAtOffset(Math.max(MARGIN_LEFT, startXName), ctx.cursorY);
        ctx.stream.showText(sanitizeText(name));
        ctx.stream.endText();
        ctx.cursorY -= 17f;

        // 2. Centered Headline / Role (10.5pt Regular)
        String headline = resume.getHeadline();
        if (headline == null || headline.isBlank()) {
            headline = resume.getTargetRole();
        }
        if (headline != null && !headline.isBlank()) {
            float headSize = 10.5f;
            float headWidth = ctx.fontRegular.getStringWidth(sanitizeText(headline)) / 1000f * headSize;
            float startXHead = (PAGE_WIDTH - headWidth) / 2f;

            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontRegular, headSize);
            ctx.stream.newLineAtOffset(Math.max(MARGIN_LEFT, startXHead), ctx.cursorY);
            ctx.stream.showText(sanitizeText(headline));
            ctx.stream.endText();
            ctx.cursorY -= 14f;
        }

        // 3. Centered Contact Info (9pt Regular)
        List<String> contactParts = new ArrayList<>();
        if (resume.getLocation() != null && !resume.getLocation().isBlank()) contactParts.add(resume.getLocation());
        if (resume.getPhone() != null && !resume.getPhone().isBlank()) contactParts.add(resume.getPhone());
        if (resume.getEmail() != null && !resume.getEmail().isBlank()) contactParts.add(resume.getEmail());
        if (resume.getLinkedinUrl() != null && !resume.getLinkedinUrl().isBlank()) {
            contactParts.add("LinkedIn");
        }
        if (resume.getGithubUrl() != null && !resume.getGithubUrl().isBlank()) {
            contactParts.add("GitHub");
        }
        if (resume.getPortfolioUrl() != null && !resume.getPortfolioUrl().isBlank()) {
            contactParts.add("Portfolio");
        }

        String contactLine = String.join("  |  ", contactParts);
        float contactSize = 9f;
        float contactWidth = ctx.fontRegular.getStringWidth(sanitizeText(contactLine)) / 1000f * contactSize;
        float startXContact = (PAGE_WIDTH - contactWidth) / 2f;

        ctx.stream.beginText();
        ctx.stream.setFont(ctx.fontRegular, contactSize);
        ctx.stream.newLineAtOffset(Math.max(MARGIN_LEFT, startXContact), ctx.cursorY);
        ctx.stream.showText(sanitizeText(contactLine));
        ctx.stream.endText();
        ctx.cursorY -= 14f;
    }

    private void renderSectionHeading(PdfContext ctx, String heading) throws IOException {
        ctx.ensureSpace(32f);
        ctx.cursorY -= 6f;

        // Heading Text (11.5pt Bold)
        ctx.stream.beginText();
        ctx.stream.setFont(ctx.fontBold, 11.5f);
        ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
        ctx.stream.showText(heading);
        ctx.stream.endText();
        ctx.cursorY -= 3.5f;

        // Clean sharp horizontal divider rule
        ctx.stream.setLineWidth(0.65f);
        ctx.stream.moveTo(MARGIN_LEFT, ctx.cursorY);
        ctx.stream.lineTo(PAGE_WIDTH - MARGIN_RIGHT, ctx.cursorY);
        ctx.stream.stroke();

        ctx.cursorY -= 9f;
    }

    private void renderExperienceEntry(PdfContext ctx, WorkExperienceDto exp) throws IOException {
        ctx.ensureSpace(36f);

        String titleComp = (exp.getJobTitle() != null ? exp.getJobTitle() : "")
                + (exp.getJobTitle() != null && exp.getCompany() != null ? " — " : "")
                + (exp.getCompany() != null ? exp.getCompany() : "");

        String dateStr = (exp.getStartDate() != null ? exp.getStartDate() : "")
                + ((exp.getStartDate() != null || exp.getEndDate() != null) ? " – " : "")
                + (exp.isCurrent() ? "Present" : (exp.getEndDate() != null ? exp.getEndDate() : ""));

        float titleSize = 10.5f;
        float dateSize = 10f;
        float dateWidth = ctx.fontRegular.getStringWidth(sanitizeText(dateStr)) / 1000f * dateSize;

        // Job Title & Company (Left)
        ctx.stream.beginText();
        ctx.stream.setFont(ctx.fontBold, titleSize);
        ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
        ctx.stream.showText(sanitizeText(titleComp));
        ctx.stream.endText();

        // Dates (Right-Aligned)
        if (!dateStr.isBlank()) {
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontRegular, dateSize);
            ctx.stream.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - dateWidth, ctx.cursorY);
            ctx.stream.showText(sanitizeText(dateStr));
            ctx.stream.endText();
        }
        ctx.cursorY -= 13f;

        // Subtitle / domain or location in italic (Line 2)
        if (exp.getLocation() != null && !exp.getLocation().isBlank()) {
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontItalic, 9.2f);
            ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
            ctx.stream.showText(sanitizeText(exp.getLocation()));
            ctx.stream.endText();
            ctx.cursorY -= 12f;
        }

        // Bullets
        if (exp.getBullets() != null) {
            for (String bullet : exp.getBullets()) {
                renderBulletPoint(ctx, bullet);
            }
        }
        ctx.cursorY -= 4f;
    }

    private void renderProjectEntry(PdfContext ctx, ProjectDto proj) throws IOException {
        ctx.ensureSpace(34f);

        String name = proj.getName() != null ? proj.getName() : "Project";
        String techStr = (proj.getTechnologies() != null && !proj.getTechnologies().isEmpty()) ?
                String.join(" · ", proj.getTechnologies()) : "";

        float titleSize = 10.5f;
        float techSize = 9.5f;

        float titleWidth = ctx.fontBold.getStringWidth(sanitizeText(name)) / 1000f * titleSize;
        float techWidth = !techStr.isBlank() ? (ctx.fontItalic.getStringWidth(sanitizeText(techStr)) / 1000f * techSize) : 0f;

        // Prevent overlap: check if Title and Tech Stack fit together with 15pt gap
        boolean fitsOnSameLine = (titleWidth + techWidth + 15f <= USABLE_WIDTH);

        if (fitsOnSameLine) {
            // Project Name (Left)
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontBold, titleSize);
            ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
            ctx.stream.showText(sanitizeText(name));
            ctx.stream.endText();

            // Tech Stack (Right-Aligned)
            if (!techStr.isBlank()) {
                ctx.stream.beginText();
                ctx.stream.setFont(ctx.fontItalic, techSize);
                ctx.stream.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - techWidth, ctx.cursorY);
                ctx.stream.showText(sanitizeText(techStr));
                ctx.stream.endText();
            }
            ctx.cursorY -= 13f;
        } else {
            // Project Name (Line 1)
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontBold, titleSize);
            ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
            ctx.stream.showText(sanitizeText(name));
            ctx.stream.endText();
            ctx.cursorY -= 12f;

            // Tech Stack (Line 2 - Right Aligned)
            if (!techStr.isBlank()) {
                float startX = (techWidth <= USABLE_WIDTH) ? (PAGE_WIDTH - MARGIN_RIGHT - techWidth) : MARGIN_LEFT;
                ctx.stream.beginText();
                ctx.stream.setFont(ctx.fontItalic, techSize);
                ctx.stream.newLineAtOffset(startX, ctx.cursorY);
                ctx.stream.showText(sanitizeText(techStr));
                ctx.stream.endText();
                ctx.cursorY -= 12f;
            }
        }

        // Bullets
        if (proj.getBullets() != null && !proj.getBullets().isEmpty()) {
            for (String b : proj.getBullets()) {
                renderBulletPoint(ctx, b);
            }
        } else if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
            renderBulletPoint(ctx, proj.getDescription());
        }
        ctx.cursorY -= 4f;
    }

    private void renderTechnicalSkills(PdfContext ctx, TailoredResumeDto resume) throws IOException {
        Map<String, List<String>> catSkills = resume.getTailoredCategorizedSkills();
        if (catSkills != null && !catSkills.isEmpty()) {
            renderSectionHeading(ctx, "TECHNICAL SKILLS");
            for (Map.Entry<String, List<String>> entry : catSkills.entrySet()) {
                renderCategorizedSkillLine(ctx, entry.getKey(), entry.getValue());
            }
            ctx.cursorY -= 4f;
        } else if (resume.getTailoredSkills() != null && !resume.getTailoredSkills().isEmpty()) {
            renderSectionHeading(ctx, "TECHNICAL SKILLS");
            String skillsLine = String.join("  |  ", resume.getTailoredSkills());
            renderParagraph(ctx, skillsLine, ctx.fontRegular, 9.8f, 13.5f);
            ctx.cursorY -= 4f;
        }
    }

    private void renderCategorizedSkillLine(PdfContext ctx, String category, List<String> skills) throws IOException {
        String catPrefix = category + ": ";
        String skillsText = String.join(", ", skills);
        float fontSize = 9.8f;
        float lineHeight = 13.5f;

        float prefixWidth = ctx.fontBold.getStringWidth(sanitizeText(catPrefix)) / 1000f * fontSize;
        float firstLineWidth = USABLE_WIDTH - prefixWidth;

        List<String> wrappedSkills = wrapText(skillsText, ctx.fontRegular, fontSize, firstLineWidth, USABLE_WIDTH);
        ctx.ensureSpace(Math.max(1, wrappedSkills.size()) * lineHeight);

        for (int i = 0; i < wrappedSkills.size(); i++) {
            String text = wrappedSkills.get(i);
            ctx.stream.beginText();
            if (i == 0) {
                // Category in Bold
                ctx.stream.setFont(ctx.fontBold, fontSize);
                ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
                ctx.stream.showText(sanitizeText(catPrefix));

                // Skills remainder in Regular
                ctx.stream.setFont(ctx.fontRegular, fontSize);
                ctx.stream.newLineAtOffset(prefixWidth, 0);
                ctx.stream.showText(sanitizeText(text));
            } else {
                // Subsequent lines wrap to MARGIN_LEFT
                ctx.stream.setFont(ctx.fontRegular, fontSize);
                ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
                ctx.stream.showText(sanitizeText(text));
            }
            ctx.stream.endText();
            ctx.cursorY -= lineHeight;
        }
    }

    private void renderEducationEntry(PdfContext ctx, EducationDto edu) throws IOException {
        ctx.ensureSpace(32f);

        String institution = edu.getInstitution() != null ? edu.getInstitution() : "";
        String dateStr = edu.getGraduationDate() != null ? edu.getGraduationDate() : "";

        float instSize = 10.5f;
        float dateSize = 10f;
        float dateWidth = ctx.fontRegular.getStringWidth(sanitizeText(dateStr)) / 1000f * dateSize;

        // Line 1: Institution (Left) and Dates (Right)
        ctx.stream.beginText();
        ctx.stream.setFont(ctx.fontBold, instSize);
        ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
        ctx.stream.showText(sanitizeText(institution));
        ctx.stream.endText();

        if (!dateStr.isBlank()) {
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontRegular, dateSize);
            ctx.stream.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - dateWidth, ctx.cursorY);
            ctx.stream.showText(sanitizeText(dateStr));
            ctx.stream.endText();
        }
        ctx.cursorY -= 13f;

        // Line 2: Degree in Field of Study in Italic
        String degreeMajor = (edu.getDegree() != null ? edu.getDegree() : "")
                + (edu.getFieldOfStudy() != null ? " — " + edu.getFieldOfStudy() : "")
                + (edu.getGpa() != null && !edu.getGpa().isBlank() ? " | GPA: " + edu.getGpa() : "");

        if (!degreeMajor.isBlank()) {
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontItalic, 9.2f);
            ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
            ctx.stream.showText(sanitizeText(degreeMajor));
            ctx.stream.endText();
            ctx.cursorY -= 12f;
        }
        ctx.cursorY -= 4f;
    }

    private void renderCertificationEntry(PdfContext ctx, CertificationDto cert) throws IOException {
        ctx.ensureSpace(16f);
        String line = cert.getName() + (cert.getIssuingOrganization() != null ? " — " + cert.getIssuingOrganization() : "");
        renderBulletPoint(ctx, line);
    }

    private void renderBulletPoint(PdfContext ctx, String bulletText) throws IOException {
        float bulletX = MARGIN_LEFT + 8f;
        float textX = MARGIN_LEFT + 18f;
        float textWidth = USABLE_WIDTH - 18f;

        float fontSize = 9.5f;
        float lineHeight = 12.8f;

        List<String> lines = wrapText(bulletText, ctx.fontRegular, fontSize, textWidth, textWidth);
        if (lines.isEmpty()) return;

        ctx.ensureSpace(lines.size() * lineHeight + 2f);

        // 1. Draw bullet symbol "•" at bulletX
        ctx.stream.beginText();
        ctx.stream.setFont(ctx.fontBold, 10f);
        ctx.stream.newLineAtOffset(bulletX, ctx.cursorY);
        ctx.stream.showText("•");
        ctx.stream.endText();

        // 2. Draw wrapped lines starting at textX (Hanging Indent)
        for (String line : lines) {
            ctx.stream.beginText();
            ctx.stream.setFont(ctx.fontRegular, fontSize);
            ctx.stream.newLineAtOffset(textX, ctx.cursorY);
            ctx.stream.showText(sanitizeText(line));
            ctx.stream.endText();
            ctx.cursorY -= lineHeight;
        }
        ctx.cursorY -= 2f;
    }

    private void renderParagraph(PdfContext ctx, String text, PDFont font, float fontSize, float lineHeight) throws IOException {
        List<String> lines = wrapText(text, font, fontSize, USABLE_WIDTH, USABLE_WIDTH);
        for (String line : lines) {
            ctx.ensureSpace(lineHeight);
            ctx.stream.beginText();
            ctx.stream.setFont(font, fontSize);
            ctx.stream.newLineAtOffset(MARGIN_LEFT, ctx.cursorY);
            ctx.stream.showText(sanitizeText(line));
            ctx.stream.endText();
            ctx.cursorY -= lineHeight;
        }
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float firstLineWidth, float subsequentLineWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String[] words = sanitizeText(text).split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        float currentMax = firstLineWidth;

        for (String word : words) {
            String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width > currentMax && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
                currentMax = subsequentLineWidth;
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                .replace("²", "2")
                .replaceAll("[^\\x20-\\x7E\\u00A0-\\u00FF\\u2022\\u2013\\u2014\\u00B7]", " ");
    }

    private static class PdfContext {
        final PDDocument document;
        final PDFont fontRegular;
        final PDFont fontBold;
        final PDFont fontItalic;

        PDPageContentStream stream;
        float cursorY;

        PdfContext(PDDocument document, PDFont fontRegular, PDFont fontBold, PDFont fontItalic) {
            this.document = document;
            this.fontRegular = fontRegular;
            this.fontBold = fontBold;
            this.fontItalic = fontItalic;
        }

        void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            cursorY = PAGE_HEIGHT - MARGIN_TOP;
        }

        void ensureSpace(float requiredHeight) throws IOException {
            if (cursorY - requiredHeight < MARGIN_BOTTOM) {
                newPage();
            }
        }

        void closeCurrentStream() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
