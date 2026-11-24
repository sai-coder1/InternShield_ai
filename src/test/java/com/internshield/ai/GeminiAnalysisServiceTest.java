package com.internshield.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GeminiAnalysisServiceTest {

    private GeminiClient mockGeminiClient;
    private GeminiAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        mockGeminiClient = Mockito.mock(GeminiClient.class);
        analysisService = new GeminiAnalysisService(mockGeminiClient);
    }

    @Test
    @DisplayName("Should parse structured Gemini JSON response into AIAnalysisResult")
    void testAnalyzeEvidence_successfulJsonParsing() throws Exception {
        when(mockGeminiClient.isConfigured()).thenReturn(true);

        String validGeminiOutput = """
                {
                  "company_name": "Initech Corp",
                  "summary": "Legitimate enterprise with verified MCA registration. No reports of training fee scams.",
                  "positive_signals": [
                    "Official corporate domain initech.com verified",
                    "Active registration on MCA registry"
                  ],
                  "risk_signals": [
                    "Recruiter uses personal contact info on job board posting"
                  ],
                  "evidence_limitations": [
                    "No glassdoor reviews found for this specific branch"
                  ],
                  "sources": [
                    "https://mca.gov.in/company/initech",
                    "https://initech.com/careers"
                  ],
                  "verdict_disclaimer": "Deterministic backend rule engine computes the final verdict."
                }
                """;

        when(mockGeminiClient.analyze(anyString())).thenReturn(validGeminiOutput);

        AIAnalysisResult result = analysisService.analyzeEvidence("Initech Corp", "{}");

        assertNotNull(result);
        assertEquals("Initech Corp", result.companyName());
        assertFalse(result.fallbackUsed());
        assertEquals(2, result.positiveSignals().size());
        assertEquals("Official corporate domain initech.com verified", result.positiveSignals().get(0));
        assertEquals(1, result.riskSignals().size());
        assertEquals(1, result.evidenceLimitations().size());
        assertEquals(2, result.sources().size());
        assertTrue(result.verdictDisclaimer().contains("rule engine computes the final verdict")
                || result.verdictDisclaimer().contains("Deterministic"));
    }

    @Test
    @DisplayName("Should return structured fallback when Gemini is unconfigured")
    void testAnalyzeEvidence_unconfiguredClientReturnsFallback() {
        when(mockGeminiClient.isConfigured()).thenReturn(false);

        AIAnalysisResult result = analysisService.analyzeEvidence("Acme Corp", "{}");

        assertNotNull(result);
        assertTrue(result.fallbackUsed());
        assertEquals("Acme Corp", result.companyName());
        assertTrue(result.summary().contains("GOOGLE_API_KEY") || result.summary().contains("fallback"));
    }

    @Test
    @DisplayName("Should handle Gemini API exception gracefully with fallback result")
    void testAnalyzeEvidence_handlesApiExceptionGracefully() throws Exception {
        when(mockGeminiClient.isConfigured()).thenReturn(true);
        when(mockGeminiClient.analyze(anyString())).thenThrow(new RuntimeException("503 Service Unavailable"));

        AIAnalysisResult result = analysisService.analyzeEvidence("Faulty Corp", "{}");

        assertNotNull(result);
        assertTrue(result.fallbackUsed());
        assertTrue(result.summary().contains("failed") || result.summary().contains("503"));
    }

    @Test
    @DisplayName("Should handle non-JSON / plain text Gemini output safely without crashing")
    void testParseGeminiResponse_plainTextHandledGracefully() {
        String plainText = "Here is my analysis: Company seems legitimate based on news articles.";

        AIAnalysisResult result = analysisService.parseGeminiResponse("Test Corp", plainText);

        assertNotNull(result);
        assertTrue(result.fallbackUsed());
        assertEquals(plainText, result.summary());
        assertEquals(1, result.evidenceLimitations().size());
    }
}
