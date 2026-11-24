package com.internshield.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeminiClient {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient() {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();

    }

    private String getApiKey() {

        String apiKey = System.getenv("GOOGLE_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_API_KEY environment variable not found.");
        }

        return apiKey;
    }

    private ObjectNode buildRequest(String evidenceJson) {

        ObjectNode requestBody = objectMapper.createObjectNode();

        // System instructions for Gemini
        ObjectNode systemInstruction = requestBody.putObject("systemInstruction");

        systemInstruction.putArray("parts")
                .addObject()
                .put("text",
                        "You are an expert internship verification research analyst. " +

                                "Analyze only the evidence supplied by the user. " +

                                "Follow these rules strictly: " +

                                "1. Distinguish company-level information from internship-specific evidence. " +

                                "2. Treat official company websites as stronger evidence of company identity, " +
                                "but never assume that a specific job offer is genuine just because the company is legitimate. "
                                +

                                "3. Treat employee reviews as general workplace feedback, " +
                                "not proof of internship fraud. " +

                                "4. Identify recruitment impersonation warnings separately from confirmed fraud. " +

                                "5. Never treat missing information as proof of fraud. " +

                                "6. Never invent facts, sources, URLs, or allegations. " +

                                "7. Clearly distinguish verified facts from unverified third-party claims. " +

                                "8. Highlight whether evidence is relevant to the specific internship role. " +

                                "9. Explain limitations when evidence is insufficient or unrelated. " +

                                "10. Never make the final fraud verdict. The backend rule engine decides that. " +

                                "Return valid JSON with these fields: " +
                                "company_name, positive_signals, risk_signals, " +
                                "evidence_limitations, sources, verdict_disclaimer. " +

                                "The sources field must contain only URLs supplied in the evidence. " +

                                "Be cautious, factual, and evidence-based.");

        // User's evidence JSON
        ObjectNode userContent = requestBody.putArray("contents")
                .addObject();

        userContent.put("role", "user");

        userContent.putArray("parts")
                .addObject()
                .put("text",
                        "Analyze this internship research evidence:\n"
                                + evidenceJson);

        // Request JSON response
        requestBody.putObject("generationConfig")
                .put("responseMimeType", "application/json");

        return requestBody;
    }

    public String analyze(String evidenceJson) throws Exception {

        ObjectNode requestBody = buildRequest(evidenceJson);
        String requestJson = objectMapper.writeValueAsString(requestBody);

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", getApiKey())
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {

                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode candidates = jsonResponse.path("candidates");

                if (!candidates.isArray() || candidates.isEmpty()) {
                    throw new RuntimeException("Gemini returned no analysis.");
                }

                return candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();
            }

            if (response.statusCode() != 503 || attempt == maxAttempts) {
                throw new RuntimeException(
                        "Gemini API error (HTTP "
                                + response.statusCode() + "): " + response.body());
            }

            long waitSeconds = (long) Math.pow(2, attempt);

            System.out.println(
                    "Gemini temporarily unavailable. Retrying in "
                            + waitSeconds + " seconds...");

            Thread.sleep(waitSeconds * 1000);
        }

        throw new RuntimeException("Gemini request failed after retries.");
    }
}