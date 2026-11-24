package com.internshield.report.dto;

import com.internshield.analysis.RiskRuleViolation;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.internship.dto.ExtractedInternshipDetails;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisReportResponse(
        Long id,
        String companyName,
        String internshipUrl,
        String status,
        Integer riskScore,
        String riskLevel,
        String recommendation,
        String recommendationExplanation,
        String aiSummary,
        List<String> positiveSignals,
        List<String> riskSignals,
        List<String> evidenceLimitations,
        List<RiskRuleViolation> triggeredRules,
        List<EvidenceResponse> evidence,
        ExtractedInternshipDetails listingDetails,
        String scoringBreakdown,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
