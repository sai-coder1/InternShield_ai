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

        // Baseline 10 - 15 (Registry) - 15 (Official Source) - 5 (Job Board) = -25 -> Clamped to 0 (genuine corporate credentials verified)
        assertEquals(0, result.riskScore());
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

        // Missing fields must not trigger any rules; corporate records reduce score to 0
        assertTrue(result.triggeredRules().isEmpty());
        assertEquals(0, result.riskScore());
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

    // ==========================================
    // 5. RULE 12, AI INTEGRATION & USER SCAM CASES
    // ==========================================

    @Test
    @DisplayName("Rule 12: Informal / UPI payment method demanded (+35, HIGH)")
    void testRule12_unofficialPaymentUpi() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "FintechScam", "Pay registration deposit via Payment: UPI to hr@upi.",
                "Intern", "₹10,000", List.of(), "", "", "STANDARD_WEB", List.of(), false, false, null);

        RiskAssessmentResult result = engine.evaluateRisk("FintechScam", listing, List.of());

        assertTrue(result.triggeredRules().stream().anyMatch(r -> "RULE_12_UNOFFICIAL_PAYMENT".equals(r.ruleId())));
        assertEquals(45, result.riskScore()); // 10 baseline + 35
    }

    @Test
    @DisplayName("Gemini AI qualitative risk signals contribute to backend score deterministically")
    void testRuleAI_geminiQualitativeRiskSignals() {
        ExtractedInternshipDetails cleanListing = createCleanListing("SuspiciousCo");
        com.internshield.ai.AIAnalysisResult aiResult = new com.internshield.ai.AIAnalysisResult(
                "SuspiciousCo",
                "Multiple inconsistencies found.",
                List.of(),
                List.of("Company claims to be registered but has no corporate registrar records", "Recruiter phone number associated with previous scam reports"),
                List.of(),
                List.of(),
                "Disclaimer",
                false,
                "{}"
        );

        RiskAssessmentResult result = engine.evaluateRisk("SuspiciousCo", cleanListing, List.of(), aiResult);

        assertTrue(result.triggeredRules().stream().anyMatch(r -> r.ruleId().startsWith("AI_RULE_")));
        assertEquals(35, result.riskScore()); // 10 baseline + 25 (scam history rule)
    }

    @Test
    @DisplayName("Case A: Normal Internship - No fee, official application, interview, realistic stipend")
    void testCaseA_normalInternship() {
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Software Engineer Intern",
                "Infosys",
                "Software engineering internship. Apply via our official careers portal. Candidates must clear 2 technical interview rounds and coding test. Stipend: Rs 25,000 per month. Official position with zero fees.",
                "Software Engineer Intern",
                "₹25,000/month",
                List.of("Develop REST APIs", "Write automated tests"),
                "recruiter@infosys.com",
                "+91-80-28520261",
                "STANDARD_WEB",
                List.of(),
                false,
                false,
                null
        );

        List<Evidence> evidence = createCleanEvidence("Infosys");

        com.internshield.ai.AIAnalysisResult aiResult = new com.internshield.ai.AIAnalysisResult(
                "Infosys",
                "Legitimate corporate internship listing from verified enterprise employer.",
                List.of("Official corporate website and careers portal verified", "Standard technical interview evaluation process", "No application or onboarding fee requested"),
                List.of(),
                List.of("Review platform feedback general in nature"),
                List.of("https://infosys.com/careers"),
                "Disclaimer",
                false,
                "{}"
        );

        RiskAssessmentResult result = engine.evaluateRisk("Infosys Ltd", listing, evidence, aiResult);

        assertTrue(result.triggeredRules().isEmpty(), "Normal internship should trigger 0 risk rules");
        assertEquals(0, result.riskScore(), "Normal verified internship should score 0% risk (not defaulted to 10)");
        assertEquals("LOW", result.riskLevel());
        assertEquals(RiskRecommendation.PROCEED_WITH_NORMAL_PRECAUTIONS, result.recommendation());
        assertFalse(result.positiveSignals().isEmpty());
    }

    @Test
    @DisplayName("Case B: CareerBoost - ₹2,999 certification fee + guaranteed job + UPI + limited seats")
    void testCaseB_careerBoost() {
        String description = """
                Company: CareerBoost Pvt Ltd
                Role: Full Stack Developer Intern
                Stipend: ₹20,000/month
                Special Offer: Complete our ₹2,999 certification program and you are 100% guaranteed a job after the internship.
                Payment: UPI
                Limited seats: 7 remaining.
                """;

        com.internshield.internship.InternshipListingExtractor extractor =
                new com.internshield.internship.InternshipListingExtractor(new com.internshield.internship.SsrfProtectionValidator());
        ExtractedInternshipDetails extracted = extractor.extract(null, description, "CareerBoost Pvt Ltd");

        com.internshield.ai.AIAnalysisResult aiResult = new com.internshield.ai.AIAnalysisResult(
                "CareerBoost Pvt Ltd",
                "Critical scam risk detected: upfront fee demanded for mandatory certification, guaranteed job placement claim, informal UPI payment channel, and artificial urgency pressure.",
                List.of(),
                List.of(
                        "Paid certification fee of ₹2,999 demanded to secure internship",
                        "Guaranteed job offered without evaluation",
                        "Payment demanded via UPI informal channel",
                        "Artificial urgency with limited seats remaining"
                ),
                List.of("Entity has no registered MCA corporate filings"),
                List.of(),
                "Disclaimer",
                false,
                "{}"
        );

        RiskAssessmentResult result = engine.evaluateRisk("CareerBoost Pvt Ltd", extracted, List.of(), aiResult);

        // Verify all 4 expected scam indicators are detected by the engine
        boolean detectedFeeOrCert = result.triggeredRules().stream()
                .anyMatch(r -> r.ruleId().contains("TRAINING") || r.ruleId().contains("CERT") || r.ruleId().contains("FEE"));
        boolean detectedGuaranteed = result.triggeredRules().stream()
                .anyMatch(r -> r.ruleId().contains("GUARANTEED"));
        boolean detectedUpi = result.triggeredRules().stream()
                .anyMatch(r -> r.ruleId().contains("PAYMENT") || r.ruleId().contains("UNOFFICIAL"));
        boolean detectedUrgency = result.triggeredRules().stream()
                .anyMatch(r -> r.ruleId().contains("URGENCY"));

        assertTrue(detectedFeeOrCert, "Must flag paid training/certification fee");
        assertTrue(detectedGuaranteed, "Must flag guaranteed job claim");
        assertTrue(detectedUpi, "Must flag UPI payment method");
        assertTrue(detectedUrgency, "Must flag urgency/limited seats");

        assertTrue(result.riskScore() >= 80, "CareerBoost score must be critical scam risk, got: " + result.riskScore());
        assertEquals("CRITICAL", result.riskLevel());
        assertEquals(RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED, result.recommendation());
    }

    @Test
    @DisplayName("Case C: Suspicious internship with non-regex wording caught via Gemini AI mapping")
    void testCaseC_suspiciousInternshipDifferentWording() {
        // Phrasing intentionally crafted so traditional exact regex patterns (e.g. 'registration fee', 'pay for training', 'whatsapp', 't.me') do NOT match
        String nonRegexDescription = """
                Company: NexaSkills Innovations
                Role: Cloud Trainee Associate
                Description: Candidates must remit an initial candidate onboarding kit charge of Rs 1,500 prior to joining.
                All selected applicants are assured an appointment with zero skill screening or testing.
                Submit payment to personal phone wallet before enrollment ends tomorrow morning.
                """;

        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Cloud Trainee Associate",
                "NexaSkills Innovations",
                nonRegexDescription,
                "Cloud Trainee Associate",
                "",
                List.of(),
                "support@nexaskills.org",
                "",
                "STANDARD_WEB",
                List.of(),
                false,
                false,
                null
        );

        // Gemini AI understands natural language semantics and produces recognized structured risk signals
        com.internshield.ai.AIAnalysisResult aiResult = new com.internshield.ai.AIAnalysisResult(
                "NexaSkills Innovations",
                "Severe recruitment scam indicators: upfront onboarding kit charge, assured appointment without screening, and phone wallet payment.",
                List.of(),
                List.of(
                        "Upfront candidate onboarding kit charge of Rs 1,500 required before start",
                        "Direct appointment promised with zero screening or technical evaluation",
                        "Informal payment requested via personal phone wallet",
                        "Urgency pressure with immediate enrollment deadline"
                ),
                List.of("No MCA corporate registration found"),
                List.of(),
                "Disclaimer",
                false,
                "{}"
        );

        RiskAssessmentResult result = engine.evaluateRisk("NexaSkills Innovations", listing, List.of(), aiResult);

        // Verify that Gemini signals reached the engine and were deterministically converted into rule violations
        assertTrue(result.triggeredRules().stream().anyMatch(r -> "AI_RULE_UPFRONT_FEE".equals(r.ruleId())),
                "AI upfront fee rule must trigger");
        assertTrue(result.triggeredRules().stream().anyMatch(r -> "AI_RULE_GUARANTEED_JOB".equals(r.ruleId())),
                "AI guaranteed job rule must trigger");
        assertTrue(result.triggeredRules().stream().anyMatch(r -> "AI_RULE_SUSPICIOUS_PAYMENT".equals(r.ruleId())),
                "AI suspicious payment rule must trigger");
        assertTrue(result.triggeredRules().stream().anyMatch(r -> "AI_RULE_URGENCY".equals(r.ruleId())),
                "AI urgency rule must trigger");

        assertTrue(result.riskScore() >= 80, "Case C score must be >= 80, got: " + result.riskScore());
        assertEquals("CRITICAL", result.riskLevel());
        assertEquals(RiskRecommendation.DO_NOT_PROCEED_UNTIL_VERIFIED, result.recommendation());
        assertTrue(result.scoringBreakdown().contains("Triggered AI Signal"));
    }

    @Test
    @DisplayName("Gemini and Regex deduplication: double counting prevented for identical scam signal")
    void testDeduplication_regexAndGeminiDoNotDoubleCount() {
        // Regex triggers RULE_01_REG_FEE (+45)
        ExtractedInternshipDetails listing = new ExtractedInternshipDetails(
                "Intern", "ScamCo", "Pay registration fee of Rs 2000.",
                "Intern", "₹10,000", List.of(), "", "", "STANDARD_WEB", List.of(), true, false, null);

        // Gemini ALSO reports the registration fee
        com.internshield.ai.AIAnalysisResult aiResult = new com.internshield.ai.AIAnalysisResult(
                "ScamCo",
                "Upfront registration fee detected.",
                List.of(),
                List.of("Upfront registration fee demanded"),
                List.of(),
                List.of(),
                "Disclaimer",
                false,
                "{}"
        );

        RiskAssessmentResult result = engine.evaluateRisk("ScamCo", listing, List.of(), aiResult);

        // Score should be 10 baseline + 45 (from regex) = 55, NOT 10 + 45 + 45 = 100!
        assertEquals(55, result.riskScore(), "Fee should be counted exactly once without double counting");
        assertTrue(result.scoringBreakdown().contains("already counted"));
    }

    @Test
    @DisplayName("Arbitrary unrecognized Gemini text adds 0 points to risk score")
    void testArbitraryAiTextDoesNotAddPoints() {
        ExtractedInternshipDetails cleanListing = createCleanListing("NormalCo");

        // Gemini returns non-scam arbitrary commentary
        com.internshield.ai.AIAnalysisResult aiResult = new com.internshield.ai.AIAnalysisResult(
                "NormalCo",
                "Office building has only 2 floors and founded recently.",
                List.of(),
                List.of("Office is located in a quiet suburban business park", "Company website was redesigned recently"),
                List.of(),
                List.of(),
                "Disclaimer",
                false,
                "{}"
        );

        RiskAssessmentResult result = engine.evaluateRisk("NormalCo", cleanListing, List.of(), aiResult);

        // Zero rules should trigger for arbitrary text
        assertTrue(result.triggeredRules().isEmpty(), "Arbitrary text must not trigger scam rules");
        assertEquals(10, result.riskScore(), "Arbitrary text must add 0 points");
        assertTrue(result.scoringBreakdown().contains("Unrecognized AI signal omitted"));
    }
}
