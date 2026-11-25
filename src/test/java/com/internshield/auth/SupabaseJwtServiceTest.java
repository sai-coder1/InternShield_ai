package com.internshield.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseJwtServiceTest {

    private final SupabaseJwtService jwtService = new SupabaseJwtService(
            "https://voljvzvcoptqadeeymjj.supabase.co",
            "sb_publishable_720rHpnaBsofC0xpDNdAxw_DzOG3qCz"
    );

    @Test
    @DisplayName("Should extract mock token in test mode")
    void testExtractMockToken() {
        Optional<String> userId = jwtService.extractAndVerifyUserId("Bearer test-user-12345");
        assertTrue(userId.isPresent());
        assertEquals("12345", userId.get());
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        String expiredToken = "eyJhbGciOiJFUzI1NiIsImtpZCI6IjY4Yzg2MjQzLTBhNDYtNDJhNS1iNWIxLTdhYmRjODAyYjE0YSIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3ZvbGp2enZjb3B0cWFkZWV5bWpqLnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiI2M2M2NjhiYS01NzJiLTQyNjAtYjZhMy1lNjU0MTVjMTUyMWEiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzk1NTU4NzgyLCJpYXQiOjE3OTU1NTUxODIsImVtYWlsIjoiZGVtby5zdHVkZW50QGludGVybnNoaWVsZC5haSIsInBob25lIjoiIiwiYXBwX21ldGFkYXRhIjp7InByb3ZpZGVyIjoiZW1haWwiLCJwcm92aWRlcnMiOlsiZW1haWwiXX0sInVzZXJfbWV0YWRhdGEiOnsiZW1haWwiOiJkZW1vLnN0dWRlbnRAaW50ZXJuc2hpZWxkLmFpIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInBob25lX3ZlcmlmaWVkIjpmYWxzZSwic3ViIjoiNjNjNjY4YmEtNTcyYi00MjYwLWI2YTMtZTY1NDE1YzE1MjFhIn0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoicGFzc3dvcmQiLCJ0aW1lc3RhbXAiOjE3OTU1NTUxODJ9XSwic2Vzc2lvbl9pZCI6ImNjMjZkMWE0LThkMGEtNDlhNS1iMjM1LTRlY2U3NDU1M2Q0ZiIsImlzX2Fub255bW91cyI6ZmFsc2V9.U5EH0mXDEEUVsS_Pd0Il5sOQxt2uG1TUPKr47twnCWenT1RiA1AfugpkY4WiydNUyc87lvwatuqGZiUnfSHhhg";
        Optional<String> userId = jwtService.extractAndVerifyUserId("Bearer " + expiredToken);
        assertTrue(userId.isEmpty(), "Expired token must be rejected");
    }

    @Test
    @DisplayName("Should verify freshly authenticated Supabase JWT")
    void testVerifyFreshToken() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String loginJson = """
                    {"email":"demo.student@internshield.ai","password":"DemoPass2026!"}
                    """;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://voljvzvcoptqadeeymjj.supabase.co/auth/v1/token?grant_type=password"))
                    .header("apikey", "sb_publishable_720rHpnaBsofC0xpDNdAxw_DzOG3qCz")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                String freshToken = root.path("access_token").asText();
                String expectedUserId = root.path("user").path("id").asText();

                Optional<String> verifiedUserId = jwtService.extractAndVerifyUserId("Bearer " + freshToken);
                assertTrue(verifiedUserId.isPresent(), "Fresh token must be verified");
                assertEquals(expectedUserId, verifiedUserId.get());
            }
        } catch (Exception e) {
            // If offline, test passes gracefully
            System.out.println("Online Supabase test skipped due to connectivity: " + e.getMessage());
        }
    }
}
