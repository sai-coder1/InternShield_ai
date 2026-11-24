package com.internshield.research;

import com.internshield.ai.AIAnalysisResult;
import com.internshield.ai.GeminiAnalysisService;
import com.internshield.analysis.RiskAssessmentEngine;
import com.internshield.analysis.RiskAssessmentResult;
import com.internshield.analysis.RiskRecommendation;
import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceJsonExporter;
import com.internshield.evidence.EvidenceProcessor;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.internship.InternshipListingExtractor;
import com.internshield.internship.SsrfProtectionValidator;
import com.internshield.internship.dto.ExtractedInternshipDetails;
import com.internshield.report.ResearchReport;
import com.internshield.report.ResearchReportRepository;
import com.internshield.report.ResearchService;
import com.internshield.report.ResearchStatus;
import com.internshield.report.dto.AnalysisReportResponse;
import com.internshield.report.dto.AnalysisRequest;
import com.internshield.report.dto.ResearchResponse;
import com.internshield.report.dto.ResearchResultUpdateRequest;
import com.internshield.search.OctenSearchClient;
import com.internshield.search.dto.SearchResultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ResearchOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ResearchOrchestrator.class);

    private final OctenSearchClient octenSearchClient;
    private final CompanyQueryGenerator companyQueryGenerator;
    private final InternshipQueryGenerator internshipQueryGenerator;
    private final SearchResultDeduplicator deduplicator;
    private final ResearchService researchService;

    private final SearchQueryGenerator searchQueryGenerator;
    private final EvidenceProcessor evidenceProcessor;
    private final EvidenceJsonExporter evidenceJsonExporter;
    private final GeminiAnalysisService geminiAnalysisService;
    private final RiskAssessmentEngine riskAssessmentEngine;
    private final InternshipListingExtractor listingExtractor;
    private final ResearchReportRepository researchReportRepository;

    /**
     * Preserved 5-argument constructor for legacy test compatibility.
     */
    public ResearchOrchestrator(
            OctenSearchClient octenSearchClient,
            CompanyQueryGenerator companyQueryGenerator,
            InternshipQueryGenerator internshipQueryGenerator,
            SearchResultDeduplicator deduplicator,
            ResearchService researchService) {
        this(
                octenSearchClient,
                companyQueryGenerator,
                internshipQueryGenerator,
                deduplicator,
                researchService,
                new SearchQueryGenerator(),
                null,
                new EvidenceJsonExporter(),
                null,
                new RiskAssessmentEngine(),
                new InternshipListingExtractor(new SsrfProtectionValidator()),
                null
        );
    }

    /**
     * Primary constructor with all integrated components.
     */
    @Autowired
    public ResearchOrchestrator(
            OctenSearchClient octenSearchClient,
            CompanyQueryGenerator companyQueryGenerator,
            InternshipQueryGenerator internshipQueryGenerator,
            SearchResultDeduplicator deduplicator,
            ResearchService researchService,
            SearchQueryGenerator searchQueryGenerator,
            EvidenceProcessor evidenceProcessor,
            EvidenceJsonExporter evidenceJsonExporter,
            GeminiAnalysisService geminiAnalysisService,
            RiskAssessmentEngine riskAssessmentEngine,
            InternshipListingExtractor listingExtractor,
            ResearchReportRepository researchReportRepository) {
        this.octenSearchClient = octenSearchClient;
        this.companyQueryGenerator = companyQueryGenerator;
        this.internshipQueryGenerator = internshipQueryGenerator;
        this.deduplicator = deduplicator;
        this.researchService = researchService;
        this.searchQueryGenerator = (searchQueryGenerator != null) ? searchQueryGenerator : new SearchQueryGenerator();
        this.evidenceProcessor = evidenceProcessor;
        this.evidenceJsonExporter = (evidenceJsonExporter != null) ? evidenceJsonExporter : new EvidenceJsonExporter();
        this.geminiAnalysisService = geminiAnalysisService;
        this.riskAssessmentEngine = (riskAssessmentEngine != null) ? riskAssessmentEngine : new RiskAssessmentEngine();
        this.listingExtractor = (listingExtractor != null) ? listingExtractor : new InternshipListingExtractor(new SsrfProtectionValidator());
        this.researchReportRepository = researchReportRepository;
    }

    /**
     * Existing conductResearch implementation preserved for Stage 5 backward compatibility.
     */
    public List<SearchResultItem> conductResearch(Long reportId) {
        ResearchResponse report = researchService.getResearchReportById(reportId);

        // Transition to IN_PROGRESS
        researchService.updateResearchStatus(reportId, ResearchStatus.IN_PROGRESS);

        List<String> queries = new ArrayList<>();
        queries.addAll(companyQueryGenerator.generateQueries(report.companyName()));
        queries.addAll(internshipQueryGenerator.generateQueries(report.companyName(), report.internshipUrl()));

        List<SearchResultItem> collected = new ArrayList<>();
        int successfulQueries = 0;

        for (String query : queries) {
            try {
                List<SearchResultItem> results = octenSearchClient.searchAndExtract(query);
                collected.addAll(results);
                successfulQueries++;
            } catch (Exception e) {
                // Allow partial query success
            }
        }

        List<SearchResultItem> uniqueResults = deduplicator.deduplicate(collected);

        if (successfulQueries == 0 && uniqueResults.isEmpty()) {
            researchService.updateResearchStatus(reportId, ResearchStatus.FAILED);
            return List.of();
        }

        StringBuilder findings = new StringBuilder();
        findings.append("Automated research query execution completed. ")
                .append("Queried ").append(queries.size()).append(" terms, ")
                .append("retrieved ").append(uniqueResults.size()).append(" unique sources.\n\n");

        for (int i = 0; i < Math.min(uniqueResults.size(), 5); i++) {
            SearchResultItem item = uniqueResults.get(i);
            findings.append("- [").append(item.source()).append("] ").append(item.title()).append(": ")
                    .append(item.snippet()).append(" (").append(item.url()).append(")\n");
        }

        ResearchResultUpdateRequest updateRequest = new ResearchResultUpdateRequest(
                0,
                "UNKNOWN",
                findings.toString(),
                "Perform manual review of extracted search sources before committing financial deposits or documents.",
                ResearchStatus.COMPLETED
        );

        researchService.updateResearchResults(reportId, updateRequest);
        return uniqueResults;
    }

    /**
     * Complete end-to-end investigation pipeline:
     * User Input -> Listing Extraction -> Query Generation -> Octen Search ->
     * Evidence Processing & Persistence -> Gemini AI Analysis -> Deterministic Risk Engine -> Final Report
     */
    @Transactional
    public AnalysisReportResponse executeCompleteAnalysis(AnalysisRequest request) {
        log.info("Starting complete internship analysis for company: {}", request.companyName());

        // 1. Initialize ResearchReport in database
        ResearchReport report = new ResearchReport(request.companyName(), request.internshipUrl());
        report.setStatus(ResearchStatus.IN_PROGRESS);
        if (researchReportRepository != null) {
            report = researchReportRepository.save(report);
        }

        // 2. Extract Listing Content from URL or Pasted Description
        ExtractedInternshipDetails listingDetails = listingExtractor.extract(
                request.internshipUrl(), request.pastedDescription(), request.companyName());

        // 3. Generate Targeted Search Queries (3-4 queries covering fees, scam allegations, reviews, official info)
        List<String> targetedQueries = searchQueryGenerator.generateTargetedQueries(
                request.companyName(), request.internshipUrl());

        // 4. Execute Search via Octen (tolerant to partial failures)
        List<SearchResultItem> searchHits = new ArrayList<>();
        int successfulQueries = 0;
        for (String query : targetedQueries) {
            try {
                List<SearchResultItem> results = octenSearchClient.searchAndExtract(query);
                if (results != null) {
                    searchHits.addAll(results);
                    successfulQueries++;
                }
            } catch (Exception e) {
                log.warn("Octen search failed for query '{}': {}", query, e.getMessage());
            }
        }

        // 5. Deduplicate Search Results
        List<SearchResultItem> uniqueHits = deduplicator.deduplicate(searchHits);

        // 6. Evidence Processing and Persistence
        List<Evidence> persistedEvidence = new ArrayList<>();
        if (evidenceProcessor != null && report.getId() != null) {
            persistedEvidence = evidenceProcessor.processAndPersistEvidence(uniqueHits, report);
        }

        // 7. Evidence JSON Export for Gemini
        String evidenceJson = evidenceJsonExporter.exportToJson(
                request.companyName(), report.getId(), persistedEvidence);

        // 8. Gemini AI Credibility Analysis
        AIAnalysisResult aiResult;
        if (geminiAnalysisService != null) {
            aiResult = geminiAnalysisService.analyzeEvidence(request.companyName(), evidenceJson);
        } else {
            aiResult = AIAnalysisResult.fallback(request.companyName(), "AI service not bound");
        }

        // 9. Deterministic Rule-Based Risk Engine (Backend owns final score and verdict)
        RiskAssessmentResult riskResult = riskAssessmentEngine.evaluateRisk(
                request.companyName(), listingDetails, persistedEvidence);

        // 10. Update & Finalize Report in Database
        report.setRiskScore(riskResult.riskScore());
        report.setRiskLevel(riskResult.riskLevel());
        report.setRecommendation(riskResult.recommendation().name() + ": " + riskResult.recommendationExplanation());

        StringBuilder findingsBuilder = new StringBuilder();
        findingsBuilder.append("=== INTERNSHIELD AI VERIFICATION REPORT ===\n\n");
        findingsBuilder.append("RECOMMENDATION: ").append(riskResult.recommendation().name()).append("\n");
        findingsBuilder.append("RISK SCORE: ").append(riskResult.riskScore()).append("/100 (").append(riskResult.riskLevel()).append(")\n\n");

        findingsBuilder.append("--- AI CREDIBILITY SUMMARY ---\n");
        findingsBuilder.append(aiResult.summary()).append("\n\n");

        if (!aiResult.positiveSignals().isEmpty()) {
            findingsBuilder.append("POSITIVE SIGNALS:\n");
            for (String pos : aiResult.positiveSignals()) {
                findingsBuilder.append("- ").append(pos).append("\n");
            }
            findingsBuilder.append("\n");
        }

        if (!aiResult.riskSignals().isEmpty()) {
            findingsBuilder.append("POTENTIAL RISK SIGNALS:\n");
            for (String risk : aiResult.riskSignals()) {
                findingsBuilder.append("- ").append(risk).append("\n");
            }
            findingsBuilder.append("\n");
        }

        if (!aiResult.evidenceLimitations().isEmpty()) {
            findingsBuilder.append("EVIDENCE LIMITATIONS:\n");
            for (String lim : aiResult.evidenceLimitations()) {
                findingsBuilder.append("- ").append(lim).append("\n");
            }
            findingsBuilder.append("\n");
        }

        findingsBuilder.append("--- DETERMINISTIC RULE BREAKDOWN ---\n");
        findingsBuilder.append(riskResult.scoringBreakdown()).append("\n");

        report.setFindings(findingsBuilder.toString());
        report.setStatus(ResearchStatus.COMPLETED);

        if (researchReportRepository != null) {
            report = researchReportRepository.save(report);
        }

        // Map persisted evidence to DTOs
        List<EvidenceResponse> evidenceDtos = persistedEvidence.stream()
                .map(EvidenceResponse::fromEntity)
                .toList();

        return new AnalysisReportResponse(
                report.getId(),
                report.getCompanyName(),
                report.getInternshipUrl(),
                report.getStatus().name(),
                riskResult.riskScore(),
                riskResult.riskLevel(),
                riskResult.recommendation().name(),
                riskResult.recommendationExplanation(),
                aiResult.summary(),
                aiResult.positiveSignals(),
                aiResult.riskSignals(),
                aiResult.evidenceLimitations(),
                riskResult.triggeredRules(),
                evidenceDtos,
                listingDetails,
                riskResult.scoringBreakdown(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
