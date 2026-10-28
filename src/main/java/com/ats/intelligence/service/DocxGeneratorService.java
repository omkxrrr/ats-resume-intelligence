package com.ats.intelligence.service;

import com.ats.intelligence.dto.CertificationDto;
import com.ats.intelligence.dto.EducationDto;
import com.ats.intelligence.dto.ProjectDto;
import com.ats.intelligence.dto.TailoredResumeDto;
import com.ats.intelligence.dto.WorkExperienceDto;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocxGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DocxGeneratorService.class);
    private static final String FONT_FAMILY = "Calibri";

    public byte[] generateDocx(TailoredResumeDto resume) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            setDocumentMargins(document);

            // 1. Header: Candidate Name (Centered, 20pt Bold)
            XWPFParagraph namePara = document.createParagraph();
            namePara.setAlignment(ParagraphAlignment.CENTER);
            namePara.setSpacingBefore(0);
            namePara.setSpacingAfter(30);
            XWPFRun nameRun = namePara.createRun();
            nameRun.setText(resume.getFullName() != null ? resume.getFullName().toUpperCase() : "CANDIDATE NAME");
            nameRun.setFontFamily(FONT_FAMILY);
            nameRun.setFontSize(20);
            nameRun.setBold(true);
            nameRun.setColor("0F172A");

            // Sub-headline / Role (Centered, 10.5pt)
            String headline = resume.getHeadline() != null ? resume.getHeadline() : resume.getTargetRole();
            if (headline != null && !headline.isBlank()) {
                XWPFParagraph headPara = document.createParagraph();
                headPara.setAlignment(ParagraphAlignment.CENTER);
                headPara.setSpacingAfter(30);
                XWPFRun headRun = headPara.createRun();
                headRun.setText(headline);
                headRun.setFontFamily(FONT_FAMILY);
                headRun.setFontSize(10.5);
                headRun.setColor("1E293B");
            }

            // Contact Info (Centered, 9.5pt)
            List<String> contacts = new ArrayList<>();
            if (resume.getLocation() != null && !resume.getLocation().isBlank()) contacts.add(resume.getLocation());
            if (resume.getPhone() != null && !resume.getPhone().isBlank()) contacts.add(resume.getPhone());
            if (resume.getEmail() != null && !resume.getEmail().isBlank()) contacts.add(resume.getEmail());
            if (resume.getLinkedinUrl() != null && !resume.getLinkedinUrl().isBlank()) {
                contacts.add("LinkedIn");
            }
            if (resume.getGithubUrl() != null && !resume.getGithubUrl().isBlank()) {
                contacts.add("GitHub");
            }
            if (resume.getPortfolioUrl() != null && !resume.getPortfolioUrl().isBlank()) {
                contacts.add("Portfolio");
            }

            XWPFParagraph contactPara = document.createParagraph();
            contactPara.setAlignment(ParagraphAlignment.CENTER);
            contactPara.setSpacingAfter(140);
            XWPFRun contactRun = contactPara.createRun();
            contactRun.setText(String.join("  |  ", contacts));
            contactRun.setFontFamily(FONT_FAMILY);
            contactRun.setFontSize(9.5);
            contactRun.setColor("334155");

            // 2. Professional Summary
            if (resume.getTailoredSummary() != null && !resume.getTailoredSummary().isBlank()) {
                addSectionHeading(document, "PROFESSIONAL SUMMARY");
                XWPFParagraph sumPara = document.createParagraph();
                sumPara.setSpacingAfter(100);
                XWPFRun sumRun = sumPara.createRun();
                sumRun.setText(resume.getTailoredSummary());
                sumRun.setFontFamily(FONT_FAMILY);
                sumRun.setFontSize(10);
            }

            // 3. Work Experience
            if (resume.getTailoredExperiences() != null && !resume.getTailoredExperiences().isEmpty()) {
                addSectionHeading(document, "EXPERIENCE");
                for (WorkExperienceDto exp : resume.getTailoredExperiences()) {
                    addExperienceEntry(document, exp);
                }
            }

            // 4. Projects
            if (resume.getTailoredProjects() != null && !resume.getTailoredProjects().isEmpty()) {
                addSectionHeading(document, "PROJECTS");
                for (ProjectDto proj : resume.getTailoredProjects()) {
                    addProjectEntry(document, proj);
                }
            }

            // 5. Technical Skills (Categorized)
            addTechnicalSkills(document, resume);

            // Natural page break before EDUCATION if content is a multi-page resume
            if (isMultiPageResume(resume)) {
                XWPFParagraph pageBreakPara = document.createParagraph();
                pageBreakPara.setPageBreak(true);
            }

            // 6. Education
            if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
                addSectionHeading(document, "EDUCATION");
                for (EducationDto edu : resume.getEducation()) {
                    addEducationEntry(document, edu);
                }
            }

            // 7. Achievements
            if (resume.getAchievements() != null && !resume.getAchievements().isEmpty()) {
                addSectionHeading(document, "ACHIEVEMENTS");
                for (String ach : resume.getAchievements()) {
                    addBulletPoint(document, ach);
                }
            }

            // 8. Additional Activities & Leadership
            if (resume.getActivities() != null && !resume.getActivities().isEmpty()) {
                addSectionHeading(document, "ADDITIONAL ACTIVITIES & LEADERSHIP");
                for (String act : resume.getActivities()) {
                    addBulletPoint(document, act);
                }
            }

            // 9. Soft Skills
            if (resume.getSoftSkills() != null && !resume.getSoftSkills().isEmpty()) {
                addSectionHeading(document, "SOFT SKILLS");
                XWPFParagraph softPara = document.createParagraph();
                softPara.setSpacingAfter(80);
                XWPFRun softRun = softPara.createRun();
                softRun.setText(String.join("  ·  ", resume.getSoftSkills()));
                softRun.setFontFamily(FONT_FAMILY);
                softRun.setFontSize(10);
            }

            // 10. Certifications (if present)
            if (resume.getCertifications() != null && !resume.getCertifications().isEmpty()) {
                addSectionHeading(document, "CERTIFICATIONS");
                for (CertificationDto cert : resume.getCertifications()) {
                    addBulletPoint(document, cert.getName() + (cert.getIssuingOrganization() != null ? " — " + cert.getIssuingOrganization() : ""));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            log.info("Generated ATS-compliant DOCX resume ({} bytes)", baos.size());
            return baos.toByteArray();
        }
    }

    private void addSectionHeading(XWPFDocument document, String title) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(140);
        para.setSpacingAfter(50);
        para.setBorderBottom(Borders.SINGLE);

        XWPFRun run = para.createRun();
        run.setText(title);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(11.5);
        run.setBold(true);
        run.setColor("0F172A");
    }

    private void addExperienceEntry(XWPFDocument document, WorkExperienceDto exp) {
        String titleComp = (exp.getJobTitle() != null ? exp.getJobTitle() : "")
                + (exp.getJobTitle() != null && exp.getCompany() != null ? " — " : "")
                + (exp.getCompany() != null ? exp.getCompany() : "");

        String dates = (exp.getStartDate() != null ? exp.getStartDate() : "")
                + " – " + (exp.isCurrent() ? "Present" : (exp.getEndDate() != null ? exp.getEndDate() : ""));

        addTwoColumnHeader(document, titleComp, true, 10.5, dates, false, 9.5, "475569");

        // Subtitle/domain line (Line 2)
        if (exp.getLocation() != null && !exp.getLocation().isBlank()) {
            XWPFParagraph subPara = document.createParagraph();
            subPara.setSpacingAfter(30);
            XWPFRun subRun = subPara.createRun();
            subRun.setText(exp.getLocation());
            subRun.setFontFamily(FONT_FAMILY);
            subRun.setFontSize(9.2);
            subRun.setItalic(true);
            subRun.setColor("475569");
        }

        // Bullets
        if (exp.getBullets() != null) {
            for (String b : exp.getBullets()) {
                addBulletPoint(document, b);
            }
        }
    }

    private void addProjectEntry(XWPFDocument document, ProjectDto proj) {
        String name = proj.getName() != null ? proj.getName() : "Project";
        String techStr = (proj.getTechnologies() != null && !proj.getTechnologies().isEmpty()) ?
                String.join(" · ", proj.getTechnologies()) : "";

        addTwoColumnHeader(document, name, true, 10.5, techStr, true, 9.2, "334155");

        if (proj.getBullets() != null && !proj.getBullets().isEmpty()) {
            for (String b : proj.getBullets()) {
                addBulletPoint(document, b);
            }
        } else if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
            addBulletPoint(document, proj.getDescription());
        }
    }

    private void addTwoColumnHeader(XWPFDocument document, String leftText, boolean leftBold, double leftSize,
                                    String rightText, boolean rightItalic, double rightSize, String rightColor) {
        XWPFTable table = document.createTable(1, 2);
        table.removeBorders();
        table.setWidth("100%");

        XWPFTableRow row = table.getRow(0);

        // Left Cell (70% width)
        XWPFTableCell leftCell = row.getCell(0);
        leftCell.setWidth("70%");
        XWPFParagraph leftPara = leftCell.getParagraphs().get(0);
        leftPara.setSpacingBefore(40);
        leftPara.setSpacingAfter(20);
        leftPara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun leftRun = leftPara.createRun();
        leftRun.setText(leftText);
        leftRun.setFontFamily(FONT_FAMILY);
        leftRun.setFontSize(leftSize);
        leftRun.setBold(leftBold);
        leftRun.setColor("0F172A");

        // Right Cell (30% width)
        XWPFTableCell rightCell = row.getCell(1);
        rightCell.setWidth("30%");
        XWPFParagraph rightPara = rightCell.getParagraphs().get(0);
        rightPara.setSpacingBefore(40);
        rightPara.setSpacingAfter(20);
        rightPara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun rightRun = rightPara.createRun();
        rightRun.setText(rightText);
        rightRun.setFontFamily(FONT_FAMILY);
        rightRun.setFontSize(rightSize);
        rightRun.setItalic(rightItalic);
        rightRun.setColor(rightColor != null ? rightColor : "475569");
    }

    private void addTechnicalSkills(XWPFDocument document, TailoredResumeDto resume) {
        Map<String, List<String>> catSkills = resume.getTailoredCategorizedSkills();
        if (catSkills != null && !catSkills.isEmpty()) {
            addSectionHeading(document, "TECHNICAL SKILLS");
            for (Map.Entry<String, List<String>> entry : catSkills.entrySet()) {
                XWPFParagraph para = document.createParagraph();
                para.setSpacingAfter(30);

                XWPFRun catRun = para.createRun();
                catRun.setText(entry.getKey() + ": ");
                catRun.setFontFamily(FONT_FAMILY);
                catRun.setFontSize(10);
                catRun.setBold(true);
                catRun.setColor("0F172A");

                XWPFRun skillsRun = para.createRun();
                skillsRun.setText(String.join(", ", entry.getValue()));
                skillsRun.setFontFamily(FONT_FAMILY);
                skillsRun.setFontSize(10);
            }
        } else if (resume.getTailoredSkills() != null && !resume.getTailoredSkills().isEmpty()) {
            addSectionHeading(document, "TECHNICAL SKILLS");
            XWPFParagraph skillsPara = document.createParagraph();
            skillsPara.setSpacingAfter(80);
            XWPFRun skillsRun = skillsPara.createRun();
            skillsRun.setText(String.join("  |  ", resume.getTailoredSkills()));
            skillsRun.setFontFamily(FONT_FAMILY);
            skillsRun.setFontSize(10);
        }
    }

    private void addEducationEntry(XWPFDocument document, EducationDto edu) {
        String inst = edu.getInstitution() != null ? edu.getInstitution() : "";
        String dates = edu.getGraduationDate() != null ? edu.getGraduationDate() : "";

        addTwoColumnHeader(document, inst, true, 10.5, dates, false, 9.5, "475569");

        String degField = (edu.getDegree() != null ? edu.getDegree() : "")
                + (edu.getFieldOfStudy() != null ? " — " + edu.getFieldOfStudy() : "")
                + (edu.getGpa() != null && !edu.getGpa().isBlank() ? " | GPA: " + edu.getGpa() : "");

        if (!degField.isBlank()) {
            XWPFParagraph degPara = document.createParagraph();
            degPara.setSpacingAfter(30);
            XWPFRun degRun = degPara.createRun();
            degRun.setText(degField);
            degRun.setFontFamily(FONT_FAMILY);
            degRun.setFontSize(9.2);
            degRun.setItalic(true);
            degRun.setColor("475569");
        }
    }

    private void addBulletPoint(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingAfter(25);
        para.setIndentationLeft(360); // 0.25 inch indent

        XWPFRun bulletSymbol = para.createRun();
        bulletSymbol.setText("•  ");
        bulletSymbol.setFontFamily(FONT_FAMILY);
        bulletSymbol.setFontSize(10);
        bulletSymbol.setBold(true);
        bulletSymbol.setColor("0F172A");

        XWPFRun run = para.createRun();
        run.setText(text != null ? text.trim() : "");
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(10);
    }

    private boolean isMultiPageResume(TailoredResumeDto resume) {
        int totalItems = 0;
        if (resume.getTailoredExperiences() != null) {
            totalItems += resume.getTailoredExperiences().size() * 3;
        }
        if (resume.getTailoredProjects() != null) {
            totalItems += resume.getTailoredProjects().size() * 2;
        }
        if (resume.getEducation() != null) {
            totalItems += resume.getEducation().size();
        }
        if (resume.getAchievements() != null) {
            totalItems += resume.getAchievements().size();
        }
        if (resume.getActivities() != null) {
            totalItems += resume.getActivities().size();
        }
        return totalItems >= 10;
    }

    private void setDocumentMargins(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        // 720 dxa = 0.5 inch margins
        pageMar.setLeft(BigInteger.valueOf(720));
        pageMar.setRight(BigInteger.valueOf(720));
        pageMar.setTop(BigInteger.valueOf(720));
        pageMar.setBottom(BigInteger.valueOf(720));
    }
}
