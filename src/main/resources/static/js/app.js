/**
 * ATS Resume Intelligence - Frontend Application Controller
 */

const AppState = {
  currentStep: 1,
  masterProfile: null,
  tailoredResume: null,
  ollamaStatus: null
};

document.addEventListener("DOMContentLoaded", () => {
  initDragAndDrop();
  initEventListeners();
  checkOllamaHealth();
  checkExistingMasterProfile();
});

/* ==========================================================================
   INITIALIZATION & HEALTH
   ========================================================================== */

async function checkOllamaHealth() {
  const pill = document.getElementById("ollamaStatusPill");
  const dot = document.getElementById("ollamaDot");
  const text = document.getElementById("ollamaText");
  const aiOption = document.getElementById("useAiOption");

  try {
    const res = await fetch("/api/ollama/status");
    if (res.ok) {
      const data = await res.json();
      AppState.ollamaStatus = data;

      if (data.available) {
        dot.className = "status-dot";
        text.innerText = `Ollama Online (${data.currentModel})`;
        if (aiOption) aiOption.disabled = false;
      } else {
        dot.className = "status-dot offline";
        text.innerText = "Deterministic Engine Active (Ollama Offline)";
      }
    }
  } catch (e) {
    dot.className = "status-dot offline";
    text.innerText = "Deterministic Engine (Rule-based Fallback)";
  }
}

async function checkExistingMasterProfile() {
  try {
    const res = await fetch("/api/resume/profile");
    if (res.ok) {
      const profile = await res.json();
      if (profile && profile.fullName) {
        AppState.masterProfile = profile;
        populateProfileForm(profile);
        setStep(2);
      }
    }
  } catch (e) {
    console.log("No previous session resume found.");
  }
}

/* ==========================================================================
   WORKFLOW NAVIGATION
   ========================================================================== */

function setStep(stepNum) {
  AppState.currentStep = stepNum;

  // Update Stepper Nav
  for (let i = 1; i <= 5; i++) {
    const navItem = document.getElementById(`stepNav${i}`);
    if (navItem) {
      navItem.classList.remove("active");
      if (i === stepNum) {
        navItem.classList.add("active");
      } else if (i < stepNum) {
        navItem.classList.add("completed");
      }
    }

    const stepSection = document.getElementById(`sectionStep${i}`);
    if (stepSection) {
      stepSection.style.display = (i === stepNum) ? "block" : "none";
    }
  }

  window.scrollTo({ top: 0, behavior: "smooth" });
}

function initEventListeners() {
  // Stepper tabs click
  for (let i = 1; i <= 5; i++) {
    const nav = document.getElementById(`stepNav${i}`);
    if (nav) {
      nav.addEventListener("click", () => {
        // Can only jump if data exists
        if (i === 1) setStep(1);
        if (i === 2 && AppState.masterProfile) setStep(2);
        if (i === 3 && AppState.masterProfile) setStep(3);
        if (i === 4 && AppState.tailoredResume) setStep(4);
        if (i === 5 && AppState.tailoredResume) setStep(5);
      });
    }
  }

  // Sample load button
  const loadSampleBtn = document.getElementById("btnLoadSample");
  if (loadSampleBtn) {
    loadSampleBtn.addEventListener("click", loadSampleResume);
  }

  // Profile verify save button
  const saveProfileBtn = document.getElementById("btnSaveProfile");
  if (saveProfileBtn) {
    saveProfileBtn.addEventListener("click", saveVerifiedProfile);
  }

  // Tailor Resume button
  const btnTailor = document.getElementById("btnGenerateTailored");
  if (btnTailor) {
    btnTailor.addEventListener("click", generateTailoredResume);
  }

  // Reset / Clear button
  const resetBtn = document.getElementById("btnResetAll");
  if (resetBtn) {
    resetBtn.addEventListener("click", resetAllData);
  }

  // New skill input enter
  const newSkillInput = document.getElementById("newSkillInput");
  if (newSkillInput) {
    newSkillInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        addSkillTag(newSkillInput.value.trim());
        newSkillInput.value = "";
      }
    });
  }

  // New soft skill input enter
  const newSoftSkillInput = document.getElementById("newSoftSkillInput");
  if (newSoftSkillInput) {
    newSoftSkillInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        addSoftSkillTag(newSoftSkillInput.value.trim());
        newSoftSkillInput.value = "";
      }
    });
  }
}

/* ==========================================================================
   STEP 1: UPLOAD & DRAG-AND-DROP
   ========================================================================== */

function initDragAndDrop() {
  const dropzone = document.getElementById("uploadDropzone");
  const fileInput = document.getElementById("resumeFileInput");

  if (!dropzone || !fileInput) return;

  dropzone.addEventListener("click", () => fileInput.click());

  dropzone.addEventListener("dragover", (e) => {
    e.preventDefault();
    dropzone.classList.add("dragover");
  });

  dropzone.addEventListener("dragleave", () => {
    dropzone.classList.remove("dragover");
  });

  dropzone.addEventListener("drop", (e) => {
    e.preventDefault();
    dropzone.classList.remove("dragover");
    if (e.dataTransfer.files.length > 0) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  });

  fileInput.addEventListener("change", () => {
    if (fileInput.files.length > 0) {
      handleFileUpload(fileInput.files[0]);
    }
  });
}

async function handleFileUpload(file) {
  // Client-side validations
  if (file.size > 5 * 1024 * 1024) {
    showError("File size exceeds 5 MB limit. Please upload a smaller file.");
    return;
  }

  const name = file.name.toLowerCase();
  if (!name.endsWith(".pdf") && !name.endsWith(".docx") && !name.endsWith(".txt")) {
    showError("Unsupported file type. Please upload a .pdf, .docx, or .txt resume.");
    return;
  }

  showLoading(true, "Extracting and parsing resume document...");

  const formData = new FormData();
  formData.append("file", file);

  try {
    const res = await fetch("/api/resume/upload", {
      method: "POST",
      body: formData
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Failed to parse resume document.");
    }

    const profile = await res.json();
    AppState.masterProfile = profile;
    populateProfileForm(profile);
    showLoading(false);
    showSuccess("Resume parsed successfully! Please verify your profile details.");
    setStep(2);
  } catch (e) {
    showLoading(false);
    showError(e.message);
  }
}

async function loadSampleResume() {
  showLoading(true, "Loading synthetic fictional sample resume...");
  try {
    const res = await fetch("/api/resume/sample", { method: "POST" });
    if (!res.ok) throw new Error("Failed to load sample resume");
    const profile = await res.json();
    AppState.masterProfile = profile;
    populateProfileForm(profile);
    showLoading(false);
    showSuccess(`Loaded verified profile for ${profile.fullName || 'Candidate'}.`);
    setStep(2);
  } catch (e) {
    showLoading(false);
    showError(e.message);
  }
}

/* ==========================================================================
   STEP 2: REVIEW & VERIFY PROFILE
   ========================================================================== */

function populateProfileForm(p) {
  document.getElementById("profFullName").value = p.fullName || "";
  const headlineEl = document.getElementById("profHeadline");
  if (headlineEl) headlineEl.value = p.headline || "";
  document.getElementById("profEmail").value = p.email || "";
  document.getElementById("profPhone").value = p.phone || "";
  document.getElementById("profLocation").value = p.location || "";
  document.getElementById("profLinkedin").value = p.linkedinUrl || "";
  document.getElementById("profGithub").value = p.githubUrl || "";
  document.getElementById("profPortfolio").value = p.portfolioUrl || "";
  document.getElementById("profSummary").value = p.summary || "";

  // Render skills tags
  renderSkillTags(p.skills || []);

  // Render Experience list
  renderExperienceList(p.workExperiences || []);

  // Render Education list
  renderEducationList(p.education || []);

  // Render Projects list
  renderProjectsList(p.projects || []);

  // Render Certifications list
  renderCertificationsList(p.certifications || []);

  // Render Achievements list
  renderAchievementsList(p.achievements || []);

  // Render Activities list
  renderActivitiesList(p.activities || []);

  // Render Soft Skills tags
  renderSoftSkillsTags(p.softSkills || []);
}

function renderSkillTags(skills) {
  const container = document.getElementById("skillsTagContainer");
  container.innerHTML = "";
  skills.forEach((skill, idx) => {
    const tag = document.createElement("span");
    tag.className = "tag-item";
    tag.innerHTML = `<span>${escapeHtml(skill)}</span><span class="tag-remove" onclick="removeSkillTag(${idx})">&times;</span>`;
    container.appendChild(tag);
  });
}

function addSkillTag(skill) {
  if (!skill) return;
  if (!AppState.masterProfile.skills) AppState.masterProfile.skills = [];
  if (!AppState.masterProfile.skills.includes(skill)) {
    AppState.masterProfile.skills.push(skill);
    renderSkillTags(AppState.masterProfile.skills);
  }
}

window.removeSkillTag = function(idx) {
  if (AppState.masterProfile && AppState.masterProfile.skills) {
    AppState.masterProfile.skills.splice(idx, 1);
    renderSkillTags(AppState.masterProfile.skills);
  }
};

function renderExperienceList(exps) {
  const container = document.getElementById("experienceListContainer");
  container.innerHTML = "";

  if (exps.length === 0) {
    container.innerHTML = `<p class="card-subtitle">No experience entries found. Add entries below.</p>`;
    return;
  }

  exps.forEach((exp, expIdx) => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.background = "#f8fafc";
    card.style.padding = "1rem";
    card.style.marginBottom = "1rem";

    let bulletsHtml = (exp.bullets || []).map((b, bIdx) => `
      <div style="display:flex; gap:0.5rem; margin-bottom:0.4rem; align-items:center;">
        <input type="text" class="form-input form-bullet-input" data-exp="${expIdx}" data-bullet="${bIdx}" value="${escapeHtml(b)}"/>
        <button class="btn btn-secondary btn-sm" onclick="removeExperienceBullet(${expIdx}, ${bIdx})">&times;</button>
      </div>
    `).join("");

    card.innerHTML = `
      <div class="form-row" style="margin-bottom:0.75rem;">
        <div>
          <label class="form-label">Job Title</label>
          <input type="text" class="form-input" id="expTitle_${expIdx}" value="${escapeHtml(exp.jobTitle || "")}"/>
        </div>
        <div>
          <label class="form-label">Company</label>
          <input type="text" class="form-input" id="expCompany_${expIdx}" value="${escapeHtml(exp.company || "")}"/>
        </div>
        <div>
          <label class="form-label">Dates</label>
          <input type="text" class="form-input" id="expDates_${expIdx}" value="${escapeHtml(exp.startDate || "")} - ${exp.isCurrent ? 'Present' : escapeHtml(exp.endDate || '')}"/>
        </div>
      </div>
      <div>
        <label class="form-label">Key Achievement Bullets</label>
        ${bulletsHtml}
        <button class="btn btn-secondary btn-sm" style="margin-top:0.4rem;" onclick="addExperienceBullet(${expIdx})">+ Add Bullet</button>
      </div>
    `;
    container.appendChild(card);
  });
}

window.addExperienceBullet = function(expIdx) {
  if (AppState.masterProfile.workExperiences[expIdx]) {
    if (!AppState.masterProfile.workExperiences[expIdx].bullets) {
      AppState.masterProfile.workExperiences[expIdx].bullets = [];
    }
    AppState.masterProfile.workExperiences[expIdx].bullets.push("New achievement or responsibility...");
    renderExperienceList(AppState.masterProfile.workExperiences);
  }
};

window.removeExperienceBullet = function(expIdx, bIdx) {
  if (AppState.masterProfile.workExperiences[expIdx] && AppState.masterProfile.workExperiences[expIdx].bullets) {
    AppState.masterProfile.workExperiences[expIdx].bullets.splice(bIdx, 1);
    renderExperienceList(AppState.masterProfile.workExperiences);
  }
};

function renderEducationList(edus) {
  const container = document.getElementById("educationListContainer");
  container.innerHTML = "";
  edus.forEach((edu, idx) => {
    const item = document.createElement("div");
    item.className = "card";
    item.style.padding = "0.75rem";
    item.style.marginBottom = "0.5rem";
    item.innerHTML = `
      <strong>${escapeHtml(edu.degree || "Degree")} ${edu.fieldOfStudy ? 'in ' + escapeHtml(edu.fieldOfStudy) : ''}</strong>
      <div>${escapeHtml(edu.institution || "")} (${escapeHtml(edu.graduationDate || "")}) ${edu.gpa ? '| GPA: ' + escapeHtml(edu.gpa) : ''}</div>
    `;
    container.appendChild(item);
  });
}

function renderProjectsList(projs) {
  const container = document.getElementById("projectsListContainer");
  container.innerHTML = "";
  projs.forEach((p, idx) => {
    const item = document.createElement("div");
    item.className = "card";
    item.style.padding = "0.75rem";
    item.style.marginBottom = "0.5rem";
    item.innerHTML = `
      <strong>${escapeHtml(p.name || "Project")}</strong> ${p.technologies ? `<span class="badge badge-verified">${escapeHtml(p.technologies.join(", "))}</span>` : ''}
      <p style="font-size:0.85rem; color:#475569; margin-top:0.25rem;">${escapeHtml(p.description || "")}</p>
    `;
    container.appendChild(item);
  });
}

function renderCertificationsList(certs) {
  const container = document.getElementById("certificationsListContainer");
  container.innerHTML = "";
  certs.forEach((c) => {
    const item = document.createElement("div");
    item.className = "tag-item";
    item.innerText = `${c.name} ${c.issuingOrganization ? ' - ' + c.issuingOrganization : ''}`;
    container.appendChild(item);
  });
}

function renderAchievementsList(achievements) {
  const container = document.getElementById("achievementsListContainer");
  if (!container) return;
  container.innerHTML = "";
  if (!achievements || achievements.length === 0) {
    container.innerHTML = `<p class="card-subtitle" style="font-size:0.85rem; margin-bottom:0.5rem;">No achievements added yet. Click "+ Add Achievement" to add hackathons, awards, or open source recognition.</p>`;
    return;
  }
  achievements.forEach((ach, idx) => {
    const row = document.createElement("div");
    row.style.display = "flex";
    row.style.gap = "0.5rem";
    row.style.marginBottom = "0.4rem";
    row.style.alignItems = "center";
    row.innerHTML = `
      <input type="text" class="form-input form-achievement-input" value="${escapeHtml(ach)}" placeholder="e.g. Winner at National Hackathon 2024 among 300+ teams" />
      <button class="btn btn-secondary btn-sm" onclick="removeAchievementEntry(${idx})">&times;</button>
    `;
    container.appendChild(row);
  });
}

window.addAchievementEntry = function() {
  if (!AppState.masterProfile) AppState.masterProfile = {};
  if (!AppState.masterProfile.achievements) AppState.masterProfile.achievements = [];
  AppState.masterProfile.achievements.push("");
  renderAchievementsList(AppState.masterProfile.achievements);
  const inputs = document.querySelectorAll(".form-achievement-input");
  if (inputs.length > 0) inputs[inputs.length - 1].focus();
};

window.removeAchievementEntry = function(idx) {
  if (AppState.masterProfile && AppState.masterProfile.achievements) {
    AppState.masterProfile.achievements.splice(idx, 1);
    renderAchievementsList(AppState.masterProfile.achievements);
  }
};

function renderActivitiesList(activities) {
  const container = document.getElementById("activitiesListContainer");
  if (!container) return;
  container.innerHTML = "";
  if (!activities || activities.length === 0) {
    container.innerHTML = `<p class="card-subtitle" style="font-size:0.85rem; margin-bottom:0.5rem;">No activities added yet. Click "+ Add Activity" to add coding profiles, club leadership, or mentorship roles.</p>`;
    return;
  }
  activities.forEach((act, idx) => {
    const row = document.createElement("div");
    row.style.display = "flex";
    row.style.gap = "0.5rem";
    row.style.marginBottom = "0.4rem";
    row.style.alignItems = "center";
    row.innerHTML = `
      <input type="text" class="form-input form-activity-input" value="${escapeHtml(act)}" placeholder="e.g. Competitive Programmer (LeetCode Knight - Max Rating 1950+, Solved 800+ Problems)" />
      <button class="btn btn-secondary btn-sm" onclick="removeActivityEntry(${idx})">&times;</button>
    `;
    container.appendChild(row);
  });
}

window.addActivityEntry = function() {
  if (!AppState.masterProfile) AppState.masterProfile = {};
  if (!AppState.masterProfile.activities) AppState.masterProfile.activities = [];
  AppState.masterProfile.activities.push("");
  renderActivitiesList(AppState.masterProfile.activities);
  const inputs = document.querySelectorAll(".form-activity-input");
  if (inputs.length > 0) inputs[inputs.length - 1].focus();
};

window.removeActivityEntry = function(idx) {
  if (AppState.masterProfile && AppState.masterProfile.activities) {
    AppState.masterProfile.activities.splice(idx, 1);
    renderActivitiesList(AppState.masterProfile.activities);
  }
};

function renderSoftSkillsTags(softSkills) {
  const container = document.getElementById("softSkillsTagContainer");
  if (!container) return;
  container.innerHTML = "";
  (softSkills || []).forEach((skill, idx) => {
    const tag = document.createElement("span");
    tag.className = "tag-item";
    tag.style.background = "#eff6ff";
    tag.style.borderColor = "#bfdbfe";
    tag.style.color = "#1e40af";
    tag.innerHTML = `<span>${escapeHtml(skill)}</span><span class="tag-remove" onclick="removeSoftSkillTag(${idx})">&times;</span>`;
    container.appendChild(tag);
  });
}

function addSoftSkillTag(skill) {
  if (!skill) return;
  if (!AppState.masterProfile) AppState.masterProfile = {};
  if (!AppState.masterProfile.softSkills) AppState.masterProfile.softSkills = [];
  if (!AppState.masterProfile.softSkills.includes(skill)) {
    AppState.masterProfile.softSkills.push(skill);
    renderSoftSkillsTags(AppState.masterProfile.softSkills);
  }
}

window.removeSoftSkillTag = function(idx) {
  if (AppState.masterProfile && AppState.masterProfile.softSkills) {
    AppState.masterProfile.softSkills.splice(idx, 1);
    renderSoftSkillsTags(AppState.masterProfile.softSkills);
  }
};

async function saveVerifiedProfile() {
  const p = AppState.masterProfile;
  p.fullName = document.getElementById("profFullName").value.trim();
  const headlineEl = document.getElementById("profHeadline");
  if (headlineEl) p.headline = headlineEl.value.trim();
  p.email = document.getElementById("profEmail").value.trim();
  p.phone = document.getElementById("profPhone").value.trim();
  p.location = document.getElementById("profLocation").value.trim();
  p.linkedinUrl = document.getElementById("profLinkedin").value.trim();
  p.githubUrl = document.getElementById("profGithub").value.trim();
  p.portfolioUrl = document.getElementById("profPortfolio").value.trim();
  p.summary = document.getElementById("profSummary").value.trim();
  p.verified = true;

  // Sync edited bullets
  document.querySelectorAll(".form-bullet-input").forEach(input => {
    const expIdx = parseInt(input.getAttribute("data-exp"));
    const bIdx = parseInt(input.getAttribute("data-bullet"));
    if (p.workExperiences[expIdx] && p.workExperiences[expIdx].bullets) {
      p.workExperiences[expIdx].bullets[bIdx] = input.value.trim();
    }
  });

  // Sync edited achievements
  p.achievements = [];
  document.querySelectorAll(".form-achievement-input").forEach(inp => {
    const v = inp.value.trim();
    if (v) p.achievements.push(v);
  });

  // Sync edited activities
  p.activities = [];
  document.querySelectorAll(".form-activity-input").forEach(inp => {
    const v = inp.value.trim();
    if (v) p.activities.push(v);
  });

  showLoading(true, "Saving and locking verified master profile...");
  try {
    const res = await fetch("/api/resume/profile", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(p)
    });
    if (!res.ok) throw new Error("Failed to save verified master profile.");
    const saved = await res.json();
    AppState.masterProfile = saved;
    showLoading(false);
    showSuccess("Master resume verified! All factual claims are locked as ground truth.");
    setStep(3);
  } catch (e) {
    showLoading(false);
    showError(e.message);
  }
}

/* ==========================================================================
   STEP 3: JOB DESCRIPTION & TAILORING
   ========================================================================== */

async function generateTailoredResume() {
  const jdText = document.getElementById("jdRawText").value.trim();
  if (!jdText) {
    showError("Please paste a complete job description to tailor against.");
    return;
  }

  const targetRole = document.getElementById("jdTargetRole").value.trim();
  const targetCompany = document.getElementById("jdTargetCompany").value.trim();
  const preferredLength = document.getElementById("jdPreferredLength").value;
  const useAi = document.getElementById("engineSelect").value === "OLLAMA_AI";

  const requestPayload = {
    jobDescriptionText: jdText,
    targetRole: targetRole,
    targetCompany: targetCompany,
    preferredLength: preferredLength,
    useAi: useAi
  };

  showLoading(true, useAi ? "Tailoring resume via Local Ollama AI & verifying factual integrity..."
                       : "Running deterministic ATS tailoring engine...");

  try {
    const res = await fetch("/api/resume/tailor", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestPayload)
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Tailoring failed.");
    }

    const tailored = await res.json();
    AppState.tailoredResume = tailored;
    showLoading(false);
    showSuccess("Tailored resume generated with 100% verified facts!");

    renderComparisonView(AppState.masterProfile, tailored);
    renderMatchReport(tailored.matchReport, tailored.validationReport);

    setStep(4);
  } catch (e) {
    showLoading(false);
    showError(e.message);
  }
}

/* ==========================================================================
   STEP 4: BEFORE/AFTER COMPARISON VIEW
   ========================================================================== */

function renderComparisonView(master, tailored) {
  // ================= Master Preview =================
  document.getElementById("origMasterName").innerText = master.fullName || "";
  const origHeadlineEl = document.getElementById("origMasterHeadline");
  if (origHeadlineEl) origHeadlineEl.innerText = master.headline || "";

  const origContactEl = document.getElementById("origMasterContact");
  if (origContactEl) {
    const contactParts = [];
    if (master.location) contactParts.push(master.location);
    if (master.phone) contactParts.push(master.phone);
    if (master.email) contactParts.push(master.email);
    if (master.linkedinUrl) contactParts.push("LinkedIn");
    if (master.githubUrl) contactParts.push("GitHub");
    if (master.portfolioUrl) contactParts.push("Portfolio");
    origContactEl.innerText = contactParts.join("  |  ");
  }

  document.getElementById("origMasterSummary").innerText = master.summary || "No summary provided";

  // Master experience bullets
  const origBulletsContainer = document.getElementById("origMasterBullets");
  origBulletsContainer.innerHTML = "";
  (master.workExperiences || []).forEach(exp => {
    const expDiv = document.createElement("div");
    expDiv.style.marginBottom = "0.75rem";
    const dateStr = (exp.startDate || "") + ((exp.startDate || exp.endDate) ? " – " : "") + (exp.isCurrent ? "Present" : (exp.endDate || ""));
    expDiv.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.15rem;">
        <strong style="color:#0f172a; font-size:0.92rem;">${escapeHtml(exp.jobTitle || "")} &mdash; ${escapeHtml(exp.company || "")}</strong>
        <span style="font-size:0.82rem; color:#64748b;">${escapeHtml(dateStr)}</span>
      </div>
      ${exp.location ? `<div style="font-size:0.82rem; font-style:italic; color:#475569; margin-bottom:0.25rem;">${escapeHtml(exp.location)}</div>` : ''}
    `;
    (exp.bullets || []).forEach(b => {
      const p = document.createElement("div");
      p.className = "resume-bullet";
      p.innerText = b;
      expDiv.appendChild(p);
    });
    origBulletsContainer.appendChild(expDiv);
  });

  // Master projects
  const origProjectsContainer = document.getElementById("origMasterProjects");
  if (origProjectsContainer) {
    origProjectsContainer.innerHTML = "";
    (master.projects || []).forEach(pr => {
      const div = document.createElement("div");
      div.style.marginBottom = "0.6rem";
      const techStr = (pr.technologies && pr.technologies.length > 0) ? pr.technologies.join(" · ") : "";
      div.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.15rem;">
          <strong style="color:#0f172a; font-size:0.92rem;">${escapeHtml(pr.name || "")}</strong>
          <span style="font-size:0.82rem; font-style:italic; color:#475569;">${escapeHtml(techStr)}</span>
        </div>
      `;
      (pr.highlights || []).forEach(h => {
        const bp = document.createElement("div");
        bp.className = "resume-bullet";
        bp.innerText = h;
        div.appendChild(bp);
      });
      if (!pr.highlights || pr.highlights.length === 0) {
        if (pr.description) {
          const bp = document.createElement("div");
          bp.className = "resume-bullet";
          bp.innerText = pr.description;
          div.appendChild(bp);
        }
      }
      origProjectsContainer.appendChild(div);
    });
  }

  // Master skills
  const origSkillsEl = document.getElementById("origMasterSkills");
  if (origSkillsEl) {
    if (master.categorizedSkills && Object.keys(master.categorizedSkills).length > 0) {
      origSkillsEl.innerHTML = Object.entries(master.categorizedSkills).map(([cat, list]) => `
        <div style="margin-bottom:0.25rem; font-size:0.85rem;"><strong style="color:#0f172a;">${escapeHtml(cat)}:</strong> ${escapeHtml(list.join(", "))}</div>
      `).join("");
    } else {
      origSkillsEl.innerText = (master.skills || []).join("  |  ");
    }
  }

  // Master education
  const origEduContainer = document.getElementById("origMasterEducation");
  if (origEduContainer) {
    origEduContainer.innerHTML = "";
    (master.education || []).forEach(edu => {
      const div = document.createElement("div");
      div.style.marginBottom = "0.5rem";
      const degField = (edu.degree || "") + (edu.fieldOfStudy ? " — " + edu.fieldOfStudy : "") + (edu.gpa ? " | GPA: " + edu.gpa : "");
      div.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.15rem;">
          <strong style="color:#0f172a; font-size:0.92rem;">${escapeHtml(edu.institution || "")}</strong>
          <span style="font-size:0.82rem; color:#64748b;">${escapeHtml(edu.graduationDate || "")}</span>
        </div>
        <div style="font-size:0.82rem; font-style:italic; color:#475569;">${escapeHtml(degField)}</div>
      `;
      origEduContainer.appendChild(div);
    });
  }

  // Master achievements
  const origAchievementsContainer = document.getElementById("origMasterAchievements");
  if (origAchievementsContainer) {
    origAchievementsContainer.innerHTML = "";
    (master.achievements || []).forEach(ach => {
      const p = document.createElement("div");
      p.className = "resume-bullet";
      p.innerText = ach;
      origAchievementsContainer.appendChild(p);
    });
  }

  // Master activities
  const origActivitiesContainer = document.getElementById("origMasterActivities");
  if (origActivitiesContainer) {
    origActivitiesContainer.innerHTML = "";
    (master.activities || []).forEach(act => {
      const p = document.createElement("div");
      p.className = "resume-bullet";
      p.innerText = act;
      origActivitiesContainer.appendChild(p);
    });
  }

  // Master soft skills
  const origSoftSkillsEl = document.getElementById("origMasterSoftSkills");
  if (origSoftSkillsEl) {
    origSoftSkillsEl.innerText = (master.softSkills || []).join("  ·  ");
  }

  // ================= Tailored Preview =================
  document.getElementById("tailoredEngineBadge").innerText = "Engine: " + (tailored.generatorEngine || "RULE_BASED");
  document.getElementById("tailoredName").innerText = tailored.fullName || "";
  const tailoredHeadlineEl = document.getElementById("tailoredTargetHeader");
  if (tailoredHeadlineEl) {
    tailoredHeadlineEl.innerText = tailored.headline || (tailored.targetRole ? tailored.targetRole + (tailored.targetCompany ? ' at ' + tailored.targetCompany : '') : '');
  }

  const tailoredContactEl = document.getElementById("tailoredContact");
  if (tailoredContactEl) {
    const contactParts = [];
    if (tailored.location) contactParts.push(tailored.location);
    if (tailored.phone) contactParts.push(tailored.phone);
    if (tailored.email) contactParts.push(tailored.email);
    if (tailored.linkedinUrl) contactParts.push("LinkedIn");
    if (tailored.githubUrl) contactParts.push("GitHub");
    if (tailored.portfolioUrl) contactParts.push("Portfolio");
    tailoredContactEl.innerText = contactParts.join("  |  ");
  }

  // Editable Tailored Summary
  document.getElementById("tailoredSummaryInput").value = tailored.tailoredSummary || "";

  // Tailored Bullets (Editable)
  const tailoredBulletsContainer = document.getElementById("tailoredBullets");
  tailoredBulletsContainer.innerHTML = "";
  (tailored.tailoredExperiences || []).forEach((exp, expIdx) => {
    const expDiv = document.createElement("div");
    expDiv.style.marginBottom = "1rem";
    const dateStr = (exp.startDate || "") + ((exp.startDate || exp.endDate) ? " – " : "") + (exp.isCurrent ? "Present" : (exp.endDate || ""));
    expDiv.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.25rem;">
        <strong style="color:#0f172a; font-size:0.92rem;">${escapeHtml(exp.jobTitle || "")} &mdash; ${escapeHtml(exp.company || "")}</strong>
        <span style="font-size:0.82rem; color:#64748b;">${escapeHtml(dateStr)}</span>
      </div>
      ${exp.location ? `<div style="font-size:0.82rem; font-style:italic; color:#475569; margin-bottom:0.35rem;">${escapeHtml(exp.location)}</div>` : ''}
    `;

    (exp.bullets || []).forEach((b, bIdx) => {
      const row = document.createElement("div");
      row.style.display = "flex";
      row.style.gap = "0.5rem";
      row.style.marginBottom = "0.4rem";
      row.innerHTML = `
        <span style="color:#059669; font-weight:bold;">✔</span>
        <input type="text" class="form-input tailored-bullet-input" data-exp="${expIdx}" data-bullet="${bIdx}" value="${escapeHtml(b)}"/>
      `;
      expDiv.appendChild(row);
    });
    tailoredBulletsContainer.appendChild(expDiv);
  });

  // Tailored Projects
  const tailoredProjectsContainer = document.getElementById("tailoredProjectsContainer");
  if (tailoredProjectsContainer) {
    tailoredProjectsContainer.innerHTML = "";
    (tailored.tailoredProjects || []).forEach(pr => {
      const div = document.createElement("div");
      div.style.marginBottom = "0.6rem";
      const techStr = (pr.technologies && pr.technologies.length > 0) ? pr.technologies.join(" · ") : "";
      div.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.25rem;">
          <strong style="color:#0f172a; font-size:0.92rem;">${escapeHtml(pr.name || "")}</strong>
          <span style="font-size:0.82rem; font-style:italic; color:#475569;">${escapeHtml(techStr)}</span>
        </div>
      `;
      (pr.highlights || []).forEach(h => {
        const bp = document.createElement("div");
        bp.className = "resume-bullet";
        bp.innerText = h;
        div.appendChild(bp);
      });
      if (!pr.highlights || pr.highlights.length === 0) {
        if (pr.description) {
          const bp = document.createElement("div");
          bp.className = "resume-bullet";
          bp.innerText = pr.description;
          div.appendChild(bp);
        }
      }
      tailoredProjectsContainer.appendChild(div);
    });
  }

  // Tailored Skills (Categorized with bold category prefix)
  const tailoredSkillsContainer = document.getElementById("tailoredSkillsContainer");
  if (tailoredSkillsContainer) {
    tailoredSkillsContainer.innerHTML = "";
    if (tailored.tailoredCategorizedSkills && Object.keys(tailored.tailoredCategorizedSkills).length > 0) {
      Object.entries(tailored.tailoredCategorizedSkills).forEach(([cat, sList]) => {
        const catRow = document.createElement("div");
        catRow.style.fontSize = "0.85rem";
        catRow.style.marginBottom = "0.3rem";
        catRow.innerHTML = `<strong style="color:#0f172a;">${escapeHtml(cat)}:</strong> ${escapeHtml(sList.join(", "))}`;
        tailoredSkillsContainer.appendChild(catRow);
      });
    } else {
      const fallbackDiv = document.createElement("div");
      fallbackDiv.style.fontSize = "0.85rem";
      fallbackDiv.innerText = (tailored.tailoredSkills || []).join("  |  ");
      tailoredSkillsContainer.appendChild(fallbackDiv);
    }
  }

  // Tailored Education
  const tailoredEduContainer = document.getElementById("tailoredEducationContainer");
  if (tailoredEduContainer) {
    tailoredEduContainer.innerHTML = "";
    (tailored.education || []).forEach(edu => {
      const div = document.createElement("div");
      div.style.marginBottom = "0.5rem";
      const degField = (edu.degree || "") + (edu.fieldOfStudy ? " — " + edu.fieldOfStudy : "") + (edu.gpa ? " | GPA: " + edu.gpa : "");
      div.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.15rem;">
          <strong style="color:#0f172a; font-size:0.92rem;">${escapeHtml(edu.institution || "")}</strong>
          <span style="font-size:0.82rem; color:#64748b;">${escapeHtml(edu.graduationDate || "")}</span>
        </div>
        <div style="font-size:0.82rem; font-style:italic; color:#475569;">${escapeHtml(degField)}</div>
      `;
      tailoredEduContainer.appendChild(div);
    });
  }

  // Tailored Achievements
  const tailoredAchievementsContainer = document.getElementById("tailoredAchievementsContainer");
  if (tailoredAchievementsContainer) {
    tailoredAchievementsContainer.innerHTML = "";
    (tailored.achievements || []).forEach(ach => {
      const p = document.createElement("div");
      p.className = "resume-bullet";
      p.innerText = ach;
      tailoredAchievementsContainer.appendChild(p);
    });
  }

  // Tailored Activities
  const tailoredActivitiesContainer = document.getElementById("tailoredActivitiesContainer");
  if (tailoredActivitiesContainer) {
    tailoredActivitiesContainer.innerHTML = "";
    (tailored.activities || []).forEach(act => {
      const p = document.createElement("div");
      p.className = "resume-bullet";
      p.innerText = act;
      tailoredActivitiesContainer.appendChild(p);
    });
  }

  // Tailored Soft Skills
  const tailoredSoftSkillsContainer = document.getElementById("tailoredSoftSkillsContainer");
  if (tailoredSoftSkillsContainer) {
    tailoredSoftSkillsContainer.innerText = (tailored.softSkills || []).join("  ·  ");
  }
}

window.saveTailoredEdits = async function() {
  if (!AppState.tailoredResume) return;

  AppState.tailoredResume.tailoredSummary = document.getElementById("tailoredSummaryInput").value.trim();

  document.querySelectorAll(".tailored-bullet-input").forEach(input => {
    const expIdx = parseInt(input.getAttribute("data-exp"));
    const bIdx = parseInt(input.getAttribute("data-bullet"));
    if (AppState.tailoredResume.tailoredExperiences[expIdx] && AppState.tailoredResume.tailoredExperiences[expIdx].bullets) {
      AppState.tailoredResume.tailoredExperiences[expIdx].bullets[bIdx] = input.value.trim();
    }
  });

  try {
    const res = await fetch("/api/resume/tailored", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(AppState.tailoredResume)
    });
    if (res.ok) {
      showSuccess("Tailored resume edits saved!");
      setStep(5);
    }
  } catch (e) {
    showError("Failed to save tailored changes.");
  }
};

/* ==========================================================================
   STEP 5: ATS MATCH REPORT & DOWNLOADS
   ========================================================================== */

function renderMatchReport(report, validation) {
  if (!report) return;

  // Score display
  const scoreNum = Math.round(report.overallScore);
  document.getElementById("overallScoreNumber").innerText = scoreNum;

  // Category breakdown bars
  const catScores = report.categoryScores || {};
  renderCategoryBar("catBarReqSkills", "Required Skills", catScores["REQUIRED_SKILL"] || 0);
  renderCategoryBar("catBarPrefSkills", "Preferred Skills", catScores["PREFERRED_SKILL"] || 0);
  renderCategoryBar("catBarResp", "Responsibilities", catScores["RESPONSIBILITY"] || 0);
  renderCategoryBar("catBarTools", "Tools & Platforms", catScores["TOOL_PLATFORM"] || 0);
  renderCategoryBar("catBarEdu", "Education & Experience", catScores["EDUCATION_EXPERIENCE"] || 0);

  // Score Explanation & Disclaimer
  document.getElementById("scoreExplanationText").innerText = report.scoreExplanation || "";
  document.getElementById("scoreDisclaimerText").innerText = report.disclaimer || "";

  // Validation Checks
  renderValidationList(validation);

  // Requirements Table
  renderRequirementsTable(report.requirementMatches || []);
}

function renderCategoryBar(elementId, label, score) {
  const el = document.getElementById(elementId);
  if (!el) return;
  const rounded = Math.round(score);
  let colorClass = "high";
  if (rounded < 50) colorClass = "low";
  else if (rounded < 75) colorClass = "mid";

  el.innerHTML = `
    <div class="category-bar-label">
      <span>${label}</span>
      <span><strong>${rounded}%</strong></span>
    </div>
    <div class="progress-track">
      <div class="progress-fill ${colorClass}" style="width: ${rounded}%;"></div>
    </div>
  `;
}

function renderValidationList(validation) {
  const container = document.getElementById("validationChecksContainer");
  container.innerHTML = "";

  if (!validation) return;

  (validation.checks || []).forEach(chk => {
    const div = document.createElement("div");
    let alertClass = chk.severity === "ERROR" ? "alert-error" : (chk.severity === "WARNING" ? "alert-warning" : "alert-success");
    div.className = `alert ${alertClass}`;
    div.style.padding = "0.6rem 0.85rem";
    div.style.marginBottom = "0.5rem";
    div.innerHTML = `
      <strong>${escapeHtml(chk.ruleName)}:</strong> ${escapeHtml(chk.message)}
    `;
    container.appendChild(div);
  });
}

function renderRequirementsTable(matches) {
  const tbody = document.getElementById("matchTableBody");
  tbody.innerHTML = "";

  if (matches.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding:1.5rem; color:#64748b;">No requirements extracted from JD.</td></tr>`;
    return;
  }

  matches.forEach(m => {
    const tr = document.createElement("tr");

    let statusBadge = "";
    if (m.status === "VERIFIED") {
      statusBadge = `<span class="badge badge-verified">VERIFIED</span>`;
    } else if (m.status === "PARTIALLY_SUPPORTED") {
      statusBadge = `<span class="badge badge-partial">PARTIAL</span>`;
    } else {
      statusBadge = `<span class="badge badge-missing">MISSING</span>`;
    }

    tr.innerHTML = `
      <td><strong>${escapeHtml(m.requirement)}</strong><div style="font-size:0.75rem; color:#64748b;">${escapeHtml(m.importance || "")}</div></td>
      <td>${statusBadge}</td>
      <td style="font-size:0.82rem; color:#334155;">${escapeHtml(m.evidence || "No evidence")}</td>
      <td style="font-size:0.82rem; color:#475569;">${escapeHtml(m.suggestedAction || "")}</td>
    `;
    tbody.appendChild(tr);
  });
}

window.filterMatchTable = function(filter) {
  const rows = document.querySelectorAll("#matchTableBody tr");
  rows.forEach(r => {
    if (filter === 'ALL') {
      r.style.display = "";
    } else if (filter === 'VERIFIED') {
      r.style.display = r.innerHTML.includes("badge-verified") ? "" : "none";
    } else if (filter === 'PARTIAL') {
      r.style.display = r.innerHTML.includes("badge-partial") ? "" : "none";
    } else if (filter === 'MISSING') {
      r.style.display = r.innerHTML.includes("badge-missing") ? "" : "none";
    }
  });
};

/* ==========================================================================
   DOWNLOADS & RESET
   ========================================================================== */

window.downloadPdf = function() {
  if (!AppState.tailoredResume) return;
  const versionId = AppState.tailoredResume.versionId;
  window.location.href = `/api/resume/download/pdf?versionId=${versionId}`;
};

window.downloadDocx = function() {
  if (!AppState.tailoredResume) return;
  const versionId = AppState.tailoredResume.versionId;
  window.location.href = `/api/resume/download/docx?versionId=${versionId}`;
};

async function resetAllData() {
  if (!confirm("Are you sure you want to clear your master resume and start over? This will remove all local session records.")) {
    return;
  }
  try {
    await fetch("/api/resume/profile", { method: "DELETE" });
    AppState.masterProfile = null;
    AppState.tailoredResume = null;
    document.getElementById("resumeFileInput").value = "";
    document.getElementById("jdRawText").value = "";
    setStep(1);
    showSuccess("All resume records cleared.");
  } catch (e) {
    showError("Failed to clear profile.");
  }
}

/* ==========================================================================
   UI UTILITIES (ALERTS, SPINNER, ESCAPING)
   ========================================================================== */

function showLoading(show, message = "Processing...") {
  const overlay = document.getElementById("loadingOverlay");
  const msgEl = document.getElementById("loadingMessage");
  if (overlay) {
    overlay.style.display = show ? "flex" : "none";
    if (msgEl) msgEl.innerText = message;
  }
}

function showSuccess(msg) {
  const container = document.getElementById("alertContainer");
  if (!container) return;
  container.innerHTML = `<div class="alert alert-success"><span>✔</span> <span>${escapeHtml(msg)}</span></div>`;
  setTimeout(() => container.innerHTML = "", 5000);
}

function showError(msg) {
  const container = document.getElementById("alertContainer");
  if (!container) return;
  container.innerHTML = `<div class="alert alert-error"><span>✖</span> <span>${escapeHtml(msg)}</span></div>`;
}

function escapeHtml(str) {
  if (str == null) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
