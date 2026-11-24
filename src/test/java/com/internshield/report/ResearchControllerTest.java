package com.internshield.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.common.GlobalExceptionHandler;
import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.report.dto.ResearchRequest;
import com.internshield.report.dto.ResearchResponse;
import com.internshield.report.dto.ResearchResultUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResearchController.class)
@Import(GlobalExceptionHandler.class)
class ResearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResearchService researchService;

    @Test
    void testCreateResearchRequest_success() throws Exception {
        ResearchRequest request = new ResearchRequest("Acme Corp", "https://acme.example.com/careers/1");
        ResearchResponse response = new ResearchResponse(
                1L,
                "Acme Corp",
                "https://acme.example.com/careers/1",
                ResearchStatus.PENDING,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(researchService.createResearchRequest(any(ResearchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.internshipUrl").value("https://acme.example.com/careers/1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testCreateResearchRequest_validationFailure_blankCompanyName() throws Exception {
        ResearchRequest request = new ResearchRequest("", "https://acme.example.com/careers/1");

        mockMvc.perform(post("/api/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.companyName").value("Company name must not be blank"));
    }

    @Test
    void testGetAllResearchReports_success() throws Exception {
        ResearchResponse r1 = new ResearchResponse(
                1L, "Acme Corp", "https://acme.com/1", ResearchStatus.COMPLETED,
                25, "LOW", "Legitimate", "Safe to apply", LocalDateTime.now(), LocalDateTime.now()
        );
        ResearchResponse r2 = new ResearchResponse(
                2L, "Beta Inc", "https://beta.com/2", ResearchStatus.PENDING,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(researchService.getAllResearchReports()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/research"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].companyName").value("Acme Corp"))
                .andExpect(jsonPath("$[1].companyName").value("Beta Inc"));
    }

    @Test
    void testGetResearchReportById_success() throws Exception {
        ResearchResponse response = new ResearchResponse(
                1L, "Acme Corp", "https://acme.com/1", ResearchStatus.COMPLETED,
                15, "LOW", "Verified domain", "Proceed", LocalDateTime.now(), LocalDateTime.now()
        );

        when(researchService.getResearchReportById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/research/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.riskScore").value(15))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void testGetResearchReportById_notFound() throws Exception {
        when(researchService.getResearchReportById(999L))
                .thenThrow(new ResourceNotFoundException("Research report not found with id: 999"));

        mockMvc.perform(get("/api/research/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999"));
    }

    @Test
    void testGetResearchReportsByCompany_success() throws Exception {
        ResearchResponse response = new ResearchResponse(
                1L, "Acme Corp", "https://acme.com/1", ResearchStatus.COMPLETED,
                20, "LOW", "Good standing", "Safe", LocalDateTime.now(), LocalDateTime.now()
        );

        when(researchService.getResearchReportsByCompany("Acme Corp")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/research/company/Acme Corp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Acme Corp"));
    }

    @Test
    void testUpdateResearchStatus_success() throws Exception {
        ResearchResponse updated = new ResearchResponse(
                1L, "Acme Corp", "https://acme.com/1", ResearchStatus.IN_PROGRESS,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(researchService.updateResearchStatus(eq(1L), eq(ResearchStatus.IN_PROGRESS))).thenReturn(updated);

        mockMvc.perform(patch("/api/research/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResearchStatus.IN_PROGRESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void testUpdateResearchStatus_notFound() throws Exception {
        when(researchService.updateResearchStatus(eq(999L), eq(ResearchStatus.COMPLETED)))
                .thenThrow(new ResourceNotFoundException("Research report not found with id: 999"));

        mockMvc.perform(patch("/api/research/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResearchStatus.COMPLETED)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999"));
    }

    @Test
    void testUpdateResearchResults_success() throws Exception {
        ResearchResultUpdateRequest request = new ResearchResultUpdateRequest(
                20,
                "LOW",
                "Verified company domain and clean history",
                "Proceed with normal application process",
                ResearchStatus.COMPLETED
        );

        ResearchResponse response = new ResearchResponse(
                1L,
                "Acme Corp",
                "https://acme.com",
                ResearchStatus.COMPLETED,
                20,
                "LOW",
                "Verified company domain and clean history",
                "Proceed with normal application process",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(researchService.updateResearchResults(eq(1L), any(ResearchResultUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/research/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.riskScore").value(20))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testUpdateResearchResults_validationFailure_completedWithoutFindings() throws Exception {
        ResearchResultUpdateRequest request = new ResearchResultUpdateRequest(
                20,
                "LOW",
                "",
                "Recommendation",
                ResearchStatus.COMPLETED
        );

        mockMvc.perform(put("/api/research/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.findingsValidWhenCompleted").value("Findings must not be blank when status is COMPLETED"));
    }

    @Test
    void testUpdateResearchResults_validationFailure_invalidRiskScore() throws Exception {
        ResearchResultUpdateRequest request = new ResearchResultUpdateRequest(
                150,
                "LOW",
                "Findings",
                "Recommendation",
                ResearchStatus.COMPLETED
        );

        mockMvc.perform(put("/api/research/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.riskScore").value("Risk score must be at most 100"));
    }

    @Test
    void testUpdateResearchResults_validationFailure_invalidRiskLevel() throws Exception {
        ResearchResultUpdateRequest request = new ResearchResultUpdateRequest(
                50,
                "INVALID_LEVEL",
                "Findings",
                "Recommendation",
                ResearchStatus.COMPLETED
        );

        mockMvc.perform(put("/api/research/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.riskLevel").value("Risk level must be LOW, MEDIUM, HIGH, or UNKNOWN"));
    }

    @Test
    void testUpdateResearchResults_validationFailure_nullStatus() throws Exception {
        ResearchResultUpdateRequest request = new ResearchResultUpdateRequest(
                50,
                "LOW",
                "Findings",
                "Recommendation",
                null
        );

        mockMvc.perform(put("/api/research/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.status").value("Status must be provided"));
    }

    @Test
    void testUpdateResearchResults_notFound() throws Exception {
        ResearchResultUpdateRequest request = new ResearchResultUpdateRequest(
                20,
                "LOW",
                "Findings",
                "Recommendation",
                ResearchStatus.COMPLETED
        );

        when(researchService.updateResearchResults(eq(999L), any(ResearchResultUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Research report not found with id: 999"));

        mockMvc.perform(put("/api/research/999/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999"));
    }
}
