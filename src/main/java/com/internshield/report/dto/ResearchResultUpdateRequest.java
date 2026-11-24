package com.internshield.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.internshield.report.ResearchStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ResearchResultUpdateRequest(
        @Min(value = 0, message = "Risk score must be at least 0")
        @Max(value = 100, message = "Risk score must be at most 100")
        Integer riskScore,

        @Pattern(regexp = "^(LOW|MEDIUM|HIGH|UNKNOWN)$", message = "Risk level must be LOW, MEDIUM, HIGH, or UNKNOWN")
        String riskLevel,

        String findings,

        String recommendation,

        @NotNull(message = "Status must be provided")
        ResearchStatus status
) {
    @AssertTrue(message = "Risk score must not be null when status is COMPLETED")
    @JsonIgnore
    public boolean isRiskScoreValidWhenCompleted() {
        if (status == ResearchStatus.COMPLETED) {
            return riskScore != null;
        }
        return true;
    }

    @AssertTrue(message = "Risk level must not be null when status is COMPLETED")
    @JsonIgnore
    public boolean isRiskLevelValidWhenCompleted() {
        if (status == ResearchStatus.COMPLETED) {
            return riskLevel != null;
        }
        return true;
    }

    @AssertTrue(message = "Findings must not be blank when status is COMPLETED")
    @JsonIgnore
    public boolean isFindingsValidWhenCompleted() {
        if (status == ResearchStatus.COMPLETED) {
            return findings != null && !findings.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "Recommendation must not be blank when status is COMPLETED")
    @JsonIgnore
    public boolean isRecommendationValidWhenCompleted() {
        if (status == ResearchStatus.COMPLETED) {
            return recommendation != null && !recommendation.isBlank();
        }
        return true;
    }
}
