package com.internshield.search;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class OctenSearchClient {

    private static final String API_URL = "https://api.octen.ai/search";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OctenSearchClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public JsonNode search(String query) throws Exception {
        String apiKey = System.getenv("OCTEN_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OCTEN_API_KEY environment variable not found.");
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("query", query);
        requestBody.put("count", 5);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }

        JsonNode jsonResponse = objectMapper.readTree(response.body());

        if (jsonResponse.path("code").asInt(-1) != 0) {
            throw new RuntimeException(
                    "Octen API error: " +
                            jsonResponse.path("msg").asText("Unknown error"));
        }

        return jsonResponse.path("data").path("results");
    }
}
