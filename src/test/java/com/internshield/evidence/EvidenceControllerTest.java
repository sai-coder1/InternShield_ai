package com.internshield.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internshield.common.GlobalExceptionHandler;
import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvidenceController.class)
@Import(GlobalExceptionHandler.class)
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvidenceService evidenceService;

    @Test
    void testCreateEvidence_success() throws Exception {
        EvidenceRequest request = new EvidenceRequest(
                "Glassdoor Review",
                "https://glassdoor.example.com/review/1",
                "Review warning of deposit fees",
                "glassdoor.example.com",
                EvidenceCategory.REVIEW_PLATFORM,
                "HIGH",
                1L
        );

        EvidenceResponse response = new EvidenceResponse(
                10L,
                "Glassdoor Review",
                "https://glassdoor.example.com/review/1",
                "Review warning of deposit fees",
                "glassdoor.example.com",
                EvidenceCategory.REVIEW_PLATFORM,
                "HIGH",
                LocalDateTime.now(),
                1L
        );

        when(evidenceService.createEvidence(any(EvidenceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Glassdoor Review"))
                .andExpect(jsonPath("$.sourceUrl").value("https://glassdoor.example.com/review/1"))
                .andExpect(jsonPath("$.category").value("REVIEW_PLATFORM"))
                .andExpect(jsonPath("$.researchReportId").value(1));
    }

    @Test
    void testCreateEvidence_validationFailure_blankTitle() throws Exception {
        EvidenceRequest request = new EvidenceRequest(
                "",
                "https://glassdoor.example.com/review/1",
                "Description",
                "glassdoor.example.com",
                EvidenceCategory.REVIEW_PLATFORM,
                "HIGH",
                1L
        );

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.title").value("Title must not be blank"));
    }

    @Test
    void testCreateEvidence_validationFailure_invalidUrl() throws Exception {
        EvidenceRequest request = new EvidenceRequest(
                "Title",
                "not-a-valid-url",
                "Description",
                "domain",
                EvidenceCategory.OTHER,
                "LOW",
                1L
        );

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.sourceUrl").value("Source URL must be a valid URL"));
    }

    @Test
    void testCreateEvidence_duplicateUrl() throws Exception {
        EvidenceRequest request = new EvidenceRequest(
                "Duplicate",
                "https://example.com/dup",
                "Description",
                "example.com",
                EvidenceCategory.OTHER,
                "LOW",
                1L
        );

        when(evidenceService.createEvidence(any(EvidenceRequest.class)))
                .thenThrow(new IllegalArgumentException("Evidence with source URL already exists for research report: https://example.com/dup"));

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Evidence with source URL already exists for research report: https://example.com/dup"));
    }

    @Test
    void testCreateEvidence_reportNotFound() throws Exception {
        EvidenceRequest request = new EvidenceRequest(
                "Title",
                "https://example.com/test",
                "Description",
                "example.com",
                EvidenceCategory.OTHER,
                "LOW",
                999L
        );

        when(evidenceService.createEvidence(any(EvidenceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Research report not found with id: 999"));

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999"));
    }

    @Test
    void testGetEvidenceById_success() throws Exception {
        EvidenceResponse response = new EvidenceResponse(
                10L,
                "MCA Registry Check",
                "https://mca.gov.in/123",
                "Verified company record",
                "mca.gov.in",
                EvidenceCategory.OFFICIAL_REGISTRY,
                "HIGH",
                LocalDateTime.now(),
                1L
        );

        when(evidenceService.getEvidenceById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/evidence/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("MCA Registry Check"))
                .andExpect(jsonPath("$.category").value("OFFICIAL_REGISTRY"));
    }

    @Test
    void testGetEvidenceById_notFound() throws Exception {
        when(evidenceService.getEvidenceById(999L))
                .thenThrow(new ResourceNotFoundException("Evidence not found with id: 999"));

        mockMvc.perform(get("/api/evidence/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found with id: 999"));
    }

    @Test
    void testGetEvidenceByReportId_success() throws Exception {
        EvidenceResponse e1 = new EvidenceResponse(1L, "T1", "https://e.com/1", "D1", "e.com", EvidenceCategory.NEWS_ARTICLE, "MED", LocalDateTime.now(), 1L);
        EvidenceResponse e2 = new EvidenceResponse(2L, "T2", "https://e.com/2", "D2", "e.com", EvidenceCategory.JOB_BOARD, "LOW", LocalDateTime.now(), 1L);

        when(evidenceService.getEvidenceByReportId(1L)).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/api/evidence/report/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("T1"))
                .andExpect(jsonPath("$[1].title").value("T2"));
    }

    @Test
    void testGetEvidenceByReportId_reportNotFound() throws Exception {
        when(evidenceService.getEvidenceByReportId(999L))
                .thenThrow(new ResourceNotFoundException("Research report not found with id: 999"));

        mockMvc.perform(get("/api/evidence/report/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Research report not found with id: 999"));
    }

    @Test
    void testGetEvidenceByCategory_success() throws Exception {
        EvidenceResponse e1 = new EvidenceResponse(1L, "Official Filing", "https://gov.in/1", "Filing details", "gov.in", EvidenceCategory.OFFICIAL_SOURCE, "HIGH", LocalDateTime.now(), 1L);

        when(evidenceService.getEvidenceByCategory(1L, EvidenceCategory.OFFICIAL_SOURCE)).thenReturn(List.of(e1));

        mockMvc.perform(get("/api/evidence/report/1/category/OFFICIAL_SOURCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("OFFICIAL_SOURCE"));
    }

    @Test
    void testGetEvidenceCount_success() throws Exception {
        when(evidenceService.getEvidenceCount(1L)).thenReturn(7L);

        mockMvc.perform(get("/api/evidence/report/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void testDeleteEvidence_success() throws Exception {
        doNothing().when(evidenceService).deleteEvidence(10L);

        mockMvc.perform(delete("/api/evidence/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteEvidence_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Evidence not found with id: 999"))
                .when(evidenceService).deleteEvidence(999L);

        mockMvc.perform(delete("/api/evidence/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence not found with id: 999"));
    }
}
