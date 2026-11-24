package com.internshield.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.search.dto.SearchResultItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OctenSearchClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSearch_successfulResponseParsing() throws Exception {
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);

        String jsonResponse = """
                {
                  "code": 0,
                  "msg": "Success",
                  "data": {
                    "results": [
                      {
                        "title": "Acme Scam Complaints",
                        "url": "https://complaints.example.com/acme",
                        "highlight": "Acme charged 2000 INR registration fee"
                      }
                    ]
                  }
                }
                """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        OctenSearchClient client = new OctenSearchClient(
                "https://api.octen.ai/search",
                "dummy-api-key",
                5,
                5,
                5,
                mockHttpClient,
                objectMapper
        );

        JsonNode results = client.search("Acme Corp complaints");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Acme Scam Complaints", results.get(0).path("title").asText());

        List<SearchResultItem> extracted = client.searchAndExtract("Acme Corp complaints");
        assertEquals(1, extracted.size());
        assertEquals("Acme Scam Complaints", extracted.get(0).title());
        assertEquals("https://complaints.example.com/acme", extracted.get(0).url());
        assertEquals("Acme charged 2000 INR registration fee", extracted.get(0).snippet());
        assertEquals("complaints.example.com", extracted.get(0).source());
    }

    @Test
    void testSearch_missingApiKeyThrowsIllegalStateException() {
        OctenSearchClient client = new OctenSearchClient(
                "https://api.octen.ai/search",
                null,
                5,
                5,
                5,
                null,
                objectMapper
        );

        assertThrows(IllegalStateException.class, () -> client.search("query"));
    }

    @Test
    void testSearch_apiErrorResponseThrowsRuntimeException() throws Exception {
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);

        String jsonResponse = """
                {
                  "code": 401,
                  "msg": "Invalid API key"
                }
                """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        OctenSearchClient client = new OctenSearchClient(
                "https://api.octen.ai/search",
                "invalid-key",
                5,
                5,
                5,
                mockHttpClient,
                objectMapper
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.search("query"));
        assertEquals("Octen API error: Invalid API key", ex.getMessage());
    }
}
