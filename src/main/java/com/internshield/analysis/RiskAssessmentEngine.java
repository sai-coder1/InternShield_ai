package com.internshield.analysis;

import com.internshield.ai.AIAnalysisResult;
import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceCategory;
import com.internshield.internship.dto.ExtractedInternshipDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RiskAssessmentEngine {

    // Regex patterns for the 12 deterministic checks
    private static final Pattern REGISTRATION_FEE_PATTERN = Pattern.compile(
            "(?i)(?<!(?:no |never |without |free of |no [a-zA-Z]{1,15} or ))(?:\\b(registration(?: and [a-z]+)? fee|registration charges|pay to register|form fee|application fee(?![:\\s]*(?:[$₹€£]?\\s*0\\b|none|free|nil))(?: of rs)?|enrollment fee|fee of \\$\\d+|upfront payment|upfront fee|pay upfront|pay to start|advance payment|advance fee|mandatory payment)|(?:^|(?<=[\\s(]))(?:[$₹€£]?\\s*\\d+[\\d,]*\\s*(?:certification|program|training|fee|charges|deposit)|pay(?:ment)?\\s*(?:of\\s*)?[$₹€£]\\s*\\d+))\\b");

    private static final Pattern TRAINING_FEE_PATTERN = Pattern.compile(
            "(?i)(?<!(?:no |never |without |free of |no [a-zA-Z]{1,15} or ))\\b(training fee|training charges|pay for training|mandatory course fee|onboarding charges|certification fee(?: to start)?|certification charges|certification program|paid certification|certificate fee|pay for certification|course fee|exam fee|complete (?:our|a) (?:[$₹€£]?\\s*\\d+[\\d,]*\\s*)?certification(?: program)?)\\b");

    private static final Pattern PAYMENT_AFTER_FREE_PATTERN = Pattern.compile(
            "(?i)(?<!(?:no |never |without |free of ))\\b(security deposit|security fee|equipment (?:security )?fee|refundable (?:registration|equipment|caution|security)?\\s*(?:deposit|amount|fee)|refundable caution deposit|refundable amount|pay to confirm seat|laptop deposit|equipment charges)\\b");

    private static final Pattern PUBLIC_EMAIL_DOMAINS = Pattern.compile(
            "(?i)@(gmail|yahoo|hotmail|outlook|rediffmail|protonmail|aol|icloud)\\.com");

    private static final Pattern TELEGRAM_WHATSAPP_PATTERN = Pattern.compile(
            "(?i)\\b(telegram|t\\.me\\/|whatsapp|wa\\.me\\/|chat group)\\b");

    private static final Pattern GUARANTEED_SELECTION_PATTERN = Pattern.compile(
            "(?i)\\b(guaranteed (?:selection|job|a job|placement|employment|hiring|offer)|100% (?:guaranteed(?: a job| job)?|selection|placement|job guarantee)|job guarantee|placement guarantee|direct selection|no interview|offer letter without interview|immediate offer letter)\\b");

    private static final Pattern UNCLEAR_RESPONSIBILITIES_PATTERN = Pattern.compile(
            "(?i)\\b(simple typing|copy paste|data entry work from home|fill captchas|earn 2000 daily|no skills required)\\b");

    private static final Pattern UNREALISTIC_STIPEND_PATTERN = Pattern.compile(
            "(?i)\\b(stipend|salary)[:\\s]*(?:rs\\.?|inr|₹|\\$)?\\s*(?:[5-9]\\d{4}|[1-9]\\d{5}|[5-9]\\d{2}\\s*(?:per week|/week)|[1-9]\\d{3}\\s*(?:per week|/week))\\s*(?:per month|/month|p\\.m\\.|per week|/week)?\\b");

    private static final Pattern ARTIFICIAL_URGENCY_PATTERN = Pattern.compile(
            "(?i)\\b(pay within|urgent requirement|offer expires in|valid for today only|last \\d+ seats|limited seats?(?:[:\\s]*\\d+)?|seats? remaining|\\b\\d+ seats? left\\b|only \\d+ seats?|immediate payment|within \\d+ hours?|hurry up|seats filling fast)\\b");

    private static final Pattern SUSPICIOUS_DOMAIN_TLDS = Pattern.compile(
            "(?i)\\.(xyz|top|buzz|click|club|rest|link|cam|work|gq|cf|tk|ml)$");

    private static final Pattern UNOFFICIAL_PAYMENT_PATTERN = Pattern.compile(
            "(?i)\\b(payment[:\\s]+(?:via\\s+|through\\s+)?(?:upi|gpay|google pay|phonepe|paytm|bhim|qr code|crypto|cryptocurrency)|pay(?:ment)? (?:via|through|using) (?:upi|gpay|google pay|phonepe|paytm|bhim|qr code|crypto)|send (?:fee|money|amount|payment) (?:to|via) upi|(?:upi|gpay|phonepe|paytm) id|scan qr(?: code)? to pay|payment[:\\s]*upi)\\b");

    // Semantic keyword dictionaries for mapping Gemini AI qualitative findings into deterministic rules
    private static final String[] UPFRONT_FEE_KEYWORDS = {
            "upfront", "registration fee", "application fee", "enrollment fee",
            "onboarding fee", "processing fee", "kit charge", "charge of rs",
            "charge of inr", "charge of $", "pay to apply", "pay before",
            "advance payment", "advance fee", "initial payment", "remit advance",
            "fee demand", "fee demanded", "fee required"
    };

    private static final String[] PAID_TRAINING_CERT_KEYWORDS = {
            "training fee", "paid training", "certification fee", "paid certification",
            "certification program", "mandatory certificate", "mandatory course",
            "course fee", "pay for training", "pay for certification", "pay for course",
            "certificate fee", "program fee"
    };

    private static final String[] SECURITY_DEPOSIT_KEYWORDS = {
            "security deposit", "caution deposit", "caution money", "refundable deposit",
            "equipment deposit", "laptop deposit", "seat confirmation deposit", "deposit of rs",
            "refundable amount"
    };

    private static final String[] SUSPICIOUS_PAYMENT_KEYWORDS = {
            "upi", "qr code", "gpay", "google pay", "phonepe", "paytm", "crypto",
            "bitcoin", "phone wallet", "personal bank account", "informal payment",
            "p2p payment", "unverified payment", "personal account"
    };

    private static final String[] GUARANTEED_JOB_KEYWORDS = {
            "guaranteed job", "guaranteed placement", "guaranteed selection", "direct appointment",
            "direct placement", "direct hiring", "direct offer", "no interview", "without interview",
            "zero screening", "no screening", "without assessment", "without technical evaluation",
            "100% placement", "100% selection", "100% guaranteed"
    };

    private static final String[] URGENCY_PRESSURE_KEYWORDS = {
            "limited seats", "urgency", "urgent requirement", "valid today", "expires in",
            "immediate payment", "seats remaining", "seats left", "pressure tactic", "deadline pressure"
    };

    private static final String[] SCAM_RECORD_KEYWORDS = {
            "scam report", "scam allegation", "fraud report", "fake offer", "unregistered entity",
            "impersonat", "complaints found", "previous scam"
    };

    /**
     * Preserved 3-argument method for backward compatibility.
     */
    public RiskAssessmentResult evaluateRisk(
            String companyName,
            ExtractedInternshipDetails listing,
            List<Evidence> evidenceList) {
        return evaluateRisk(companyName, listing, evidenceList, null);
    }

    /**
     * Executes deterministic rule evaluation across extracted listing details, collected research evidence,
     * and qualitative Gemini AI credibility findings.
     *
     * @param companyName company under review
     * @param listing extracted details from URL or pasted text
     * @param evidenceList persisted research evidence items
     * @param aiResult optional Gemini AI analysis result
     * @return comprehensive risk assessment result with transparent scoring
     */
    public RiskAssessmentResult evaluateRisk(
            String companyName,
            ExtractedInternshipDetails listing,
            List<Evidence> evidenceList,
            AIAnalysisResult aiResult) {

        List<RiskRuleViolation> violations = new ArrayList<>();
        List<String> positiveSignals = new ArrayList<>();

        int score = 10; // Baseline vigilance score
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Baseline vigilance score: +10.\n");

        String combinedText = buildCombinedCorpus(listing, evidenceList);

        // Check for evidence sufficiency
        boolean hasListing = listing != null && !listing.unreachable() && !listing.rawContent().isBlank();
        boolean hasEvidence = evidenceList != null && !evidenceList.isEmpty();

        if (!hasListing && (!hasEvidence || evidenceList.size() <= 1)) {
            return new RiskAssessmentResult(
                    50,
                    "INSUFFICIENT_EVIDENCE",
                    RiskRecommendation.INSUFFICIENT_EVIDENCE,
                    "Insufficient public records and listing information found to conclusively assess risk. Verify company identity independently.",
                    List.of(),
                    List.of(),
                    false,
                    "Insufficient search hits and no accessible listing content. Minimum evidence threshold not met."
            );
        }

        // Rule 1: Registration / Upfront fee requests (+45, CRITICAL)
        boolean rule01Triggered = false;
        if (REGISTRATION_FEE_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_01_REG_FEE",
                    "Internship Registration Fee Demanded",
                    "FINANCIAL_DEMAND",
                    45,
                    "CRITICAL",
                    "The opportunity or public complaints indicate fees are demanded for internship registration.",
                    findMatches(REGISTRATION_FEE_PATTERN, combinedText)
            ));
            score += 45;
            rule01Triggered = true;
            breakdown.append("Triggered Rule 01 (Registration Fee): +45.\n");
        }

        // Rule 2: Training or onboarding fee demands (+45, CRITICAL)
        boolean rule02Triggered = false;
        if (TRAINING_FEE_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_02_TRAINING_FEE",
                    "Training/Onboarding Fee Demanded",
                    "FINANCIAL_DEMAND",
                    45,
                    "CRITICAL",
                    "Candidates are requested to pay for training, onboarding courses, or certifications.",
                    findMatches(TRAINING_FEE_PATTERN, combinedText)
            ));
            score += 45;
            rule02Triggered = true;
            breakdown.append("Triggered Rule 02 (Training Fee): +45.\n");
        }

        // Rule 3: Payment after advertising free / Security Deposit (+40, CRITICAL)
        boolean rule03Triggered = false;
        if (PAYMENT_AFTER_FREE_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_03_SECURITY_DEPOSIT",
                    "Mandatory Caution Deposit / Equipment Charge",
                    "FINANCIAL_DEMAND",
                    40,
                    "CRITICAL",
                    "Requests for security deposit, equipment charges, or seat confirmation payments.",
                    findMatches(PAYMENT_AFTER_FREE_PATTERN, combinedText)
            ));
            score += 40;
            rule03Triggered = true;
            breakdown.append("Triggered Rule 03 (Security Deposit/Payment): +40.\n");
        }

        // Rule 4: Suspicious Recruiter Email Domain (+25, HIGH)
        String recruiterEmail = listing != null ? listing.recruiterEmail() : "";
        if (!recruiterEmail.isBlank() && PUBLIC_EMAIL_DOMAINS.matcher(recruiterEmail).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_04_PUBLIC_EMAIL",
                    "Recruiter Using Free Public Email Provider",
                    "COMMUNICATION_CHANNEL",
                    25,
                    "HIGH",
                    "The recruiter uses a free webmail address (" + recruiterEmail + ") rather than an official corporate domain.",
                    List.of(recruiterEmail)
            ));
            score += 25;
            breakdown.append("Triggered Rule 04 (Free Public Recruiter Email): +25.\n");
        }

        // Rule 5: Email Domain Mismatch (+25, HIGH)
        if (!recruiterEmail.isBlank() && companyName != null && !companyName.isBlank()) {
            String emailDomain = extractDomainFromEmail(recruiterEmail);
            if (!emailDomain.isBlank() && !PUBLIC_EMAIL_DOMAINS.matcher(recruiterEmail).find()) {
                String cleanComp = companyName.toLowerCase(Locale.ROOT)
                        .replaceAll("\\b(pvt|ltd|limited|private|inc|llc|corp|corporation)\\b", "")
                        .replaceAll("[^a-z0-9]", "");
                String cleanDomain = emailDomain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                if (cleanComp.length() > 3 && !cleanDomain.contains(cleanComp)) {
                    violations.add(new RiskRuleViolation(
                            "RULE_05_DOMAIN_MISMATCH",
                            "Recruiter Domain Differs From Company Identity",
                            "IMPERSONATION",
                            25,
                            "HIGH",
                            "The recruiter email domain (" + emailDomain + ") does not match the company name (" + companyName + ").",
                            List.of(recruiterEmail)
                    ));
                    score += 25;
                    breakdown.append("Triggered Rule 05 (Domain Mismatch): +25.\n");
                }
            }
        }

        // Rule 6: WhatsApp / Telegram-Only Recruitment (+30, HIGH)
        if (listing != null && ("TELEGRAM".equals(listing.communicationChannel()) || "WHATSAPP".equals(listing.communicationChannel()))) {
            violations.add(new RiskRuleViolation(
                    "RULE_06_MESSAGING_ONLY",
                    "Recruitment Conducted Exclusively Via Instant Messaging",
                    "COMMUNICATION_CHANNEL",
                    30,
                    "HIGH",
                    "The hiring process directs applicants exclusively through " + listing.communicationChannel() + " without formal corporate portals.",
                    List.of(listing.communicationChannel())
            ));
            score += 30;
            breakdown.append("Triggered Rule 06 (Messaging-only recruitment): +30.\n");
        }

        // Rule 7: Guaranteed Selection Without Evaluation (+25, HIGH)
        boolean rule07Triggered = false;
        if (GUARANTEED_SELECTION_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_07_GUARANTEED_SELECTION",
                    "Guaranteed Selection Without Assessment",
                    "RECRUITMENT_PROCESS",
                    25,
                    "HIGH",
                    "Claims of guaranteed placement or immediate offer issuance without technical interview or evaluation.",
                    findMatches(GUARANTEED_SELECTION_PATTERN, combinedText)
            ));
            score += 25;
            rule07Triggered = true;
            breakdown.append("Triggered Rule 07 (Guaranteed Selection): +25.\n");
        }

        // Rule 8: Unclear / Vague Responsibilities (+15, MEDIUM)
        if (UNCLEAR_RESPONSIBILITIES_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_08_VAGUE_DUTIES",
                    "Unclear or Vague Internship Responsibilities",
                    "JOB_QUALITY",
                    15,
                    "MEDIUM",
                    "Listing specifies vague, repetitive micro-tasks (e.g. captcha filling, copy-paste) rather than genuine educational internship projects.",
                    findMatches(UNCLEAR_RESPONSIBILITIES_PATTERN, combinedText)
            ));
            score += 15;
            breakdown.append("Triggered Rule 08 (Vague Responsibilities): +15.\n");
        }

        // Rule 9: Ambiguous or Unrealistic Stipend (+20, MEDIUM)
        if (UNREALISTIC_STIPEND_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_09_UNREALISTIC_STIPEND",
                    "Unusually High or Disproportionate Stipend Claims",
                    "STIPEND_AND_OFFER",
                    20,
                    "MEDIUM",
                    "Promises unusually high initial stipend amounts disproportionate to standard student internship positions.",
                    findMatches(UNREALISTIC_STIPEND_PATTERN, combinedText)
            ));
            score += 20;
            breakdown.append("Triggered Rule 09 (Unrealistic Stipend): +20.\n");
        }

        // Rule 10: Artificial Urgency or Pressure (+30, HIGH)
        boolean rule10Triggered = false;
        if (ARTIFICIAL_URGENCY_PATTERN.matcher(combinedText).find()) {
            violations.add(new RiskRuleViolation(
                    "RULE_10_ARTIFICIAL_URGENCY",
                    "Artificial Urgency or Payment Pressure",
                    "PSYCHOLOGICAL_PRESSURE",
                    30,
                    "HIGH",
                    "High-pressure sales tactics creating artificial urgency to force rapid payment or commitment.",
                    findMatches(ARTIFICIAL_URGENCY_PATTERN, combinedText)
            ));
            score += 30;
            rule10Triggered = true;
            breakdown.append("Triggered Rule 10 (Urgency/Pressure): +30.\n");
        }

        // Rule 11: Suspicious Domain Warning Signals (+20, MEDIUM)
        if (evidenceList != null) {
            for (Evidence ev : evidenceList) {
                if (ev.getSourceDomain() != null && SUSPICIOUS_DOMAIN_TLDS.matcher(ev.getSourceDomain()).find()) {
                    violations.add(new RiskRuleViolation(
                            "RULE_11_SUSPICIOUS_DOMAIN",
                            "Suspicious Domain Extension Encountered",
                            "DOMAIN_REPUTATION",
                            20,
                            "MEDIUM",
                            "Encountered unusual top-level domain (" + ev.getSourceDomain() + ") commonly associated with disposable redirect schemes.",
                            List.of(ev.getSourceDomain())
                    ));
                    score += 20;
                    breakdown.append("Triggered Rule 11 (Suspicious TLD ").append(ev.getSourceDomain()).append("): +20.\n");
                    break;
                }
            }
        }

        // Rule 12: Informal / Peer-to-Peer Payment Demanded (UPI) (+35, HIGH)
        boolean rule12Triggered = false;
        boolean hasUpiMention = Pattern.compile("(?i)\\b(upi|gpay|phonepe|paytm|bhim)\\b").matcher(combinedText).find();
        boolean feeKeywordsPresent = violations.stream().anyMatch(v -> "FINANCIAL_DEMAND".equals(v.category()))
                || (listing != null && listing.feeDemanded());
        if (UNOFFICIAL_PAYMENT_PATTERN.matcher(combinedText).find() || (hasUpiMention && feeKeywordsPresent)) {
            List<String> upiMatches = findMatches(UNOFFICIAL_PAYMENT_PATTERN, combinedText);
            if (upiMatches.isEmpty()) {
                upiMatches = List.of("Payment: UPI / P2P Channel");
            }
            violations.add(new RiskRuleViolation(
                    "RULE_12_UNOFFICIAL_PAYMENT",
                    "Informal / Peer-to-Peer Payment Demanded (UPI)",
                    "PAYMENT_METHOD",
                    35,
                    "HIGH",
                    "The opportunity directs financial transactions through personal or peer-to-peer payment channels (UPI, Google Pay, PhonePe, Paytm, or crypto) rather than verified corporate invoicing.",
                    upiMatches
            ));
            score += 35;
            rule12Triggered = true;
            breakdown.append("Triggered Rule 12 (UPI/P2P Payment Method): +35.\n");
        }

        // Qualitative Gemini AI Risk Signals mapping into deterministic scoring
        if (aiResult != null && !aiResult.fallbackUsed() && aiResult.riskSignals() != null) {
            boolean aiUpfrontDone = false;
            boolean aiTrainingDone = false;
            boolean aiDepositDone = false;
            boolean aiUpiDone = false;
            boolean aiGuaranteedDone = false;
            boolean aiUrgencyDone = false;
            boolean aiScamRecordDone = false;

            for (String rawSignal : aiResult.riskSignals()) {
                if (rawSignal == null || rawSignal.isBlank()) continue;
                String lower = rawSignal.toLowerCase(Locale.ROOT);

                if (containsAny(lower, UPFRONT_FEE_KEYWORDS)) {
                    if (!rule01Triggered && !aiUpfrontDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_UPFRONT_FEE",
                                "AI Flagged: Upfront Registration Fee Demanded",
                                "FINANCIAL_DEMAND",
                                45,
                                "CRITICAL",
                                "Gemini AI identified upfront fee demands: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 45;
                        aiUpfrontDone = true;
                        breakdown.append("Triggered AI Signal (Upfront Fee): +45.\n");
                    } else {
                        breakdown.append("AI Signal (Upfront Fee) already counted in deterministic rule evaluation.\n");
                    }
                } else if (containsAny(lower, PAID_TRAINING_CERT_KEYWORDS)) {
                    if (!rule02Triggered && !aiTrainingDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_PAID_TRAINING",
                                "AI Flagged: Mandatory Training / Certification Fee",
                                "FINANCIAL_DEMAND",
                                45,
                                "CRITICAL",
                                "Gemini AI identified paid certification/training demands: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 45;
                        aiTrainingDone = true;
                        breakdown.append("Triggered AI Signal (Paid Certification): +45.\n");
                    } else {
                        breakdown.append("AI Signal (Paid Training/Cert) already counted in deterministic rule evaluation.\n");
                    }
                } else if (containsAny(lower, SECURITY_DEPOSIT_KEYWORDS)) {
                    if (!rule03Triggered && !aiDepositDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_SECURITY_DEPOSIT",
                                "AI Flagged: Mandatory Caution Deposit / Equipment Fee",
                                "FINANCIAL_DEMAND",
                                40,
                                "CRITICAL",
                                "Gemini AI identified security deposit or equipment fees: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 40;
                        aiDepositDone = true;
                        breakdown.append("Triggered AI Signal (Security Deposit): +40.\n");
                    } else {
                        breakdown.append("AI Signal (Security Deposit) already counted in deterministic rule evaluation.\n");
                    }
                } else if (containsAny(lower, SUSPICIOUS_PAYMENT_KEYWORDS)) {
                    if (!rule12Triggered && !aiUpiDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_SUSPICIOUS_PAYMENT",
                                "AI Flagged: Informal Payment Method (UPI/Wallet)",
                                "PAYMENT_METHOD",
                                35,
                                "HIGH",
                                "Gemini AI identified informal/P2P payment channels: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 35;
                        aiUpiDone = true;
                        breakdown.append("Triggered AI Signal (Suspicious Payment): +35.\n");
                    } else {
                        breakdown.append("AI Signal (Suspicious Payment) already counted in deterministic rule evaluation.\n");
                    }
                } else if (containsAny(lower, GUARANTEED_JOB_KEYWORDS)) {
                    if (!rule07Triggered && !aiGuaranteedDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_GUARANTEED_JOB",
                                "AI Flagged: Guaranteed Selection / Placement Claim",
                                "RECRUITMENT_PROCESS",
                                25,
                                "HIGH",
                                "Gemini AI identified guaranteed job claims without assessment: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 25;
                        aiGuaranteedDone = true;
                        breakdown.append("Triggered AI Signal (Guaranteed Job): +25.\n");
                    } else {
                        breakdown.append("AI Signal (Guaranteed Job) already counted in deterministic rule evaluation.\n");
                    }
                } else if (containsAny(lower, URGENCY_PRESSURE_KEYWORDS)) {
                    if (!rule10Triggered && !aiUrgencyDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_URGENCY",
                                "AI Flagged: Artificial Urgency & Seat Pressure",
                                "PSYCHOLOGICAL_PRESSURE",
                                30,
                                "HIGH",
                                "Gemini AI identified artificial urgency or pressure tactics: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 30;
                        aiUrgencyDone = true;
                        breakdown.append("Triggered AI Signal (Urgency): +30.\n");
                    } else {
                        breakdown.append("AI Signal (Urgency) already counted in deterministic rule evaluation.\n");
                    }
                } else if (containsAny(lower, SCAM_RECORD_KEYWORDS)) {
                    if (!aiScamRecordDone) {
                        violations.add(new RiskRuleViolation(
                                "AI_RULE_SCAM_HISTORY",
                                "AI Flagged: Prior Scam Allegations or Unregistered Entity",
                                "REPUTATION",
                                25,
                                "HIGH",
                                "Gemini AI noted prior scam allegations or missing corporate identity: " + rawSignal,
                                List.of(rawSignal)
                        ));
                        score += 25;
                        aiScamRecordDone = true;
                        breakdown.append("Triggered AI Signal (Scam History/Record): +25.\n");
                    }
                } else {
                    breakdown.append("Unrecognized AI signal omitted from score impact: ").append(rawSignal).append("\n");
                }
            }
        }

        // Mitigating Factors (Positive Signals from Octen Evidence)
        if (evidenceList != null) {
            boolean hasOfficialRegistry = evidenceList.stream()
                    .anyMatch(e -> e.getCategory() == EvidenceCategory.OFFICIAL_REGISTRY);
            if (hasOfficialRegistry) {
                positiveSignals.add("Verified corporate filing identified on official government registrar (MCA / ZaubaCorp / SEC).");
                score -= 15;
                breakdown.append("Mitigating Signal (Official Registry): -15.\n");
            }

            boolean hasOfficialSource = evidenceList.stream()
                    .anyMatch(e -> e.getCategory() == EvidenceCategory.OFFICIAL_SOURCE);
            if (hasOfficialSource) {
                positiveSignals.add("Official corporate careers portal and contact information located.");
                score -= 15;
                breakdown.append("Mitigating Signal (Official Careers Portal): -15.\n");
            }

            boolean hasJobBoard = evidenceList.stream()
                    .anyMatch(e -> e.getCategory() == EvidenceCategory.JOB_BOARD);
            if (hasJobBoard) {
                positiveSignals.add("Opportunity listed through reputable verified job boards.");
                score -= 5;
                breakdown.append("Mitigating Signal (Established Job Board): -5.\n");
            }
        }

        // Mitigating Factors from Gemini AI
        if (aiResult != null && !aiResult.fallbackUsed() && aiResult.positiveSignals() != null 
                && !aiResult.positiveSignals().isEmpty() && violations.isEmpty()) {
            positiveSignals.addAll(aiResult.positiveSignals());
            score -= 10;
            breakdown.append("Mitigating Signal (Gemini AI Positive Verification): -10.\n");
        }

        // Clamp score between 0 and 100
        // When genuine corporate credentials are confirmed and 0 violations occur, score reaches 0.
        // When high-risk communication or impersonation flags are tripped (e.g. Telegram recruitment, free webmail),
        // genuine corporate registry records cannot reduce risk below MODERATE vigilance (min 30).
        int minScore = violations.isEmpty()
                ? 0
                : (violations.stream().anyMatch(v -> "HIGH".equals(v.severity()) || "CRITICAL".equals(v.severity())) ? 30 : 10);
        score = Math.max(minScore, Math.min(100, score));

        // Determine Level and Recommendation
        RiskRecommendation recommendation;
        String riskLevel;
        String explanation;

        if (score >= 56 || violations.stream().anyMatch(v -> "CRITICAL".equals(v.severity()))) {
            recommendation = RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED;
            riskLevel = score >= 75 ? "CRITICAL" : "HIGH";
            explanation = "Critical risk indicators detected (e.g., fee requests or impersonation signs). Do not transfer funds or share sensitive credentials until officially verified.";
        } else if (score >= 26) {
            recommendation = RiskRecommendation.INVESTIGATE_FURTHER;
            riskLevel = "MODERATE";
            explanation = "Elevated risk signals or unconfirmed recruiter communication detected. Inquire directly through verified company channels before proceeding.";
        } else {
            recommendation = RiskRecommendation.PROCEED_WITH_NORMAL_PRECAUTIONS;
            riskLevel = "LOW";
            explanation = "No high-risk scam indicators triggered. Proceed with normal precautions standard for any student employment offer.";
        }

        breakdown.append("Final calculated score: ").append(score).append("/100 (").append(riskLevel).append(").\n");

        return new RiskAssessmentResult(
                score,
                riskLevel,
                recommendation,
                explanation,
                violations,
                positiveSignals,
                true,
                breakdown.toString()
        );
    }

    private boolean containsAny(String text, String[] keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String buildCombinedCorpus(ExtractedInternshipDetails listing, List<Evidence> evidenceList) {
        StringBuilder sb = new StringBuilder();
        boolean hasListingContent = listing != null && !listing.unreachable() 
                && listing.rawContent() != null && !listing.rawContent().isBlank();

        if (hasListingContent) {
            sb.append(listing.rawContent()).append("\n");
        }

        if (evidenceList != null) {
            for (Evidence ev : evidenceList) {
                // Strictly only include evidence categorized as COMMUNITY_REPORT (e.g. direct student scam complaints).
                // Benign web searches, news articles, official portals, and awareness guides must NEVER be included
                // in the listing corpus to avoid false positives from general job scam articles.
                if (ev.getCategory() == EvidenceCategory.COMMUNITY_REPORT) {
                    if (ev.getTitle() != null) sb.append(ev.getTitle()).append(" ");
                    if (ev.getDescription() != null) sb.append(ev.getDescription()).append(" ");
                }
            }
        }
        return sb.toString();
    }

    private List<String> findMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String hit = matcher.group().trim();
            if (!matches.contains(hit)) {
                matches.add(hit);
                if (matches.size() >= 3) break;
            }
        }
        return matches;
    }

    private String extractDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        int atIdx = email.lastIndexOf('@');
        return email.substring(atIdx + 1).trim();
    }
}
