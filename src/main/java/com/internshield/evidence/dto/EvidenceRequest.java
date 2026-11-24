package com.internshield.evidence.dto;

import com.internshield.evidence.EvidenceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record EvidenceRequest(
        @NotBlank(message = "Title must not be blank")
        String title,

        @NotBlank(message = "Source URL must not be blank")
        @URL(message = "Source URL must be a valid URL")
        String sourceUrl,

        String description,

        String sourceDomain,

        @NotNull(message = "Evidence category must be provided")
        EvidenceCategory category,

        String sourceCredibility,

        @NotNull(message = "Research report ID must be provided")
        Long researchReportId
) {
}
