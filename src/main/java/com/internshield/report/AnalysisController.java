package com.internshield.report;

import com.internshield.auth.SupabaseJwtService;
import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceRepository;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.internship.InternshipListingExtractor;
import com.internshield.internship.dto.ExtractedInternshipDetails;
import com.internshield.report.dto.AnalysisReportResponse;
import com.internshield.report.dto.AnalysisRequest;
import com.internshield.research.ResearchOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final ResearchOrchestrator orchestrator;
    private final ResearchReportRepository researchReportRepository;
    private final EvidenceRepository evidenceRepository;
    private final InternshipListingExtractor listingExtractor;
    private final SupabaseJwtService jwtService;

    public AnalysisController(
            ResearchOrchestrator orchestrator,
            ResearchReportRepository researchReportRepository,
            EvidenceRepository evidenceRepository,
            InternshipListingExtractor listingExtractor,
            SupabaseJwtService jwtService) {
        this.orchestrator = orchestrator;
        this.researchReportRepository = researchReportRepository;
        this.evidenceRepository = evidenceRepository;
        this.listingExtractor = listingExtractor;
        this.jwtService = jwtService;
    }

    /**
     * Executes complete, end-to-end internship verification and risk analysis.
     * Requires a valid Supabase JWT in the Authorization header.
     */
    @PostMapping
    public ResponseEntity<AnalysisReportResponse> analyzeInternship(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody AnalysisRequest request) {
        String userId = jwtService.extractAndVerifyUserId(authHeader)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid Supabase authentication token required"));

        AnalysisReportResponse response = orchestrator.executeCompleteAnalysis(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves an existing analysis report and its collected evidence by report ID.
     * Enforces user isolation: only the owner of the report can retrieve it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnalysisReportResponse> getAnalysisById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        String userId = jwtService.extractAndVerifyUserId(authHeader)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid Supabase authentication token required"));

        ResearchReport report = researchReportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Research report not found with id: " + id));

        List<Evidence> evidenceList = evidenceRepository.findByResearchReportId(id);
        List<EvidenceResponse> evidenceDtos = evidenceList.stream()
                .map(EvidenceResponse::fromEntity)
                .toList();

        ExtractedInternshipDetails listingDetails = ExtractedInternshipDetails.empty();
        if (report.getInternshipUrl() != null && !report.getInternshipUrl().isBlank()) {
            listingDetails = listingExtractor.extractFromUrl(report.getInternshipUrl());
        }

        String recommendationName = "UNKNOWN";
        String recommendationExplanation = report.getRecommendation();
        if (report.getRecommendation() != null && report.getRecommendation().contains(":")) {
            int splitIdx = report.getRecommendation().indexOf(':');
            recommendationName = report.getRecommendation().substring(0, splitIdx).trim();
            recommendationExplanation = report.getRecommendation().substring(splitIdx + 1).trim();
        } else if (report.getRecommendation() != null) {
            recommendationName = report.getRecommendation();
        }

        AnalysisReportResponse response = new AnalysisReportResponse(
                report.getId(),
                report.getCompanyName(),
                report.getInternshipUrl(),
                report.getStatus().name(),
                report.getRiskScore() != null ? report.getRiskScore() : 0,
                report.getRiskLevel() != null ? report.getRiskLevel() : "UNKNOWN",
                recommendationName,
                recommendationExplanation,
                report.getFindings() != null ? report.getFindings() : "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                evidenceDtos,
                listingDetails,
                report.getFindings(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all analysis reports for the authenticated user, ordered by creation date.
     * Enforces user isolation: reports with other user IDs or null user_id are never returned.
     */
    @GetMapping
    public ResponseEntity<List<AnalysisReportResponse>> getAllReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = jwtService.extractAndVerifyUserId(authHeader)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid Supabase authentication token required"));

        List<ResearchReport> reports = researchReportRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<AnalysisReportResponse> responses = reports.stream()
                .map(r -> new AnalysisReportResponse(
                        r.getId(),
                        r.getCompanyName(),
                        r.getInternshipUrl(),
                        r.getStatus().name(),
                        r.getRiskScore() != null ? r.getRiskScore() : 0,
                        r.getRiskLevel() != null ? r.getRiskLevel() : "UNKNOWN",
                        r.getRecommendation() != null ? r.getRecommendation() : "UNKNOWN",
                        "",
                        r.getFindings() != null ? r.getFindings() : "",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        ExtractedInternshipDetails.empty(),
                        "",
                        r.getCreatedAt(),
                        r.getUpdatedAt()
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }
}
