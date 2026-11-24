package com.internshield.report.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank(message = "Company name is required")
        String companyName,
        String internshipUrl,
        String pastedDescription
) {
}
