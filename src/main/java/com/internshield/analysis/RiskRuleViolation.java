package com.internshield.analysis;

import java.util.List;

/**
 * Details of a specific deterministic risk rule triggered during evaluation.
 */
public record RiskRuleViolation(
        String ruleId,
        String ruleName,
        String category,
        int scoreImpact,
        String severity,
        String explanation,
        List<String> matchedEvidence
) {
}
