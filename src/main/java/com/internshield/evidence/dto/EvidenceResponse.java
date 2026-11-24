package com.internshield.evidence.dto;

import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceCategory;

import java.time.LocalDateTime;

public record EvidenceResponse(
        Long id,
        String title,
        String sourceUrl,
        String description,
        String sourceDomain,
        EvidenceCategory category,
        String sourceCredibility,
        LocalDateTime collectedAt,
        Long researchReportId
) {
    public static EvidenceResponse fromEntity(Evidence evidence) {
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getTitle(),
                evidence.getSourceUrl(),
                evidence.getDescription(),
                evidence.getSourceDomain(),
                evidence.getCategory(),
                evidence.getSourceCredibility(),
                evidence.getCollectedAt(),
                evidence.getResearchReport() != null ? evidence.getResearchReport().getId() : null
        );
    }
}
