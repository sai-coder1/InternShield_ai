package com.internshield.analysis;

import java.util.List;

/**
 * Result of the deterministic rule-based risk evaluation engine.
 */
public record RiskAssessmentResult(
        int riskScore,
        String riskLevel,
        RiskRecommendation recommendation,
        String recommendationExplanation,
        List<RiskRuleViolation> triggeredRules,
        List<String> positiveSignals,
        boolean sufficientEvidence,
        String scoringBreakdown
) {
}
