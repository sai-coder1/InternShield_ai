/**
 * InternShield AI - Main UI Controller
 * Handles user interactions, unified smooth navigation, mobile menu,
 * simulated analysis flow with safe scroll handling, circular risk gauge animations,
 * company verification, and session history.
 */

document.addEventListener("DOMContentLoaded", () => {
  // Navigation elements
  const mobileToggle = document.getElementById("mobileToggle");
  const navMenu = document.getElementById("navMenu");

  // Scanner UI Components
  const offerInput = document.getElementById("offerInput");
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
  let analysisTimeoutId = null;

  /**
   * Unified smooth scroll function that offsets for the sticky header
   * Ensures target section headings and cards are never hidden under the navigation bar.
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

  /**
   * Cancel pending automatic analysis scrolls so delayed timers
   * do not interrupt or hijack a user's manual navigation.
   */
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

  /**
   * Unified Click Handler for ALL Internal Anchor Links:
   * - Header nav links (#scanner, #features, #red-flags, #checklist)
   * - Header "Check Offer" button (#scanner)
   * - Brand logo (#top)
   * - Hero "Check a Job Offer" button (#scanner)
   * - Hero "View Scam Signals" button (#red-flags)
   * - Footer links (#top, #scanner, #features, #red-flags, #checklist)
   */
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener("click", (e) => {
      const href = anchor.getAttribute("href");
      if (!href || href === "#") return;

      const targetElement = href === "#top"
        ? document.body
        : document.querySelector(href);

      if (targetElement) {
        e.preventDefault();

        // 1. Invalidate any in-flight analysis scrolling so manual navigation takes precedence
        cancelPendingAnalysisScroll();

        // 2. Automatically close mobile hamburger menu if open
        if (navMenu && navMenu.classList.contains("open")) {
          navMenu.classList.remove("open");
          if (mobileToggle) {
            mobileToggle.setAttribute("aria-expanded", "false");
          }
        }

        // 3. Smoothly scroll to the target section with header offset
        smoothScrollToElement(targetElement);

        // 4. Update browser URL history cleanly without sudden jumps
        if (history.pushState) {
          history.pushState(null, "", href);
        }
      }
    });
  });

  // Populate sample scenario when chip is clicked
  scenarioChips.forEach(chip => {
    chip.addEventListener("click", () => {
      const scenarioKey = chip.getAttribute("data-scenario");
      scenarioChips.forEach(c => c.classList.remove("active"));
      chip.classList.add("active");

      if (MOCK_SCENARIOS && MOCK_SCENARIOS[scenarioKey]) {
        const scenario = MOCK_SCENARIOS[scenarioKey];
        offerInput.value = scenario.sampleText;
        runAnalysis(scenario, scenario.label);
      }
    });
  });

  // Clear button resets input, hides result, displays empty state
  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      cancelPendingAnalysisScroll();
      offerInput.value = "";
      scenarioChips.forEach(c => c.classList.remove("active"));
      if (analysisResult) analysisResult.classList.remove("visible");
      if (scanningIndicator) scanningIndicator.classList.remove("active");
      if (scannerEmptyState) scannerEmptyState.classList.remove("hidden");
      offerInput.focus();
    });
  }

  // Analyze button click
  if (analyzeBtn) {
    analyzeBtn.addEventListener("click", () => {
      const text = offerInput.value.trim();
      if (!text) {
        alert("Please paste or type an offer letter, message, or job snippet to check.");
        offerInput.focus();
        return;
      }

      // Check if text matches known scenario or run dynamic rule evaluation
      const matched = evaluateOfferText(text);
      runAnalysis(matched, "Custom Offer Input");
    });
  }

  /**
   * Simulate scanning state with loading animation and safe scroll handling
   */
  function runAnalysis(data, sourceLabel) {
    if (!scanningIndicator || !analysisResult) return;

    const thisAnalysisId = ++currentAnalysisId;

    if (scannerEmptyState) scannerEmptyState.classList.add("hidden");
    analysisResult.classList.remove("visible");
    scanningIndicator.classList.add("active");
    if (analyzeBtn) analyzeBtn.disabled = true;

    analysisTimeoutId = setTimeout(() => {
      scanningIndicator.classList.remove("active");
      if (analyzeBtn) analyzeBtn.disabled = false;

      renderAnalysisResult(data);
      addSessionHistory(data, sourceLabel);
      analysisResult.classList.add("visible");

      // Only scroll to results if the user did NOT navigate away during the scan
      if (thisAnalysisId === currentAnalysisId) {
        const scannerSection = document.getElementById("scanner");
        if (scannerSection) {
          const rect = scannerSection.getBoundingClientRect();
          // If scanner area is still visible in viewport, scroll down to result card smoothly
          const isScannerInView = rect.top < window.innerHeight && rect.bottom > 100;
          if (isScannerInView) {
            smoothScrollToElement(analysisResult);
          }
        }
      }
    }, 600);
  }

  // Heuristic rule matcher for custom text
  function evaluateOfferText(text) {
    for (const key of Object.keys(MOCK_SCENARIOS)) {
      if (text === MOCK_SCENARIOS[key].sampleText) {
        return MOCK_SCENARIOS[key];
      }
    }

    const lower = text.toLowerCase();
    const hasFee = lower.includes("fee") || lower.includes("pay") || lower.includes("deposit") ||
                   lower.includes("registration") || lower.includes("training fee") ||
                   lower.includes("refundable") || lower.includes("upi") || lower.includes("wire");
    const hasMessaging = lower.includes("telegram") || lower.includes("whatsapp") ||
                         lower.includes("crypto") || lower.includes("gift card") ||
                         lower.includes("@gmail") || lower.includes("@yahoo");

    if (hasFee) {
      return {
        score: 91,
        riskLevel: "danger",
        classification: "High Risk",
        riskBadge: "High Risk - Fee Pattern Detected",
        summary: "Potential payment demand detected in text. Genuine employers do not charge candidates application, registration, training, or equipment fees.",
        warningSigns: [
          {
            title: "Upfront Fee Keywords Detected",
            severity: "Critical",
            type: "danger",
            icon: "dollar",
            observation: "Text contains indicators of fee payments, deposits, or registration charges.",
            whyItMatters: "Legitimate corporate recruiters never require job seekers to pay money for equipment, background checks, or training modules."
          },
          {
            title: "Direct Monetary Transfer Request",
            severity: "Critical",
            type: "danger",
            icon: "credit-card",
            observation: "Payment mechanism mentioned (wire/deposit/fee) with non-standard employment terms.",
            whyItMatters: "Scammers frequently request untraceable or peer-to-peer transfers before ghosting victims."
          }
        ],
        companyVerification: {
          isSimulated: true,
          companyName: "Unverified Custom Sender",
          identity: {
            status: "fail",
            badge: "Unverified Entity",
            detail: "No business registration or corporate footprint confirmed from provided snippet."
          },
          website: {
            status: "warn",
            badge: "Unconfirmed Portal",
            detail: "Custom input lacks direct reference to a verifiable corporate careers portal."
          },
          recruiter: {
            status: "fail",
            badge: "High Risk Signals",
            detail: "Monetary demand indicates an unauthorized or fraudulent recruiter identity."
          }
        },
        recommendedActions: [
          "Do NOT transfer any money or provide banking credentials under any pretext.",
          "Cease contact with the sender immediately.",
          "Check the company's verified careers portal independently to verify if the job ID exists."
        ]
      };
    } else if (hasMessaging) {
      return {
        score: 64,
        riskLevel: "warning",
        classification: "Medium Risk",
        riskBadge: "Medium Risk - Suspicious Channel",
        summary: "Communication through unofficial channels like Telegram or generic freemail accounts is a frequent hallmark of impersonation scams.",
        warningSigns: [
          {
            title: "Unofficial Messaging Platform",
            severity: "Moderate",
            type: "warning",
            icon: "chat",
            observation: "Interview or correspondence directed through third-party chat services.",
            whyItMatters: "Corporate talent teams conduct business through verified enterprise emails, ATS systems, and scheduled video conferencing."
          },
          {
            title: "Informal Hiring Workflow",
            severity: "Moderate",
            type: "warning",
            icon: "mail",
            observation: "Absence of corporate domain verification or scheduled formal evaluations.",
            whyItMatters: "Lack of institutional transparency allows malicious actors to impersonate reputable firms without detection."
          }
        ],
        companyVerification: {
          isSimulated: true,
          companyName: "Unverified Custom Recruiter",
          identity: {
            status: "warn",
            badge: "Unconfirmed Identity",
            detail: "Entity details are insufficient for complete validation."
          },
          website: {
            status: "warn",
            badge: "External Platform",
            detail: "Relies on external messaging networks rather than official employer domain."
          },
          recruiter: {
            status: "fail",
            badge: "Unofficial Channel",
            detail: "Recruiter contact is not tied to a registered enterprise email domain."
          }
        },
        recommendedActions: [
          "Insist on written correspondence using an official corporate domain email address (@company.com).",
          "Never accept text-only interviews as a substitute for verified interviews.",
          "Search LinkedIn for the recruiter's name to confirm their active employment status."
        ]
      };
    } else {
      return {
        score: 18,
        riskLevel: "safe",
        classification: "Low Risk",
        riskBadge: "Low Risk - Clean Indicators",
        summary: "No standard scam keywords (fees, crypto, unverified chat apps) were detected in this snippet.",
        warningSigns: [
          {
            title: "Absence of Monetary Requests",
            severity: "Safe Indicator",
            type: "safe",
            icon: "shield-check",
            observation: "No fees, registration charges, or security deposits found in the provided text.",
            whyItMatters: "Legitimate employment is characterized by employer-provided compensation without upfront expenses."
          },
          {
            title: "Standard Professional Language",
            severity: "Safe Indicator",
            type: "safe",
            icon: "file-text",
            observation: "Tone and structure match normal hiring correspondence.",
            whyItMatters: "Typical scam signals like urgency pressure or inflated salaries were not detected."
          }
        ],
        companyVerification: {
          isSimulated: true,
          companyName: "Self-Reported Employer",
          identity: {
            status: "pass",
            badge: "No Red Flags",
            detail: "Text does not exhibit brand spoofing or known fake employer signatures."
          },
          website: {
            status: "pass",
            badge: "Standard Reference",
            detail: "No suspicious redirection links or scam portals identified in the text."
          },
          recruiter: {
            status: "pass",
            badge: "Standard Communication",
            detail: "No requests to transition to insecure messaging platforms."
          }
        },
        recommendedActions: [
          "Always double-check that the sender's full email domain accurately matches the company's real URL.",
          "Verify the position is listed on the company's official career portal.",
          "Keep records of all formal interview invitations and written agreements."
        ]
      };
    }
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
          offerInput.value = found.data.sampleText || "";
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
});
