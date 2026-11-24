package com.internshield.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAnalysisService.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiAnalysisService(GeminiClient geminiClient) {
        this(geminiClient, new ObjectMapper());
    }

    public GeminiAnalysisService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    /**
     * Executes AI credibility analysis on structured evidence JSON.
     * Gracefully falls back if Gemini API is unconfigured or encounters transient service failures.
     *
     * @param companyName the target company
     * @param evidenceJson the JSON payload exported from persisted evidence
     * @return structured AI analysis result
     */
    public AIAnalysisResult analyzeEvidence(String companyName, String evidenceJson) {
        if (!geminiClient.isConfigured()) {
            log.info("Gemini API key is not configured; using deterministic rules fallback.");
            return AIAnalysisResult.fallback(companyName, "GOOGLE_API_KEY environment variable not set");
        }

        try {
            String rawJson = geminiClient.analyze(evidenceJson);
            return parseGeminiResponse(companyName, rawJson);
        } catch (Exception e) {
            log.error("Gemini AI analysis failed for company {}: {}", companyName, e.getMessage());
            return AIAnalysisResult.fallback(companyName, "Gemini API call failed: " + e.getMessage());
        }
    }

    public AIAnalysisResult parseGeminiResponse(String fallbackCompany, String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            String companyName = root.path("company_name").asText(fallbackCompany);
            String summary = root.path("summary").asText("");
            if (summary.isBlank() && root.hasNonNull("verdict_disclaimer")) {
                summary = "AI analysis completed based on available research evidence.";
            }

            List<String> positiveSignals = extractStringList(root.path("positive_signals"));
            List<String> riskSignals = extractStringList(root.path("risk_signals"));
            List<String> evidenceLimitations = extractStringList(root.path("evidence_limitations"));
            List<String> sources = extractStringList(root.path("sources"));
            String verdictDisclaimer = root.path("verdict_disclaimer").asText(
                    "Deterministic backend risk engine computes the final verdict and risk score.");

            return new AIAnalysisResult(
                    companyName,
                    summary,
                    positiveSignals,
                    riskSignals,
                    evidenceLimitations,
                    sources,
                    verdictDisclaimer,
                    false,
                    rawResponse
            );
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response as JSON: {}. Using fallback format.", e.getMessage());
            return new AIAnalysisResult(
                    fallbackCompany,
                    rawResponse,
                    List.of(),
                    List.of(),
                    List.of("Gemini returned unstructured output; raw text preserved."),
                    List.of(),
                    "Notice: Backend rule engine computes the final verdict.",
                    true,
                    rawResponse
            );
        }
    }

    private List<String> extractStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }
}
