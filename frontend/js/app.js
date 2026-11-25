/**
 * InternShield AI - Main UI Controller
 * Connected to Spring Boot Backend API (configurable via window.INTERNSHIELD_API_BASE_URL)
 *
 * Handles live offer verification, Octen research orchestration,
 * Gemini AI credibility analysis, circular risk gauge animations,
 * company verification, and persistent session history from the database.
 */

/**
 * Checks whether the current page is running in a local development environment.
 */
function isLocalDevelopment() {
  const host = window.location.hostname;
  return Boolean(
    host === "localhost" ||
    host === "127.0.0.1" ||
    host === "[::1]" ||
    host === "" ||
    window.location.protocol === "file:"
  );
}

/**
 * Resolves the backend API base URL.
 * Priority:
 * 1. window.INTERNSHIELD_API_BASE_URL (configured via js/config.js, Vercel build, or window).
 * 2. <meta name="internshield-api-base-url" content="...">
 * 3. Localhost / local development -> "http://localhost:8080".
 * 4. Production -> returns "" (relative) and NEVER falls back to localhost.
 */
function resolveApiBaseUrl() {
  const configured = typeof window.INTERNSHIELD_API_BASE_URL === "string"
    ? window.INTERNSHIELD_API_BASE_URL.trim()
    : "";

  if (configured) {
    return configured.replace(/\/+$/, "");
  }

  const metaTag = document.querySelector('meta[name="internshield-api-base-url"]');
  if (metaTag && metaTag.content && metaTag.content.trim() !== "") {
    return metaTag.content.trim().replace(/\/+$/, "");
  }

  if (isLocalDevelopment()) {
    return "http://localhost:8080";
  }

  // Production: Do not fall back to localhost!
  return "";
}

// Expose on window for diagnostics and tests
window.resolveInternShieldApiUrl = resolveApiBaseUrl;
window.isLocalDevelopment = isLocalDevelopment;

document.addEventListener("DOMContentLoaded", () => {
  // Backend API Base URL
  const API_BASE_URL = resolveApiBaseUrl();
  const isLocal = isLocalDevelopment();

  // Navigation elements
  const mobileToggle = document.getElementById("mobileToggle");
  const navMenu = document.getElementById("navMenu");

  // Scanner UI Components
  const companyInput = document.getElementById("companyInput");
  const companyInputError = document.getElementById("companyInputError");
  const urlInput = document.getElementById("urlInput");
  const offerInput = document.getElementById("offerInput");
  const scannerApiError = document.getElementById("scannerApiError");
  const analyzeBtn = document.getElementById("analyzeBtn");
  const clearBtn = document.getElementById("clearBtn");
  const scannerEmptyState = document.getElementById("scannerEmptyState");
  const scanningIndicator = document.getElementById("scanningIndicator");
  const analysisResult = document.getElementById("analysisResult");
  const scenarioChips = document.querySelectorAll(".chip-btn");

  // Recent checks components
  const recentEmptyState = document.getElementById("recentEmptyState");
  const recentChecksList = document.getElementById("recentChecksList");

  // Session history array
  let sessionChecks = [];

  // Analysis scroll protection state
  let currentAnalysisId = 0;

  /**
   * Unified smooth scroll function that offsets for the sticky header
   */
  function smoothScrollToElement(targetElement) {
    if (!targetElement) return;

    const header = document.querySelector(".site-header");
    const headerHeight = header ? header.offsetHeight : 72;
    const elementPosition = targetElement.getBoundingClientRect().top + window.pageYOffset;
    const offsetPosition = Math.max(0, elementPosition - headerHeight - 20);

    window.scrollTo({
      top: offsetPosition,
      behavior: "smooth"
    });
  }

  function cancelPendingAnalysisScroll() {
    currentAnalysisId++;
  }

  // Mobile Navigation Drawer Toggle
  if (mobileToggle && navMenu) {
    mobileToggle.addEventListener("click", () => {
      navMenu.classList.toggle("open");
      const isExpanded = navMenu.classList.contains("open");
      mobileToggle.setAttribute("aria-expanded", isExpanded ? "true" : "false");
    });
  }

  // Unified Click Handler for Internal Anchor Links
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener("click", (e) => {
      const href = anchor.getAttribute("href");
      if (!href || href === "#") return;

      const targetElement = href === "#top"
        ? document.body
        : document.querySelector(href);

      if (targetElement) {
        e.preventDefault();
        cancelPendingAnalysisScroll();

        if (navMenu && navMenu.classList.contains("open")) {
          navMenu.classList.remove("open");
          if (mobileToggle) {
            mobileToggle.setAttribute("aria-expanded", "false");
          }
        }

        smoothScrollToElement(targetElement);

        if (history.pushState) {
          history.pushState(null, "", href);
        }
      }
    });
  });

  // Real-time clearing of validation error on company input
  if (companyInput) {
    companyInput.addEventListener("input", () => {
      companyInput.classList.remove("error");
      if (companyInputError) companyInputError.classList.remove("visible");
      hideApiError();
    });
  }

  // Populate scenario and run live analysis when chip is clicked
  scenarioChips.forEach(chip => {
    chip.addEventListener("click", () => {
      const scenarioKey = chip.getAttribute("data-scenario");
      scenarioChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");

      if (typeof MOCK_SCENARIOS !== "undefined" && MOCK_SCENARIOS[scenarioKey]) {
        const scenario = MOCK_SCENARIOS[scenarioKey];
        if (companyInput) companyInput.value = scenario.company || "";
        if (urlInput) urlInput.value = scenario.url || "";
        if (offerInput) offerInput.value = scenario.sampleText || "";

        if (companyInput) companyInput.classList.remove("error");
        if (companyInputError) companyInputError.classList.remove("visible");
        hideApiError();

        runLiveAnalysis(scenario.company || "Sample Company", scenario.url || "", scenario.sampleText || "", scenario.label);
      }
    });
  });

  // Clear button resets form inputs, hides results, displays empty state
  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      cancelPendingAnalysisScroll();
      if (companyInput) {
        companyInput.value = "";
        companyInput.classList.remove("error");
      }
      if (companyInputError) companyInputError.classList.remove("visible");
      if (urlInput) urlInput.value = "";
      if (offerInput) offerInput.value = "";
      hideApiError();

      scenarioChips.forEach(c => c.classList.remove("active"));
      if (analysisResult) analysisResult.classList.remove("visible");
      if (scanningIndicator) scanningIndicator.classList.remove("active");
      if (scannerEmptyState) scannerEmptyState.classList.remove("hidden");
      resetAnalyzeBtn();
      if (companyInput) companyInput.focus();
    });
  }

  // Analyze button click
  if (analyzeBtn) {
    analyzeBtn.addEventListener("click", () => {
      hideApiError();

      let company = companyInput ? companyInput.value.trim() : "";
      const url = urlInput ? urlInput.value.trim() : "";
      const text = offerInput ? offerInput.value.trim() : "";

      // If user pasted text without filling company, attempt heuristic extraction
      if (!company && text) {
        company = extractCompanyFromText(text) || "";
        if (company && companyInput) {
          companyInput.value = company;
        }
      }

      if (!company) {
        if (companyInput) {
          companyInput.classList.add("error");
          companyInput.focus();
        }
        if (companyInputError) companyInputError.classList.add("visible");
        showApiError("Please enter the <strong>Company / Organization name</strong> so our search orchestrator and AI engine can verify it.");
        return;
      }

      if (!text && !url) {
        showApiError("Please provide an <strong>offer message/description</strong> or enter a <strong>job listing URL</strong> to evaluate.");
        if (offerInput) offerInput.focus();
        return;
      }

      runLiveAnalysis(company, url, text, `${company} Scan`);
    });
  }

  function showApiError(messageHtml) {
    if (!scannerApiError) return;
    scannerApiError.innerHTML = messageHtml;
    scannerApiError.style.display = "block";
    scannerApiError.classList.add("visible");
    smoothScrollToElement(scannerApiError);
  }

  function hideApiError() {
    if (!scannerApiError) return;
    scannerApiError.style.display = "none";
    scannerApiError.classList.remove("visible");
    scannerApiError.innerHTML = "";
  }

  function resetAnalyzeBtn() {
    if (!analyzeBtn) return;
    analyzeBtn.disabled = false;
    analyzeBtn.innerHTML = `
      Analyze Offer
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
        stroke-linecap="round" stroke-linejoin="round">
        <line x1="7" y1="17" x2="17" y2="7" />
        <polyline points="7 7 17 7 17 17" />
      </svg>
    `;
  }

  /**
   * Helper to get authenticated headers from Supabase session
   */
  async function getAuthHeaders() {
    const headers = {
      "Content-Type": "application/json"
    };
    try {
      let token = null;
      if (typeof window.getValidAccessToken === "function") {
        token = await window.getValidAccessToken();
      } else if (window.supabaseClient && window.supabaseClient.auth) {
        const { data: { session } } = await window.supabaseClient.auth.getSession();
        token = session?.access_token || null;
      }
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }
    } catch (err) {
      console.warn("Failed to retrieve Supabase auth session:", err);
    }
    return headers;
  }

  /**
   * Connect to real Spring Boot backend: POST /api/analysis
   */
  async function runLiveAnalysis(companyName, internshipUrl, pastedDescription, sourceLabel) {
    if (!scanningIndicator || !analysisResult) return;

    const thisAnalysisId = ++currentAnalysisId;

    hideApiError();

    // Check Supabase authentication before starting scan
    const authHeaders = await getAuthHeaders();
    if (!authHeaders["Authorization"]) {
      resetAnalyzeBtn();
      if (scannerEmptyState) scannerEmptyState.classList.remove("hidden");
      showApiError(`
        <strong>Authentication Required:</strong> Please <a href="login.html" style="color:var(--primary);text-decoration:underline;font-weight:600;">Sign In</a> or <a href="register.html" style="color:var(--primary);text-decoration:underline;font-weight:600;">Create an Account</a> to run an internship verification scan and save your history.
      `);
      return;
    }

    // In production without configured backend URL, inform the user clearly
    if (!API_BASE_URL && !isLocal) {
      resetAnalyzeBtn();
      if (scannerEmptyState) scannerEmptyState.classList.remove("hidden");
      showApiError(`
        <strong>Backend URL Not Configured:</strong><br>
        The production backend URL has not been configured.<br>
        <span style="font-size:0.85rem; opacity:0.9;">Please set <code>INTERNSHIELD_API_BASE_URL</code> in your Vercel Project Settings (Environment Variables) to your deployed Render URL (e.g., <code>https://&lt;service&gt;.onrender.com</code>).</span>
      `);
      return;
    }

    if (scannerEmptyState) scannerEmptyState.classList.add("hidden");
    analysisResult.classList.remove("visible");
    scanningIndicator.classList.add("active");

    if (analyzeBtn) {
      analyzeBtn.disabled = true;
      analyzeBtn.innerHTML = `Scanning... <span class="scanner-spinner" style="width:14px;height:14px;border-width:2px;display:inline-block;margin-left:6px;vertical-align:middle;"></span>`;
    }

    try {
      let response = await fetch(`${API_BASE_URL}/api/analysis`, {
        method: "POST",
        headers: authHeaders,
        body: JSON.stringify({
          companyName: companyName,
          internshipUrl: internshipUrl ? internshipUrl.trim() : null,
          pastedDescription: pastedDescription ? pastedDescription.trim() : null
        })
      });

      // Proactively handle 401 by attempting session refresh and retry once
      if (response.status === 401 && typeof window.refreshAuthSession === "function") {
        console.warn("Received 401 Unauthorized; attempting Supabase session refresh...");
        const refreshedToken = await window.refreshAuthSession();
        if (refreshedToken) {
          authHeaders["Authorization"] = `Bearer ${refreshedToken}`;
          response = await fetch(`${API_BASE_URL}/api/analysis`, {
            method: "POST",
            headers: authHeaders,
            body: JSON.stringify({
              companyName: companyName,
              internshipUrl: internshipUrl ? internshipUrl.trim() : null,
              pastedDescription: pastedDescription ? pastedDescription.trim() : null
            })
          });
        }
      }

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Your login session has expired or is invalid. Please <a href='login.html' style='color:var(--primary);text-decoration:underline;font-weight:600;'>Sign In</a> again.");
        }
        let errorDetail = `Backend HTTP ${response.status} (${response.statusText})`;
        try {
          const errJson = await response.json();
          if (errJson && errJson.message) {
            errorDetail = errJson.message;
          } else if (errJson && errJson.error) {
            errorDetail = errJson.error;
          }
        } catch (_) {}
        throw new Error(errorDetail);
      }

      const backendReport = await response.json();

      // Transform backend DTO to UI model
      const uiData = transformBackendReportToUi(backendReport);

      scanningIndicator.classList.remove("active");
      resetAnalyzeBtn();

      renderAnalysisResult(uiData);
      addSessionHistory(uiData, sourceLabel || companyName);
      analysisResult.classList.add("visible");

      if (thisAnalysisId === currentAnalysisId) {
        smoothScrollToElement(analysisResult);
      }

    } catch (err) {
      scanningIndicator.classList.remove("active");
      resetAnalyzeBtn();
      if (scannerEmptyState) scannerEmptyState.classList.remove("hidden");

      const connectionHint = API_BASE_URL
        ? `Ensure the Spring Boot backend server is reachable at <code>${API_BASE_URL}</code> and CORS on Render allows requests from this domain.`
        : (isLocal
            ? `Ensure the Spring Boot backend server is running on <code>http://localhost:8080</code>.`
            : `Ensure <code>INTERNSHIELD_API_BASE_URL</code> is set in Vercel environment variables.`);

      showApiError(`
        <strong>Live Analysis Error:</strong> ${err.message}<br>
        <span style="font-size:0.85rem; opacity:0.9;">${connectionHint}</span>
      `);
    }
  }

  /**
   * Load real past checks from Spring Boot database for the authenticated user: GET /api/analysis
   */
  async function loadRecentChecksFromBackend() {
    try {
      if (!API_BASE_URL && !isLocal) {
        // Backend not configured on production deployment; history unavailable
        return;
      }

      const authHeaders = await getAuthHeaders();
      if (!authHeaders["Authorization"]) {
        sessionChecks = [];
        renderSessionHistory();
        return;
      }

      let res = await fetch(`${API_BASE_URL}/api/analysis`, {
        headers: authHeaders
      });

      // Proactively handle 401 in history load
      if (res.status === 401 && typeof window.refreshAuthSession === "function") {
        const refreshedToken = await window.refreshAuthSession();
        if (refreshedToken) {
          authHeaders["Authorization"] = `Bearer ${refreshedToken}`;
          res = await fetch(`${API_BASE_URL}/api/analysis`, {
            headers: authHeaders
          });
        }
      }

      if (res.ok) {
        const reports = await res.json();
        if (Array.isArray(reports) && reports.length > 0) {
          sessionChecks = reports.slice(0, 8).map(report => {
            const uiData = transformBackendReportToUi(report);
            return {
              id: report.id,
              label: report.companyName || "Analyzed Offer",
              timestamp: report.createdAt
                ? new Date(report.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                : "Recent",
              score: report.riskScore != null ? report.riskScore : 0,
              riskLevel: uiData.riskLevel,
              classification: uiData.classification,
              companyName: report.companyName,
              internshipUrl: report.internshipUrl,
              data: uiData
            };
          });
        } else {
          sessionChecks = [];
        }
        renderSessionHistory();
      } else {
        sessionChecks = [];
        renderSessionHistory();
      }
    } catch (err) {
      console.warn("Could not load recent checks from backend:", err.message);
    }
  }

  /**
   * Transform backend AnalysisReportResponse to UI presentation model
   */
  function transformBackendReportToUi(report) {
    const score = report.riskScore != null ? report.riskScore : 0;

    // Map riskLevel to badge colors ("danger", "warning", "safe")
    let riskLevel = "warning";
    const rawLevel = (report.riskLevel || "").toUpperCase();
    if (rawLevel === "HIGH" || score >= 70) {
      riskLevel = "danger";
    } else if (rawLevel === "LOW" || score < 30) {
      riskLevel = "safe";
    }

    const classification = rawLevel === "HIGH" ? "High Risk" : rawLevel === "LOW" ? "Low Risk" : "Medium Risk";
    const riskBadge = report.recommendation
      ? `${report.recommendation.replace(/_/g, " ")} — ${classification}`
      : `${classification} Detected`;

    const summary = report.aiSummary || report.recommendationExplanation || report.findings || "Verification analysis completed by InternShield AI.";

    // Warning signs derived from triggeredRules + riskSignals
    const warningSigns = [];

    if (Array.isArray(report.triggeredRules) && report.triggeredRules.length > 0) {
      report.triggeredRules.forEach(rule => {
        const isCritical = (rule.riskPoints || 0) >= 30;
        const type = isCritical ? "danger" : "warning";
        let icon = "alert-triangle";
        if (rule.category === "PAYMENT_FRAUD") icon = "dollar";
        else if (rule.category === "UNOFFICIAL_COMMUNICATION") icon = "chat";
        else if (rule.category === "SUSPICIOUS_DOMAIN" || rule.category === "OFFICIAL_PORTAL_BYPASS") icon = "globe";

        warningSigns.push({
          title: rule.ruleName ? rule.ruleName.replace(/_/g, " ") : "Triggered Risk Rule",
          severity: isCritical ? "Critical" : "Moderate",
          type: type,
          icon: icon,
          observation: rule.description || "Suspicious pattern identified during rule-based risk evaluation.",
          whyItMatters: getRuleWhyItMatters(rule.category, rule.ruleName)
        });
      });
    }

    if (Array.isArray(report.riskSignals) && report.riskSignals.length > 0) {
      report.riskSignals.forEach(signal => {
        warningSigns.push({
          title: "AI Detected Risk Pattern",
          severity: "Moderate",
          type: "warning",
          icon: "chat",
          observation: signal,
          whyItMatters: "Language patterns and recruitment structures observed match documented internship risk indicators."
        });
      });
    }

    // Fallback if no warning signs were triggered
    if (warningSigns.length === 0) {
      if (Array.isArray(report.positiveSignals) && report.positiveSignals.length > 0) {
        report.positiveSignals.forEach(pos => {
          warningSigns.push({
            title: "Legitimate Verification Indicator",
            severity: "Safe Indicator",
            type: "safe",
            icon: "shield-check",
            observation: pos,
            whyItMatters: "Verified presence and absence of upfront financial requirements indicate an authentic offer."
          });
        });
      } else {
        warningSigns.push({
          title: "Clean Risk Profile",
          severity: "Safe Indicator",
          type: "safe",
          icon: "shield-check",
          observation: "No upfront fees, cryptocurrency demands, or informal chat interview patterns were detected.",
          whyItMatters: "Authentic corporate offers follow structured evaluation cycles and do not require candidate payments."
        });
      }
    }

    // Company verification cards
    const hasEvidence = Array.isArray(report.evidence) && report.evidence.length > 0;
    const officialSourceFound = hasEvidence && report.evidence.some(e => e.category === "OFFICIAL_SOURCE" || e.category === "OFFICIAL_REGISTRY");
    const hasListingUrl = Boolean(report.internshipUrl);
    const domainReachable = report.listingDetails && report.listingDetails.domainReachable;

    const companyVerification = {
      companyName: report.companyName || "Target Employer",
      identity: {
        status: officialSourceFound ? "pass" : hasEvidence ? "pass" : "warn",
        badge: officialSourceFound ? "Official Source Found" : hasEvidence ? "Search Records Found" : "Unverified Entity",
        detail: officialSourceFound
          ? "Verified against official company registries and career indexes."
          : hasEvidence
            ? `Discovered ${report.evidence.length} external search sources for this entity.`
            : "Limited external registry or corporate footprints confirmed."
      },
      website: {
        status: domainReachable ? "pass" : hasListingUrl ? "warn" : "pass",
        badge: domainReachable ? "Domain Verified" : hasListingUrl ? "Domain Unreachable" : "Standard Reference",
        detail: hasListingUrl
          ? (domainReachable ? "Target employer careers domain is live and accessible." : "Unable to establish secure connection to provided listing link.")
          : "Evaluation relied on company search citations and offer text."
      },
      recruiter: {
        status: riskLevel === "danger" ? "fail" : riskLevel === "warning" ? "warn" : "pass",
        badge: riskLevel === "danger" ? "High Risk Signals" : riskLevel === "warning" ? "Caution Advised" : "Standard Channel",
        detail: riskLevel === "danger"
          ? "Payment demand or communication channel violates standard employment practices."
          : riskLevel === "warning"
            ? "Ensure recruiter correspondence originates from verified corporate domain email."
            : "No recruiter impersonation or payment red flags triggered."
      }
    };

    // Recommended actions
    const recommendedActions = [];
    if (report.recommendationExplanation) {
      recommendedActions.push(report.recommendationExplanation);
    }

    if (riskLevel === "danger") {
      recommendedActions.push("Do NOT transfer any money, registration fees, or equipment deposits under any circumstance.");
      recommendedActions.push("Immediately cease contact with the sender across all communication channels.");
      recommendedActions.push("Verify the position independently by calling the company's verified corporate headquarters.");
    } else if (riskLevel === "warning") {
      recommendedActions.push("Insist on written correspondence using an official corporate domain email address (@company.com).");
      recommendedActions.push("Refuse text-only interviews; insist on a scheduled video conference with human resources.");
      recommendedActions.push("Search LinkedIn for the recruiter's full name to verify their active corporate role.");
    } else {
      recommendedActions.push("Always double-check that the sender's email domain accurately matches the company's real website.");
      recommendedActions.push("Verify the internship position is officially listed on the company's careers portal.");
      recommendedActions.push("Keep copies of all interview scheduling notes and formal offer documentation.");
    }

    return {
      score: score,
      riskLevel: riskLevel,
      classification: classification,
      riskBadge: riskBadge,
      summary: summary,
      companyVerification: companyVerification,
      warningSigns: warningSigns,
      recommendedActions: recommendedActions,
      companyName: report.companyName,
      internshipUrl: report.internshipUrl,
      sampleText: report.findings || ""
    };
  }

  function getRuleWhyItMatters(category, ruleName) {
    if (category === "PAYMENT_FRAUD") {
      return "Legitimate employers never require job seekers to pay money for equipment, training modules, or background checks.";
    }
    if (category === "UNOFFICIAL_COMMUNICATION") {
      return "Encrypted chat platforms and generic freemail accounts are commonly used to prevent accountability and mask fraudulent identities.";
    }
    if (category === "SUSPICIOUS_DOMAIN" || category === "OFFICIAL_PORTAL_BYPASS") {
      return "Spoofed or unconfirmed domains allow malicious actors to impersonate reputable brands.";
    }
    return "Adhering to verified recruitment workflows prevents credential theft and financial losses.";
  }

  function extractCompanyFromText(text) {
    if (!text) return null;
    const match = text.match(/(?:at|from|with|by)\s+([A-Z][A-Za-z0-9&.\s]{2,30}?)(?=[,.;\n]|\s+for|\s+as|\s+to|\s+via|\s+is|\s+in)/);
    if (match && match[1]) {
      return match[1].trim();
    }
    return null;
  }

  // Render Full Results Interface
  function renderAnalysisResult(data) {
    // 1. Circular Progress Gauge & Score
    updateCircularGauge(data.score, data.riskLevel);

    // 2. Risk Classification & Overview Heading
    const badgeEl = document.getElementById("resultClassificationBadge");
    const headingEl = document.getElementById("resultHeading");
    const summaryEl = document.getElementById("resultSummary");

    if (badgeEl) {
      badgeEl.className = `classification-badge badge-${data.riskLevel}`;
      badgeEl.textContent = data.classification || (data.riskLevel === "danger" ? "High Risk" : data.riskLevel === "warning" ? "Medium Risk" : "Low Risk");
    }

    if (headingEl) {
      headingEl.textContent = data.riskBadge;
    }

    if (summaryEl) {
      summaryEl.textContent = data.summary;
    }

    // 3. Company Verification Summary Card
    renderCompanyVerification(data.companyVerification);

    // 4. Detected Warning Signs Cards
    renderWarningSigns(data.warningSigns || []);

    // 5. Recommended Actions
    renderRecommendedActions(data.recommendedActions || [], data.riskLevel);
  }

  // Update circular gauge with stroke-dashoffset transition
  function updateCircularGauge(score, riskLevel) {
    const ring = document.getElementById("gaugeProgressRing");
    const scoreVal = document.getElementById("gaugeScoreValue");

    if (!ring || !scoreVal) return;

    // Circumference for r=52 is 2 * PI * 52 = 326.7
    const circumference = 326.7;
    const offset = circumference - (score / 100) * circumference;

    ring.className = `gauge-progress ${riskLevel}`;
    ring.style.strokeDashoffset = offset;

    animateValue(scoreVal, 0, score, 600, "%");
  }

  function animateValue(elem, start, end, duration, suffix = "") {
    let startTimestamp = null;
    const step = (timestamp) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      const current = Math.floor(progress * (end - start) + start);
      elem.textContent = `${current}${suffix}`;
      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        elem.textContent = `${end}${suffix}`;
      }
    };
    window.requestAnimationFrame(step);
  }

  // Render Company Verification
  function renderCompanyVerification(verification) {
    const grid = document.getElementById("companyVerificationGrid");
    if (!grid || !verification) return;

    const items = [
      { key: "identity", label: "Company Identity", data: verification.identity },
      { key: "website", label: "Official Website", data: verification.website },
      { key: "recruiter", label: "Recruiter Contact", data: verification.recruiter }
    ];

    grid.innerHTML = items.map(item => {
      const status = item.data.status;
      const statusIcon = status === "pass"
        ? '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>'
        : status === "warn"
          ? '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>'
          : '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';

      return `
        <div class="verification-item">
          <div class="verification-item-header">
            <span class="verification-name">${item.label}</span>
            <span class="verification-badge ${status}">
              ${statusIcon} ${item.data.badge}
            </span>
          </div>
          <div class="verification-detail">${item.data.detail}</div>
        </div>
      `;
    }).join("");
  }

  // Render Warning Signs Cards
  function renderWarningSigns(signs) {
    const container = document.getElementById("warningCardsGrid");
    if (!container) return;

    if (signs.length === 0) {
      container.innerHTML = `<div class="warning-card safe">
        <div class="warning-title">No explicit warning signs triggered</div>
        <p class="warning-observation">Standard scam keywords were not found in this sample.</p>
      </div>`;
      return;
    }

    container.innerHTML = signs.map(sign => {
      const iconSvg = getWarningIconSvg(sign.icon);
      return `
        <div class="warning-card ${sign.type}">
          <div class="warning-card-top">
            <div class="warning-title-group">
              <span class="warning-icon">${iconSvg}</span>
              <span class="warning-title">${sign.title}</span>
            </div>
            <span class="badge badge-${sign.type}">${sign.severity}</span>
          </div>
          <p class="warning-observation">${sign.observation}</p>
          <div class="why-matters-box">
            <span class="why-matters-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
              Why this matters:
            </span>
            ${sign.whyItMatters}
          </div>
        </div>
      `;
    }).join("");
  }

  function getWarningIconSvg(iconName) {
    switch (iconName) {
      case "dollar":
      case "credit-card":
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>`;
      case "chat":
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`;
      case "mail":
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>`;
      case "shield-check":
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/></svg>`;
      case "globe":
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`;
      default:
        return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;
    }
  }

  // Render Recommended Actions
  function renderRecommendedActions(actions, riskLevel) {
    const list = document.getElementById("recommendedActionsList");
    const card = document.getElementById("recommendedActionsCard");
    if (!list) return;

    if (card) {
      card.className = `recommended-actions-card ${riskLevel}`;
    }

    list.innerHTML = actions.map((action, index) => `
      <div class="action-step-item">
        <span class="action-number">${index + 1}</span>
        <span>${action}</span>
      </div>
    `).join("");
  }

  // Session History Management
  function addSessionHistory(data, label) {
    const now = new Date();
    const timeString = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const historyItem = {
      id: Date.now(),
      label: label || "Analyzed Offer",
      timestamp: timeString,
      score: data.score,
      riskLevel: data.riskLevel,
      classification: data.classification || "Result",
      companyName: data.companyName,
      internshipUrl: data.internshipUrl,
      data: data
    };

    sessionChecks.unshift(historyItem);
    if (sessionChecks.length > 8) sessionChecks.pop();

    renderSessionHistory();
  }

  function renderSessionHistory() {
    if (!recentChecksList || !recentEmptyState) return;

    if (sessionChecks.length === 0) {
      recentEmptyState.style.display = "block";
      recentChecksList.style.display = "none";
      return;
    }

    recentEmptyState.style.display = "none";
    recentChecksList.style.display = "flex";

    recentChecksList.innerHTML = sessionChecks.map(item => `
      <div class="recent-item">
        <div class="recent-info">
          <div>
            <div class="recent-item-title">${item.label}</div>
            <div class="recent-item-time">Checked at ${item.timestamp}</div>
          </div>
        </div>
        <div class="recent-item-actions">
          <span class="badge badge-${item.riskLevel}">${item.score}% ${item.classification}</span>
          <button type="button" class="btn btn-secondary btn-xs reload-check-btn" data-id="${item.id}">
            Re-examine
          </button>
        </div>
      </div>
    `).join("");

    // Attach click listeners to reload past checks with header offset
    recentChecksList.querySelectorAll(".reload-check-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const id = Number(btn.getAttribute("data-id"));
        const found = sessionChecks.find(c => c.id === id);
        if (found) {
          cancelPendingAnalysisScroll();
          if (scannerEmptyState) scannerEmptyState.classList.add("hidden");
          if (companyInput && found.companyName) companyInput.value = found.companyName;
          if (urlInput) urlInput.value = found.internshipUrl || "";
          if (offerInput) offerInput.value = found.data.sampleText || "";
          renderAnalysisResult(found.data);
          analysisResult.classList.add("visible");
          smoothScrollToElement(analysisResult);
        }
      });
    });
  }

  // Accordion Toggle
  const accordionHeaders = document.querySelectorAll(".accordion-header");
  accordionHeaders.forEach(header => {
    header.addEventListener("click", () => {
      const parent = header.closest(".accordion-item");
      const isOpen = parent.classList.contains("active");

      document.querySelectorAll(".accordion-item").forEach(item => {
        item.classList.remove("active");
      });

      if (!isOpen) {
        parent.classList.add("active");
      }
    });
  });

  // On page load, verify active Supabase session before querying backend history
  if (window.supabaseClient && window.supabaseClient.auth) {
    window.supabaseClient.auth.getSession().then(({ data }) => {
      if (data && data.session) {
        loadRecentChecksFromBackend();
      } else {
        sessionChecks = [];
        renderSessionHistory();
      }
    }).catch(() => {
      sessionChecks = [];
      renderSessionHistory();
    });

    // Listen for Supabase auth state changes (login / logout / token refreshed)
    window.supabaseClient.auth.onAuthStateChange((event, session) => {
      if (event === "SIGNED_IN" || event === "TOKEN_REFRESHED" || event === "INITIAL_SESSION") {
        if (session) {
          loadRecentChecksFromBackend();
        }
      } else if (event === "SIGNED_OUT") {
        sessionChecks = [];
        renderSessionHistory();
      }
    });
  } else {
    sessionChecks = [];
    renderSessionHistory();
  }
});
