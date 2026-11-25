package com.internshield.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.ai.GeminiClient;
import com.internshield.auth.SupabaseJwtService;
import com.internshield.evidence.EvidenceRepository;
import com.internshield.report.dto.AnalysisReportResponse;
import com.internshield.report.dto.AnalysisRequest;
import com.internshield.research.ResearchOrchestrator;
import com.internshield.search.OctenSearchClient;
import com.internshield.search.dto.SearchResultItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScamScoringVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OctenSearchClient octenSearchClient;

    @MockBean
    private GeminiClient geminiClient;

    private static final String AUTH_HEADER = "Bearer test-user-student-demo";

    @Test
    @DisplayName("Verify CareerBoost Suspicious Example produces CRITICAL / HIGH risk score")
    void testCareerBoostSuspiciousExample() throws Exception {
        String companyName = "CareerBoost Pvt Ltd";
        String description = """
                Company: CareerBoost Pvt Ltd
                Role: Full Stack Developer Intern
                Stipend: ₹20,000/month
                Special Offer: Complete our ₹2,999 certification program and you are 100% guaranteed a job after the internship.
                Payment: UPI
                Limited seats: 7 remaining.
                """;

        AnalysisRequest request = new AnalysisRequest(companyName, null, description);

        // Mock external search hits
        SearchResultItem scamItem = new SearchResultItem(
                "CareerBoost Complaints - Upfront fee",
                "https://consumerforum.example.com/careerboost-scam",
                "Students reported CareerBoost charging certification fees with UPI payments",
                "consumerforum.example.com",
                companyName + " reviews scam"
        );
        when(octenSearchClient.searchAndExtract(anyString())).thenReturn(List.of(scamItem));
        when(octenSearchClient.searchAndExtract(anyString(), anyInt())).thenReturn(List.of(scamItem));

        when(geminiClient.isConfigured()).thenReturn(true);
        String mockGeminiResponse = """
                {
                  "company_name": "CareerBoost Pvt Ltd",
                  "summary": "Severe fraud flags: upfront certification fee demanded, 100% job guarantee without interview, informal UPI payment request, and urgency pressure.",
                  "positive_signals": [],
                  "risk_signals": [
                    "Upfront certification fee of Rs 2999 required",
                    "100% guaranteed job claim",
                    "Payment requested via UPI",
                    "Artificial urgency: 7 seats remaining"
                  ],
                  "evidence_limitations": ["No registered MCA records"],
                  "sources": ["https://consumerforum.example.com/careerboost-scam"],
                  "verdict_disclaimer": "Deterministic engine computes the final verdict."
                }
                """;
        when(geminiClient.analyze(anyString())).thenReturn(mockGeminiResponse);

        MvcResult result = mockMvc.perform(post("/api/analysis")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AnalysisReportResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AnalysisReportResponse.class
        );

        assertNotNull(response);
        assertEquals(companyName, response.companyName());
        System.out.println("CareerBoost Risk Score: " + response.riskScore() + " (" + response.riskLevel() + ")");
        assertTrue(response.riskScore() >= 80, "CareerBoost must produce high/critical score, got: " + response.riskScore());
        assertEquals("CRITICAL", response.riskLevel());
        assertFalse(response.triggeredRules().isEmpty(), "Triggered rules must not be empty");
    }

    @Test
    @DisplayName("Verify Clean Internship Example produces LOW / SAFE risk score")
    void testCleanInternshipExample() throws Exception {
        String companyName = "Infosys Technologies";
        String description = """
                Company: Infosys Technologies
                Role: Software Engineering Intern
                Stipend: ₹25,000/month
                Details: Join our cloud engineering team for a 6-month internship. You will build microservices, write clean code and automated tests, and collaborate with agile teams. Apply through our official careers portal careers.infosys.com.
                """;

        AnalysisRequest request = new AnalysisRequest(companyName, null, description);

        // Mock legitimate external search hits
        SearchResultItem legitItem1 = new SearchResultItem(
                "Infosys Careers Official Portal",
                "https://careers.infosys.com",
                "Official careers and internship opportunities at Infosys Limited",
                "infosys.com",
                companyName + " official website"
        );
        SearchResultItem legitItem2 = new SearchResultItem(
                "Infosys Limited MCA Registration",
                "https://zaubacorp.com/company/infosys-limited",
                "Active publicly listed corporate entity registered in India",
                "zaubacorp.com",
                companyName + " corporate registration"
        );
        when(octenSearchClient.searchAndExtract(anyString())).thenReturn(List.of(legitItem1, legitItem2));
        when(octenSearchClient.searchAndExtract(anyString(), anyInt())).thenReturn(List.of(legitItem1, legitItem2));

        when(geminiClient.isConfigured()).thenReturn(true);
        String mockGeminiResponse = """
                {
                  "company_name": "Infosys Technologies",
                  "summary": "Legitimate enterprise internship offer from verified global organization.",
                  "positive_signals": ["Official careers domain careers.infosys.com", "MCA corporate registration verified"],
                  "risk_signals": [],
                  "evidence_limitations": [],
                  "sources": ["https://careers.infosys.com", "https://zaubacorp.com/company/infosys-limited"],
                  "verdict_disclaimer": "Deterministic engine computes the final verdict."
                }
                """;
        when(geminiClient.analyze(anyString())).thenReturn(mockGeminiResponse);

        MvcResult result = mockMvc.perform(post("/api/analysis")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AnalysisReportResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AnalysisReportResponse.class
        );

        assertNotNull(response);
        assertEquals(companyName, response.companyName());
        System.out.println("Clean Example Risk Score: " + response.riskScore() + " (" + response.riskLevel() + ")");
        assertTrue(response.riskScore() <= 20, "Clean listing must produce low/safe score, got: " + response.riskScore());
        assertEquals("LOW", response.riskLevel());
    }
}
