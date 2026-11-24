package com.internshield.search;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.internshield.search.dto.SearchResultItem;

@Component
public class OctenSearchClient implements SearchProvider {

    private final String apiUrl;
    private final String apiKey;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int defaultCount;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor for standalone executions or legacy invocations.
     * Preserves compatibility with App.java.
     */
    public OctenSearchClient() {
        this(
                "https://api.octen.ai/search",
                System.getenv("OCTEN_API_KEY"),
                10,
                30,
                5,
                null,
                new ObjectMapper()
        );
    }

    /**
     * Spring Boot constructor injection with configurable properties.
     */
    @Autowired
    public OctenSearchClient(
            @Value("${octen.api.url:https://api.octen.ai/search}") String apiUrl,
            @Value("${octen.api.key:}") String apiKey,
            @Value("${octen.api.connect-timeout-seconds:10}") int connectTimeoutSeconds,
            @Value("${octen.api.read-timeout-seconds:30}") int readTimeoutSeconds,
            @Value("${octen.api.default-result-count:5}") int defaultCount) {
        this(
                apiUrl,
                (apiKey != null && !apiKey.isBlank()) ? apiKey : System.getenv("OCTEN_API_KEY"),
                connectTimeoutSeconds,
                readTimeoutSeconds,
                defaultCount,
                null,
                new ObjectMapper()
        );
    }

    /**
     * Fully parameterized constructor for testing and dependency injection.
     */
    public OctenSearchClient(
            String apiUrl,
            String apiKey,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int defaultCount,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.apiUrl = (apiUrl != null && !apiUrl.isBlank()) ? apiUrl : "https://api.octen.ai/search";
        this.apiKey = apiKey;
        this.connectTimeoutSeconds = connectTimeoutSeconds > 0 ? connectTimeoutSeconds : 10;
        this.readTimeoutSeconds = readTimeoutSeconds > 0 ? readTimeoutSeconds : 30;
        this.defaultCount = defaultCount > 0 ? defaultCount : 5;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.httpClient = (httpClient != null) ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.connectTimeoutSeconds))
                .build();
    }

    public JsonNode search(String query) throws Exception {
        return search(query, defaultCount);
    }

    public JsonNode search(String query, int count) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OCTEN_API_KEY environment variable not found.");
        }

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("query", query);
        requestBody.put("count", count > 0 ? count : defaultCount);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(readTimeoutSeconds))
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

    @Override
    public List<SearchResultItem> searchAndExtract(String query) throws Exception {
        return searchAndExtract(query, defaultCount);
    }

    @Override
    public List<SearchResultItem> searchAndExtract(String query, int count) throws Exception {
        JsonNode resultsNode = search(query, count);
        List<SearchResultItem> items = new ArrayList<>();
        if (resultsNode != null && resultsNode.isArray()) {
            for (JsonNode node : resultsNode) {
                String title = node.path("title").asText("");
                String url = node.path("url").asText("");

                String snippet = "";
                if (node.hasNonNull("highlight") && !node.path("highlight").asText().isBlank()) {
                    snippet = node.path("highlight").asText();
                } else if (node.hasNonNull("snippet") && !node.path("snippet").asText().isBlank()) {
                    snippet = node.path("snippet").asText();
                } else if (node.hasNonNull("description") && !node.path("description").asText().isBlank()) {
                    snippet = node.path("description").asText();
                }

                String source = extractDomain(url);
                if (node.hasNonNull("source") && !node.path("source").asText().isBlank()) {
                    source = node.path("source").asText();
                }

                if (!url.isBlank()) {
                    items.add(new SearchResultItem(title, url, snippet, source, query));
                }
            }
        }
        return items;
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return "unknown";
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {
        }
        return "web";
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public int getDefaultCount() {
        return defaultCount;
    }
}
