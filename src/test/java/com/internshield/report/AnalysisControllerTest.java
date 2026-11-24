package com.internshield.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.common.GlobalExceptionHandler;
import com.internshield.evidence.EvidenceRepository;
import com.internshield.internship.InternshipListingExtractor;
import com.internshield.internship.dto.ExtractedInternshipDetails;
import com.internshield.report.dto.AnalysisReportResponse;
import com.internshield.report.dto.AnalysisRequest;
import com.internshield.research.ResearchOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
@Import(GlobalExceptionHandler.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResearchOrchestrator orchestrator;

    @MockBean
    private ResearchReportRepository researchReportRepository;

    @MockBean
    private EvidenceRepository evidenceRepository;

    @MockBean
    private InternshipListingExtractor listingExtractor;

    @Test
    @DisplayName("POST /api/analysis - Should return 201 Created and analysis report on valid request")
    void testAnalyzeInternship_success() throws Exception {
        AnalysisRequest request = new AnalysisRequest(
                "Acme Global",
                "https://acme.example.com/jobs/intern",
                "Python Developer Intern. Training fee Rs 2000."
        );

        com.internshield.analysis.RiskRuleViolation violation = new com.internshield.analysis.RiskRuleViolation(
                "RULE_01_REG_FEE",
                "Registration Fee Demanded",
                "FINANCIAL_DEMAND",
                45,
                "CRITICAL",
                "Registration fee of Rs 2000",
                List.of("registration fee")
        );

        AnalysisReportResponse response = new AnalysisReportResponse(
                1L,
                "Acme Global",
                "https://acme.example.com/jobs/intern",
                "COMPLETED",
                55,
                "HIGH",
                "DO_NOT_PROCEED_UNTIL_VERIFIED",
                "Critical risk indicators detected: registration fee demanded.",
                "AI summary of risk signals",
                List.of(),
                List.of("Registration fee requested"),
                List.of(),
                List.of(violation),
                Collections.emptyList(),
                ExtractedInternshipDetails.empty(),
                "Findings summary",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orchestrator.executeCompleteAnalysis(any(AnalysisRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Acme Global"))
                .andExpect(jsonPath("$.riskScore").value(55))
                .andExpect(jsonPath("$.recommendation").value("DO_NOT_PROCEED_UNTIL_VERIFIED"));
    }

    @Test
    @DisplayName("POST /api/analysis - Should return 400 Bad Request when company name is blank")
    void testAnalyzeInternship_blankCompanyNameFailsValidation() throws Exception {
        AnalysisRequest invalidRequest = new AnalysisRequest(
                "",
                "https://acme.example.com/jobs/intern",
                "Description"
        );

        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/analysis/{id} - Should return 200 OK and report when found")
    void testGetAnalysisById_found() throws Exception {
        ResearchReport report = new ResearchReport();
        report.setId(5L);
        report.setCompanyName("Alpha Corp");
        report.setInternshipUrl("https://alphacorp.com/careers");
        report.setStatus(ResearchStatus.COMPLETED);
        report.setRiskScore(20);
        report.setRiskLevel("LOW");
        report.setRecommendation("PROCEED_WITH_NORMAL_PRECAUTIONS: No high-risk indicators.");
        report.setFindings("Company verified on MCA.");
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        when(researchReportRepository.findById(5L)).thenReturn(Optional.of(report));
        when(evidenceRepository.findByResearchReportId(5L)).thenReturn(Collections.emptyList());
        when(listingExtractor.extractFromUrl("https://alphacorp.com/careers")).thenReturn(ExtractedInternshipDetails.empty());

        mockMvc.perform(get("/api/analysis/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.companyName").value("Alpha Corp"))
                .andExpect(jsonPath("$.riskScore").value(20))
                .andExpect(jsonPath("$.recommendation").value("PROCEED_WITH_NORMAL_PRECAUTIONS"));
    }

    @Test
    @DisplayName("GET /api/analysis/{id} - Should return 404 Not Found when report does not exist")
    void testGetAnalysisById_notFound() throws Exception {
        when(researchReportRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/analysis/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/analysis - Should return 200 OK and list of analysis reports")
    void testGetAllReports() throws Exception {
        ResearchReport r1 = new ResearchReport();
        r1.setId(10L);
        r1.setCompanyName("Company 1");
        r1.setStatus(ResearchStatus.COMPLETED);
        r1.setRiskScore(10);
        r1.setCreatedAt(LocalDateTime.now());

        when(researchReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].companyName").value("Company 1"));
    }
}
