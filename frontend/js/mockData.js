/**
 * InternShield AI - Mock Scenarios & Detection Signatures
 * Provides representative sample data for client-side UI simulation and testing.
 * All data is strictly for educational and demonstration purposes.
 */

const MOCK_SCENARIOS = {
  fee_scam: {
    id: "fee_scam",
    label: "Training Fee Demand",
    sampleText: "Congratulations! You have been selected for the Remote Web Development Internship at Apex Global Tech. To finalize your onboarding and receive company software licenses, you are required to pay a mandatory refundable registration and training fee of $150 via UPI/Wire transfer. Once paid, your training module begins immediately.",
    score: 94,
    riskLevel: "danger",
    classification: "High Risk",
    riskBadge: "High Risk - Probable Scam",
    summary: "The offer demands an upfront monetary payment for 'registration', 'training modules', or 'software tools'. Legitimate employers NEVER demand payments from candidates during recruitment or onboarding.",
    // Warning Signs with "Why this matters" explanation
    warningSigns: [
      {
        title: "Upfront Fee or Training Demand",
        severity: "Critical",
        type: "danger",
        icon: "dollar",
        observation: "Mandatory $150 refundable registration/training fee requested before onboarding.",
        whyItMatters: "Genuine employers absorb candidate onboarding, background checks, and software provisioning costs. Asking candidates for advance fees is the single most definitive indicator of employment fraud."
      },
      {
        title: "Unusual & Irreversible Payment Method",
        severity: "Critical",
        type: "danger",
        icon: "credit-card",
        observation: "Demands direct funds transfer via Wire transfer / UPI.",
        whyItMatters: "Direct wire and peer-to-peer transfers lack consumer protection or chargeback mechanisms, allowing scammers to pocket untraceable funds immediately."
      },
      {
        title: "Instant Offer Without Due Evaluation",
        severity: "Moderate",
        type: "warning",
        icon: "user-x",
        observation: "Immediate onboarding offered without prior technical interviews, code reviews, or voice screening.",
        whyItMatters: "Scammers cast wide nets by offering instant gratification to students, bypassing standard assessment cycles to rush victims into paying fees."
      }
    ],

    // Company verification demo indicators
    companyVerification: {
      isSimulated: true,
      companyName: "Apex Global Tech",
      identity: {
        status: "fail",
        badge: "Unverified Entity",
        detail: "No business registration or verifiable corporate footprint found matching this hiring entity."
      },
      website: {
        status: "fail",
        badge: "Missing Corporate Domain",
        detail: "No secure official domain provided; correspondence relies on unverified channels."
      },
      recruiter: {
        status: "fail",
        badge: "High Impersonation Risk",
        detail: "Sender contact details do not correspond to any known corporate employee or authorized recruiter."
      }
    },

    // Actionable recommendations
    recommendedActions: [
      "Do NOT transfer any money, registration fees, or security deposits under any circumstance.",
      "Immediately cease all communications with the sender across all channels.",
      "Report the scam message to your university career center and the platform where you found the posting."
    ]
  },

  telegram_scam: {
    id: "telegram_scam",
    label: "Telegram / WhatsApp Only",
    sampleText: "Hi! We reviewed your profile on LinkedIn and are excited to offer you a part-time Data Entry position paying $45/hour. No prior experience is needed. Please install Telegram and contact our HR manager @hiring_officer_fast to conduct your text-only interview and receive your welcome packet.",
    score: 68,
    riskLevel: "warning",
    classification: "Medium Risk",
    riskBadge: "Medium Risk - Suspicious Patterns",
    summary: "Recruiter insists on text-only communication via third-party messaging apps (Telegram) without official company emails, scheduled video meetings, or corporate credentials.",

    warningSigns: [
      {
        title: "Off-Platform Text-Only Interview",
        severity: "Moderate",
        type: "warning",
        icon: "chat",
        observation: "Interview requested via personal Telegram handle (@hiring_officer_fast) rather than official tools.",
        whyItMatters: "Encrypted messaging apps prevent accountability, hide the interviewer's true location, and allow fraudulent accounts to vanish once sensitive data or funds are collected."
      },
      {
        title: "Disproportionate Compensation Ratio",
        severity: "Moderate",
        type: "warning",
        icon: "trending-up",
        observation: "Offers $45/hour for entry-level data entry with explicit 'no prior experience needed'.",
        whyItMatters: "Unrealistically generous wage promises are weaponized to trigger urgency and override a student's natural skepticism."
      },
      {
        title: "Absence of Corporate Domain Correspondence",
        severity: "Moderate",
        type: "warning",
        icon: "mail",
        observation: "Initial contact bypasses verified corporate email addresses or verified company career portals.",
        whyItMatters: "Legitimate corporate recruiters almost exclusively utilize verifiable company domain addresses (e.g., recruiter@company.com)."
      }
    ],

    companyVerification: {
      isSimulated: true,
      companyName: "Unspecified TechStaffing Group",
      identity: {
        status: "warn",
        badge: "Partially Identified",
        detail: "Company name exists as a common moniker, making it vulnerable to brand spoofing."
      },
      website: {
        status: "warn",
        badge: "Unlinked Portal",
        detail: "Candidate was not directed to an established corporate applicant tracking system."
      },
      recruiter: {
        status: "fail",
        badge: "Unverified Handle",
        detail: "Recruiter identity is an anonymous Telegram handle with no LinkedIn or email correlation."
      }
    },

    recommendedActions: [
      "Insist on conducting all professional correspondence via an official company domain email address.",
      "Refuse text-only interviews on messaging platforms; request a scheduled video call with verified HR staff.",
      "Independently locate the company's verified website and contact their talent team to verify the recruiter's name."
    ]
  },

  legitimate_offer: {
    id: "legitimate_offer",
    label: "Legitimate Offer Sample",
    sampleText: "Dear Candidate, following your technical interview and code review with our engineering team, Acme Corp is pleased to offer you the Summer Software Engineering Internship. Your stipend is $28/hour. Please review the attached official offer document and submit your signed copy through our careers portal at careers.acme.com by next Friday.",
    score: 12,
    riskLevel: "safe",
    classification: "Low Risk",
    riskBadge: "Low Risk - Verified Pattern",
    summary: "Standard recruitment sequence matching verified corporate practices: references prior technical assessments, provides formal documentation, and utilizes an official career portal domain.",

    warningSigns: [
      {
        title: "Zero Monetary Demands",
        severity: "Safe Indicator",
        type: "safe",
        icon: "shield-check",
        observation: "No application, training, equipment, or background check fees requested.",
        whyItMatters: "Completely aligns with fair labor practices; professional employers compensate employees rather than charge them."
      },
      {
        title: "Verified Domain Applicant Portal",
        severity: "Safe Indicator",
        type: "safe",
        icon: "globe",
        observation: "Document submission managed through authenticated portal: careers.acme.com.",
        whyItMatters: "Dedicated subdomains linked to primary corporate domains confirm that the communication originates from within the legitimate organization."
      },
      {
        title: "Standard Hiring Sequence Followed",
        severity: "Safe Indicator",
        type: "safe",
        icon: "file-text",
        observation: "Offer directly references prior technical interviews, code reviews, and formal deadlines.",
        whyItMatters: "Legitimate technical internships require multi-stage vetting before extending binding formal employment offers."
      }
    ],

    companyVerification: {
      isSimulated: true,
      companyName: "Acme Corporation",
      identity: {
        status: "pass",
        badge: "Verified Entity Pattern",
        detail: "Standard corporate entity with documented hiring policies and public registry."
      },
      website: {
        status: "pass",
        badge: "Domain Match: acme.com",
        detail: "Valid SSL certificate and authenticated enterprise career sub-portal."
      },
      recruiter: {
        status: "pass",
        badge: "Verified Recruiter Pipeline",
        detail: "Correspondence routes through legitimate company hiring systems."
      }
    },

    recommendedActions: [
      "Review the formal contract terms, working hours, and intellectual property clauses carefully.",
      "Submit signed agreements exclusively through the official corporate career portal URL.",
      "Confirm receipt with your assigned HR coordinator before the deadline."
    ]
  }
};
