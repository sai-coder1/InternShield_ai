package com.internshield.report.dto;

import com.internshield.report.ResearchReport;
import com.internshield.report.ResearchStatus;

import java.time.LocalDateTime;

public record ResearchResponse(
        Long id,
        String companyName,
        String internshipUrl,
        ResearchStatus status,
        Integer riskScore,
        String riskLevel,
        String findings,
        String recommendation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResearchResponse fromEntity(ResearchReport report) {
        return new ResearchResponse(
                report.getId(),
                report.getCompanyName(),
                report.getInternshipUrl(),
                report.getStatus(),
                report.getRiskScore(),
                report.getRiskLevel(),
                report.getFindings(),
                report.getRecommendation(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
