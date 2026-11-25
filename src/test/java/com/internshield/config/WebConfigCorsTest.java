package com.internshield.config;

import com.internshield.common.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = {
    "cors.allowed-origins=https://internshield-ai.vercel.app/,http://localhost:3000"
})
class WebConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPreflightCorsRequestAllowedFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/health")
                .header("Origin", "https://internshield-ai.vercel.app")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://internshield-ai.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void testPreflightCorsRequestAllowedFromLocalhost() throws Exception {
        mockMvc.perform(options("/api/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void testPreflightCorsRequestForbiddenFromUnauthorizedOrigin() throws Exception {
        mockMvc.perform(options("/api/health")
                .header("Origin", "https://malicious-site.example.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
