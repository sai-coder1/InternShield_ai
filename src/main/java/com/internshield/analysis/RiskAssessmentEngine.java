package com.internshield.analysis;

import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceCategory;
import com.internshield.internship.dto.ExtractedInternshipDetails;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RiskAssessmentEngine {

    // Regex patterns for the 11 checks
    private static final Pattern REGISTRATION_FEE_PATTERN = Pattern.compile(
            "(?i)\\b(registration fee|registration charges|pay to register|form fee|application fee of rs|enrollment fee)\\b");

    private static final Pattern TRAINING_FEE_PATTERN = Pattern.compile(
            "(?i)\\b(training fee|training charges|pay for training|mandatory course fee|onboarding charges|certification fee to start)\\b");

    private static final Pattern PAYMENT_AFTER_FREE_PATTERN = Pattern.compile(
            "(?i)\\b(security deposit|refundable caution deposit|refundable amount|pay to confirm seat|laptop deposit|equipment charges)\\b");

    private static final Pattern PUBLIC_EMAIL_DOMAINS = Pattern.compile(
            "(?i)@(gmail|yahoo|hotmail|outlook|rediffmail|protonmail|aol|icloud)\\.com");

    private static final Pattern TELEGRAM_WHATSAPP_PATTERN = Pattern.compile(
            "(?i)\\b(telegram|t\\.me\\/|whatsapp|wa\\.me\\/|chat group)\\b");

    private static final Pattern GUARANTEED_SELECTION_PATTERN = Pattern.compile(
            "(?i)\\b(guaranteed selection|direct selection|no interview|100% selection|offer letter without interview|immediate offer letter)\\b");

    private static final Pattern UNCLEAR_RESPONSIBILITIES_PATTERN = Pattern.compile(
            "(?i)\\b(simple typing|copy paste|data entry work from home|fill captchas|earn 2000 daily|no skills required)\\b");

    private static final Pattern UNREALISTIC_STIPEND_PATTERN = Pattern.compile(
            "(?i)\\b(stipend|salary)[:\\s]*(?:rs\\.?|inr|₹)?\\s*(?:[5-9]\\d{4}|[1-9]\\d{5})\\s*(?:per month|/month|p\\.m\\.)\\b");

    private static final Pattern ARTIFICIAL_URGENCY_PATTERN = Pattern.compile(
            "(?i)\\b(pay within|urgent requirement|offer expires in|valid for today only|last \\d+ seats|immediate payment)\\b");

    private static final Pattern SUSPICIOUS_DOMAIN_TLDS = Pattern.compile(
            "(?i)\\.(xyz|top|buzz|click|club|rest|link|cam|work|gq|cf|tk|ml)$");

    /**
     * Executes deterministic rule evaluation across extracted listing details and collected research evidence.
     *
     * @param companyName company under review
     * @param listing extracted details from URL or pasted text
     * @param evidenceList persisted research evidence items
     * @return comprehensive risk assessment result with transparent scoring
     */
    public RiskAssessmentResult evaluateRisk(
            String companyName,
            ExtractedInternshipDetails listing,
            List<Evidence> evidenceList) {

        List<RiskRuleViolation> violations = new ArrayList<>();
        List<String> positiveSignals = new ArrayList<>();

        int score = 10; // Baseline vigilance score (never 0)
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Baseline vigilance score: +10.\n");

        String combinedText = buildCombinedCorpus(listing, evidenceList);
        String lowerText = combinedText.toLowerCase(Locale.ROOT);

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

        // Rule 1: Registration fee requests (+45, CRITICAL)
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
            breakdown.append("Triggered Rule 01 (Registration Fee): +45.\n");
        }

        // Rule 2: Training or onboarding fee demands (+45, CRITICAL)
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
            breakdown.append("Triggered Rule 02 (Training Fee): +45.\n");
        }

        // Rule 3: Payment after advertising free / Security Deposit (+40, CRITICAL)
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
                String cleanComp = companyName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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

        // Mitigating Factors (Positive Signals)
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

        // Clamp score between 10 and 100
        score = Math.max(10, Math.min(100, score));

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

    private String buildCombinedCorpus(ExtractedInternshipDetails listing, List<Evidence> evidenceList) {
        StringBuilder sb = new StringBuilder();
        if (listing != null && !listing.unreachable() && listing.rawContent() != null) {
            sb.append(listing.rawContent()).append("\n");
        }
        if (evidenceList != null) {
            for (Evidence ev : evidenceList) {
                if (ev.getTitle() != null) sb.append(ev.getTitle()).append(" ");
                if (ev.getDescription() != null) sb.append(ev.getDescription()).append(" ");
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
