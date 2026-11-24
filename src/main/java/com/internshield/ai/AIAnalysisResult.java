package com.internshield.ai;

import java.util.List;

/**
 * Structured result produced by Gemini AI analysis of research evidence.
 * Note: Gemini identifies qualitative signals and evidence limitations, but does NOT
 * compute the final risk score or verdict; the deterministic risk engine owns that calculation.
 */
public record AIAnalysisResult(
        String companyName,
        String summary,
        List<String> positiveSignals,
        List<String> riskSignals,
        List<String> evidenceLimitations,
        List<String> sources,
        String verdictDisclaimer,
        boolean fallbackUsed,
        String rawResponse
) {
    public static AIAnalysisResult fallback(String companyName, String reason) {
        return new AIAnalysisResult(
                companyName != null ? companyName : "Target Company",
                "Automated AI credibility analysis unavailable (" + reason + "). Deterministic rule engine evaluated available evidence.",
                List.of(),
                List.of("External AI credibility analysis could not complete: " + reason),
                List.of("Assessment is based strictly on deterministic heuristic rules and collected evidence records."),
                List.of(),
                "Notice: Verdict and risk score are computed deterministically by the InternShield backend engine.",
                true,
                "{}"
        );
    }
}
