package com.internshield.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.ai.GeminiClient;
import com.internshield.evidence.Evidence;
import com.internshield.evidence.EvidenceCategory;
import com.internshield.evidence.EvidenceRepository;
import com.internshield.report.dto.AnalysisReportResponse;
import com.internshield.report.dto.AnalysisRequest;
import com.internshield.research.ResearchOrchestrator;
import com.internshield.search.OctenSearchClient;
import com.internshield.search.SearchProvider;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalysisWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResearchReportRepository researchReportRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private ResearchOrchestrator orchestrator;

    @MockBean
    private OctenSearchClient octenSearchClient;

    @MockBean
    private GeminiClient geminiClient;

    @Test
    @DisplayName("Complete Pipeline Verification: Request -> Extract -> QueryGen -> Search -> Process -> Persist -> AI -> RiskEngine -> Final Report")
    void testCompleteAnalysisWorkflow_endToEndPipeline() throws Exception {
        String companyName = "Apex Financial Solutions";
        String pastedJob = """
                Role: Quantitative Analyst Intern
                Company: Apex Financial Solutions
                Recruiter: hr@apex-fake-portal.org
                Details: Guaranteed selection without interview. Registration fee of Rs 3000 required for onboarding.
                Contact: Join Telegram channel t.me/apex_interns to complete payment.
                """;

        AnalysisRequest request = new AnalysisRequest(
                companyName,
                null,
                pastedJob
        );

        // 1. Mock SearchProvider (Octen) returning relevant search results
        SearchResultItem searchItem1 = new SearchResultItem(
                "Apex Financial MCA Registration",
                "https://zaubacorp.com/company/apex-financial",
                "Active registered private limited company in Mumbai",
                "zaubacorp.com",
                companyName + " official website"
        );
        SearchResultItem searchItem2 = new SearchResultItem(
                "Student Scam Warning: Apex Financial Onboarding Fee",
                "https://reddit.com/r/developersIndia/comments/apex_fee_scam",
                "Students charged Rs 3000 registration fee on Telegram",
                "reddit.com",
                companyName + " internship scam fake offer complaints"
        );

        when(octenSearchClient.searchAndExtract(anyString()))
                .thenReturn(List.of(searchItem1, searchItem2));
        when(octenSearchClient.searchAndExtract(anyString(), anyInt()))
                .thenReturn(List.of(searchItem1, searchItem2));

        // 2. Mock Gemini AI Client
        when(geminiClient.isConfigured()).thenReturn(true);
        String mockGeminiResponse = """
                {
                  "company_name": "Apex Financial Solutions",
                  "summary": "Entity exists on MCA registrar, but recruitment communication exhibits critical fraud indicators: fee demands and Telegram recruitment.",
                  "positive_signals": ["MCA corporate registration verified on ZaubaCorp"],
                  "risk_signals": ["Upfront registration fee demanded", "Telegram-only recruitment process", "Domain mismatch on recruiter email"],
                  "evidence_limitations": ["Limited public employee reviews on Glassdoor"],
                  "sources": ["https://zaubacorp.com/company/apex-financial", "https://reddit.com/r/developersIndia/comments/apex_fee_scam"],
                  "verdict_disclaimer": "Deterministic backend rule engine computes the final verdict."
                }
                """;
        when(geminiClient.analyze(anyString())).thenReturn(mockGeminiResponse);

        // 3. Execute request via REST API
        MvcResult mvcResult = mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value(companyName))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.recommendation").value("DO_NOT_PROCEED_UNTIL_VERIFIED"))
                .andReturn();

        AnalysisReportResponse response = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                AnalysisReportResponse.class
        );

        // 4. Verify Final Report DTO
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(companyName, response.companyName());
        assertEquals("DO_NOT_PROCEED_UNTIL_VERIFIED", response.recommendation());
        assertTrue(response.riskScore() >= 56, "Score should reflect high/critical risk due to fee demand");

        // Verify Triggered Rules in response
        assertFalse(response.triggeredRules().isEmpty());
        assertTrue(response.triggeredRules().stream().anyMatch(rf -> rf.ruleName().contains("Registration Fee") || rf.ruleId().contains("RULE_01")));

        // Verify AI summary presence
        assertNotNull(response.aiSummary());
        assertFalse(response.aiSummary().isEmpty());

        // 5. Verify Database Persistence (Evidence & ResearchReport)
        ResearchReport persistedReport = researchReportRepository.findById(response.id()).orElse(null);
        assertNotNull(persistedReport, "ResearchReport must be persisted in database");
        assertEquals(companyName, persistedReport.getCompanyName());
        assertEquals(ResearchStatus.COMPLETED, persistedReport.getStatus());
        assertEquals(response.riskScore(), persistedReport.getRiskScore());

        List<Evidence> persistedEvidence = evidenceRepository.findByResearchReportId(response.id());
        assertNotNull(persistedEvidence);
        assertEquals(2, persistedEvidence.size(), "Both unique search items must be persisted into database");

        // Verify evidence categories correctly mapped in DB
        assertTrue(persistedEvidence.stream().anyMatch(e -> e.getCategory() == EvidenceCategory.OFFICIAL_REGISTRY));
        assertTrue(persistedEvidence.stream().anyMatch(e -> e.getCategory() == EvidenceCategory.COMMUNITY_REPORT));
    }
}
