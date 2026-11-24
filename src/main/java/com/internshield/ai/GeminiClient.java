package com.internshield.ai;

<<<<<<< HEAD
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

=======
>>>>>>> origin/backend
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

<<<<<<< HEAD
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final String apiUrl;
    private final String apiKey;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int maxRetries;
=======
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeminiClient {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent";
>>>>>>> origin/backend

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

<<<<<<< HEAD
    /**
     * Default constructor for standalone executions or legacy invocations.
     */
    public GeminiClient() {
        this(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
                System.getenv("GOOGLE_API_KEY"),
                10,
                60,
                3,
                null,
                new ObjectMapper()
        );
    }

    /**
     * Spring Boot constructor injection with configurable properties.
     */
    @Autowired
    public GeminiClient(
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}") String apiUrl,
            @Value("${google.ai.api.key:}") String apiKey,
            @Value("${gemini.api.connect-timeout-seconds:10}") int connectTimeoutSeconds,
            @Value("${gemini.api.read-timeout-seconds:60}") int readTimeoutSeconds,
            @Value("${gemini.api.max-retries:3}") int maxRetries) {
        this(
                apiUrl,
                (apiKey != null && !apiKey.isBlank()) ? apiKey : System.getenv("GOOGLE_API_KEY"),
                connectTimeoutSeconds,
                readTimeoutSeconds,
                maxRetries,
                null,
                new ObjectMapper()
        );
    }

    /**
     * Fully parameterized constructor for testing and dependency injection.
     */
    public GeminiClient(
            String apiUrl,
            String apiKey,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int maxRetries,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.apiUrl = (apiUrl != null && !apiUrl.isBlank())
                ? apiUrl
                : "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
        this.apiKey = apiKey;
        this.connectTimeoutSeconds = connectTimeoutSeconds > 0 ? connectTimeoutSeconds : 10;
        this.readTimeoutSeconds = readTimeoutSeconds > 0 ? readTimeoutSeconds : 60;
        this.maxRetries = maxRetries > 0 ? maxRetries : 3;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.httpClient = (httpClient != null) ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.connectTimeoutSeconds))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String getApiKey() {
        if (!isConfigured()) {
            throw new IllegalStateException("GOOGLE_API_KEY environment variable not found.");
        }
        return apiKey;
    }

    public ObjectNode buildRequest(String evidenceJson) {
=======
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

>>>>>>> origin/backend
        ObjectNode requestBody = objectMapper.createObjectNode();

        // System instructions for Gemini
        ObjectNode systemInstruction = requestBody.putObject("systemInstruction");

        systemInstruction.putArray("parts")
                .addObject()
                .put("text",
<<<<<<< HEAD
                        "You are an expert internship verification research analyst for InternShield AI. " +
                                "Analyze only the evidence supplied in the user's JSON payload. " +
                                "Follow these rules strictly: " +
                                "1. Distinguish company-level information from internship-specific evidence. " +
                                "2. Treat official company websites as stronger evidence of company identity, " +
                                "but never assume that a specific job offer is genuine just because the company is legitimate. " +
                                "3. Treat employee reviews as general workplace feedback, " +
                                "not proof of internship fraud. " +
                                "4. Identify recruitment impersonation warnings separately from confirmed fraud. " +
                                "5. Never treat missing information as proof of fraud. " +
                                "6. Never invent facts, sources, URLs, or allegations. " +
                                "7. Clearly distinguish verified facts from unverified third-party claims. " +
                                "8. Highlight whether evidence is relevant to the specific internship role. " +
                                "9. Explain limitations when evidence is insufficient or unrelated. " +
                                "10. Never make the final fraud verdict or risk score. The backend deterministic rule engine decides that. " +
                                "Return valid JSON with these exact fields: " +
                                "company_name (string), summary (string), positive_signals (array of strings), " +
                                "risk_signals (array of strings), evidence_limitations (array of strings), " +
                                "sources (array of URLs present in the input only), " +
                                "verdict_disclaimer (string noting that backend rule engine calculates the score).");

        // User's evidence JSON
        ObjectNode userContent = requestBody.putArray("contents").addObject();
        userContent.put("role", "user");
        userContent.putArray("parts")
                .addObject()
                .put("text", "Analyze this internship research evidence:\n" + evidenceJson);
=======
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
>>>>>>> origin/backend

        // Request JSON response
        requestBody.putObject("generationConfig")
                .put("responseMimeType", "application/json");

        return requestBody;
    }

    public String analyze(String evidenceJson) throws Exception {
<<<<<<< HEAD
        ObjectNode requestBody = buildRequest(evidenceJson);
        String requestJson = objectMapper.writeValueAsString(requestBody);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", getApiKey())
                    .timeout(Duration.ofSeconds(readTimeoutSeconds))
=======

        ObjectNode requestBody = buildRequest(evidenceJson);
        String requestJson = objectMapper.writeValueAsString(requestBody);

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", getApiKey())
                    .timeout(Duration.ofSeconds(60))
>>>>>>> origin/backend
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
<<<<<<< HEAD
=======

>>>>>>> origin/backend
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode candidates = jsonResponse.path("candidates");

                if (!candidates.isArray() || candidates.isEmpty()) {
<<<<<<< HEAD
                    throw new RuntimeException("Gemini returned no candidates in response.");
=======
                    throw new RuntimeException("Gemini returned no analysis.");
>>>>>>> origin/backend
                }

                return candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();
            }

<<<<<<< HEAD
            if (response.statusCode() != 503 || attempt == maxRetries) {
                throw new RuntimeException(
                        "Gemini API error (HTTP " + response.statusCode() + "): " + response.body());
            }

            long waitSeconds = (long) Math.pow(2, attempt);
            log.warn("Gemini 503 service unavailable. Retrying attempt {} in {} seconds...", attempt, waitSeconds);
            Thread.sleep(waitSeconds * 1000);
        }

        throw new RuntimeException("Gemini request failed after " + maxRetries + " retries.");
    }
}
=======
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
>>>>>>> origin/backend
