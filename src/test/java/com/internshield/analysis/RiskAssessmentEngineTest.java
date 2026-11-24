package com.internshield.analysis;

import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceCategory;
import com.internshield.internship.dto.ExtractedInternshipDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskAssessmentEngineTest {

    private RiskAssessmentEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskAssessmentEngine();
    }

    private ExtractedInternshipDetails createCleanListing(String company) {
        return new ExtractedInternshipDetails(
                "Software Engineering Intern",
                company,
                "Standard engineering internship building backend services with Java Spring Boot.",
                "Software Engineering Intern",
                "₹20,000 per month",
                List.of("Write clean code", "Write unit tests"),
                "recruiter@" + company.toLowerCase().replaceAll("[^a-z0-9]", "") + ".com",
                "+91-9876543210",
                "STANDARD_WEB",
                List.of(),
                false,
                false,
                null
        );
    }

    private List<Evidence> createCleanEvidence(String company) {
        Evidence registry = new Evidence();
        registry.setTitle(company + " Official Registration");
        registry.setSourceDomain(company.toLowerCase().replaceAll("[^a-z0-9]", "") + ".com");
        registry.setCategory(EvidenceCategory.OFFICIAL_REGISTRY);
        registry.setDescription("Active registered corporate entity on MCA.");

        Evidence careers = new Evidence();
        careers.setTitle(company + " Careers");
        careers.setSourceDomain(company.toLowerCase().replaceAll("[^a-z0-9]", "") + ".com");
        careers.setCategory(EvidenceCategory.OFFICIAL_SOURCE);
        careers.setDescription("Official careers portal with verified job openings.");

        return List.of(registry, careers);
    }

    // ==========================================
    // 1. TEST ALL 11 RULES INDIVIDUALLY
    // ==========================================

    @Test
    @DisplayName("Rule 01: Registration fee demanded (+45, CRITICAL)")
    void testRule01_registrationFee() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "Acme", "Selected candidate must pay a registration fee of Rs 2000 to confirm slot.",
                "Intern", "Unpaid", List.of(), "", "", "STANDARD_WEB", List.of("registration fee"), true, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("Acme", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_01_REG_FEE".equals(r.ruleId())));
        assertEquals(RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED, result.recommendation());
        assertEquals(55, result.riskScore()); // 10 baseline + 45
    }

    @Test
    @DisplayName("Rule 02: Training fee demanded (+45, CRITICAL)")
    void testRule02_trainingFee() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "Beta Corp", "Candidates must pay for training charges before start date.",
                "Intern", "₹10,000", List.of(), "", "", "STANDARD_WEB", List.of(), true, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("Beta Corp", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_02_TRAINING_FEE".equals(r.ruleId())));
        assertEquals(RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED, result.recommendation());
        assertEquals(55, result.riskScore());
    }

    @Test
    @DisplayName("Rule 03: Security deposit / payment after advertising free (+40, CRITICAL)")
    void testRule03_securityDeposit() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "Gamma Inc", "Interns must submit a refundable caution deposit of Rs 5000.",
                "Intern", "₹5,000", List.of(), "", "", "STANDARD_WEB", List.of(), true, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("Gamma Inc", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_03_SECURITY_DEPOSIT".equals(r.ruleId())));
        assertEquals(RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED, result.recommendation());
        assertEquals(50, result.riskScore()); // 10 baseline + 40
    }

    @Test
    @DisplayName("Rule 04: Free public recruiter email (+25, HIGH)")
    void testRule04_publicEmail() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "Delta Corp", "Standard internship.", "Intern", "₹15,000", List.of(),
                "delta.recruiter@gmail.com", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("Delta Corp", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_04_PUBLIC_EMAIL".equals(r.ruleId())));
        assertEquals(35, result.riskScore()); // 10 baseline + 25
        assertEquals(RiskRecommendation.INVESTIGATE_FURTHER, result.recommendation());
    }

    @Test
    @DisplayName("Rule 05: Email domain mismatch with company identity (+25, HIGH)")
    void testRule05_domainMismatch() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "Microsoft", "Join Microsoft as an intern.", "Intern", "₹50,000", List.of(),
                "hr@quickhiringagency.org", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("Microsoft", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_05_DOMAIN_MISMATCH".equals(r.ruleId())));
        assertEquals(35, result.riskScore());
        assertEquals(RiskRecommendation.INVESTIGATE_FURTHER, result.recommendation());
    }

    @Test
    @DisplayName("Rule 06: WhatsApp/Telegram-only recruitment (+30, HIGH)")
    void testRule06_messagingOnly() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "Zeta Tech", "Apply on Telegram.", "Intern", "₹10,000", List.of(),
                "", "", "TELEGRAM", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("Zeta Tech", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_06_MESSAGING_ONLY".equals(r.ruleId())));
        assertEquals(40, result.riskScore()); // 10 baseline + 30
        assertEquals(RiskRecommendation.INVESTIGATE_FURTHER, result.recommendation());
    }

    @Test
    @DisplayName("Rule 07: Guaranteed selection without evaluation (+25, HIGH)")
    void testRule07_guaranteedSelection() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "FastTrack", "Guaranteed selection and immediate offer letter without interview.",
                "Intern", "₹15,000", List.of(), "", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("FastTrack", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_07_GUARANTEED_SELECTION".equals(r.ruleId())));
        assertEquals(35, result.riskScore());
    }

    @Test
    @DisplayName("Rule 08: Unclear / vague responsibilities (+15, MEDIUM)")
    void testRule08_vagueDuties() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "DataTyping", "Simple copy paste and fill captchas. No skills required.",
                "Intern", "₹12,000", List.of(), "", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("DataTyping", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_08_VAGUE_DUTIES".equals(r.ruleId())));
        assertEquals(25, result.riskScore()); // 10 + 15
    }

    @Test
    @DisplayName("Rule 09: Unrealistic stipend (+20, MEDIUM)")
    void testRule09_unrealisticStipend() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "DreamGig", "Work 2 hours a day. Stipend: Rs 90000 per month.",
                "Intern", "₹90,000", List.of(), "", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("DreamGig", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_09_UNREALISTIC_STIPEND".equals(r.ruleId())));
        assertEquals(30, result.riskScore()); // 10 + 20
    }

    @Test
    @DisplayName("Rule 10: Artificial urgency or pressure (+30, HIGH)")
    void testRule10_artificialUrgency() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "UrgentHiring", "Offer expires in 2 hours! Immediate payment required to secure seat.",
                "Intern", "₹10,000", List.of(), "", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("UrgentHiring", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_10_ARTIFICIAL_URGENCY".equals(r.ruleId())));
        assertEquals(40, result.riskScore()); // 10 + 30
    }

    @Test
    @DisplayName("Rule 11: Suspicious domain TLD encountered (+20, MEDIUM)")
    void testRule11_suspiciousDomainTld() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "ShadyPortal", "Apply for internship.", "Intern", "₹10,000", List.of(),
                "", "", "STANDARD_WEB", List.of(), false, false, null);

        Evidence ev = new Evidence();
        ev.setTitle("Shady portal careers");
        ev.setSourceDomain("internships-direct.xyz");
        ev.setCategory(EvidenceCategory.OTHER);

        RiskAssessmentResult result = engine.evaluateRisk("ShadyPortal", listing, List.of(ev));

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_11_SUSPICIOUS_DOMAIN".equals(r.ruleId())));
        assertEquals(30, result.riskScore()); // 10 + 20
    }

    // ==========================================
    // 2. MITIGATING SIGNALS AND SCORE BOUNDARIES
    // ==========================================

    @Test
    @DisplayName("Mitigating signals reduce score, clamped to min 10")
    void testMitigatingSignalsAndClamping() {
        ExtractedInternshipDetails cleanListing = createCleanListing("Infosys");
        List<Evidence> cleanEvidence = createCleanEvidence("Infosys");

        // Add Job Board evidence (-5)
        Evidence jobBoard = new Evidence();
        jobBoard.setTitle("Infosys on Naukri");
        jobBoard.setCategory(EvidenceCategory.JOB_BOARD);
        List<Evidence> allEvidence = new ArrayList<>(cleanEvidence);
        allEvidence.add(jobBoard);

        RiskAssessmentResult result = engine.evaluateRisk("Infosys", cleanListing, allEvidence);

        // Baseline 10 - 15 (Registry) - 15 (Official Source) - 5 (Job Board) = -25 -> Clamped to 10
        assertEquals(10, result.riskScore());
        assertEquals("LOW", result.riskLevel());
        assertEquals(RiskRecommendation.PROCEED_WITH_NORMAL_PRECAUTIONS, result.recommendation());
        assertEquals(3, result.positiveSignals().size());
        assertTrue(result.triggeredRules().isEmpty());
    }

    @Test
    @DisplayName("Multiple high-risk violations clamped to max 100")
    void testMaximumScoreClamping() {
        // Triggers Rule 1 (+45), Rule 2 (+45), Rule 3 (+40), Rule 10 (+30) -> Total 170 -> Clamped to 100
        String text = "Pay registration fee of Rs 2000. Mandatory training fee applies. Refundable caution deposit needed. Immediate payment offer expires in 1 hour!";
        ExtractedInternshipDetails extremeScam = new ExtractedInternshipDetails(
                "Intern", "MegaScam", text, "Intern", "₹100,000", List.of(), "", "", "STANDARD_WEB", List.of(), true, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("MegaScam", extremeScam, List.of());

        assertEquals(100, result.riskScore());
        assertEquals("CRITICAL", result.riskLevel());
        assertEquals(RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED, result.recommendation());
    }

    // ==========================================
    // 3. INSUFFICIENT EVIDENCE HANDLING
    // ==========================================

    @Test
    @DisplayName("Should return INSUFFICIENT_EVIDENCE when listing is empty and <= 1 evidence item found")
    void testInsufficientEvidence_unreachableListingAndSparseEvidence() {
        ExtractedInternshipDetails unreachableListing = ExtractedInternshipDetails.unreachable("DNS lookup failed");
        List<Evidence> sparseEvidence = List.of();

        RiskAssessmentResult result = engine.evaluateRisk("GhostCorp", unreachableListing, sparseEvidence);

        assertEquals(50, result.riskScore());
        assertEquals(RiskRecommendation.INSUFFICIENT_EVIDENCE, result.recommendation());
        assertFalse(result.sufficientEvidence()); // Flag indicates lack of conclusive evidence
        assertTrue(result.triggeredRules().isEmpty());
    }

    // ==========================================
    // 4. FAIRNESS RULES: MISSING INFO != FRAUD, REVIEWS != FRAUD
    // ==========================================

    @Test
    @DisplayName("Missing optional information must NEVER be treated as proof of fraud")
    void testMissingInfoNotTreatedAsFraud() {
        // Listing lacks stipend, phone, and specific responsibilities
        ExtractedInternshipDetails minimalListing = new ExtractedInternshipDetails(
                "Intern", "StartUpCo", "Internship opportunity at StartUpCo.",
                "Intern", "", List.of(), "contact@startupco.com", "", "STANDARD_WEB", List.of(), false, false, null);

        List<Evidence> evidence = createCleanEvidence("StartUpCo");
        RiskAssessmentResult result = engine.evaluateRisk("StartUpCo", minimalListing, evidence);

        // Missing fields must not trigger any rules
        assertTrue(result.triggeredRules().isEmpty());
        assertEquals(10, result.riskScore());
        assertEquals(RiskRecommendation.PROCEED_WITH_NORMAL_PRECAUTIONS, result.recommendation());
    }

    @Test
    @DisplayName("Employee reviews alone on review platforms do not establish internship fraud")
    void testEmployeeReviewsAloneDoNotTriggerFraud() {
        ExtractedInternshipDetails cleanListing = createCleanListing("TechGiant");

        Evidence reviewEvidence1 = new Evidence();
        reviewEvidence1.setTitle("TechGiant Reviews on Glassdoor");
        reviewEvidence1.setSourceDomain("glassdoor.com");
        reviewEvidence1.setCategory(EvidenceCategory.REVIEW_PLATFORM);
        reviewEvidence1.setDescription("Work-life balance is challenging. Long hours and strict deadlines.");

        Evidence reviewEvidence2 = new Evidence();
        reviewEvidence2.setTitle("TechGiant Reviews on AmbitionBox");
        reviewEvidence2.setSourceDomain("ambitionbox.com");
        reviewEvidence2.setCategory(EvidenceCategory.REVIEW_PLATFORM);
        reviewEvidence2.setDescription("Average management, good learning opportunity for freshers.");

        RiskAssessmentResult result = engine.evaluateRisk("TechGiant", cleanListing, List.of(reviewEvidence1, reviewEvidence2));

        // Workplace complaints must NOT trigger scam rules
        assertTrue(result.triggeredRules().isEmpty());
        assertEquals(10, result.riskScore());
        assertEquals(RiskRecommendation.PROCEED_WITH_NORMAL_PRECAUTIONS, result.recommendation());
    }
}
