package com.internshield.research;

import com.internshield.report.ResearchService;
import com.internshield.report.ResearchStatus;
import com.internshield.report.dto.ResearchResponse;
import com.internshield.report.dto.ResearchResultUpdateRequest;
import com.internshield.search.OctenSearchClient;
import com.internshield.search.dto.SearchResultItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResearchOrchestratorTest {

    @Test
    void testConductResearch_successfulWorkflow() throws Exception {
        OctenSearchClient mockSearchClient = Mockito.mock(OctenSearchClient.class);
        ResearchService mockResearchService = Mockito.mock(ResearchService.class);
        CompanyQueryGenerator companyQueryGen = new CompanyQueryGenerator();
        InternshipQueryGenerator internshipQueryGen = new InternshipQueryGenerator();
        SearchResultDeduplicator deduplicator = new SearchResultDeduplicator();

        ResearchResponse initialReport = new ResearchResponse(
                1L,
                "Acme Corp",
                "https://acme.com/jobs/1",
                ResearchStatus.PENDING,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(mockResearchService.getResearchReportById(1L)).thenReturn(initialReport);

        SearchResultItem hit1 = new SearchResultItem("Complaint 1", "https://reviews.example.com/acme", "Charged training fees", "reviews.example.com");
        SearchResultItem hit2 = new SearchResultItem("Complaint 1 Duplicate", "https://reviews.example.com/acme/", "Charged training fees", "reviews.example.com");
        SearchResultItem hit3 = new SearchResultItem("Official Info", "https://acme.com/about", "Official company page", "acme.com");

        when(mockSearchClient.searchAndExtract(anyString()))
                .thenReturn(List.of(hit1, hit2))
                .thenReturn(List.of(hit3));

        ResearchOrchestrator orchestrator = new ResearchOrchestrator(
                mockSearchClient,
                companyQueryGen,
                internshipQueryGen,
                deduplicator,
                mockResearchService
        );

        List<SearchResultItem> results = orchestrator.conductResearch(1L);

        assertEquals(2, results.size());
        verify(mockResearchService).updateResearchStatus(1L, ResearchStatus.IN_PROGRESS);

        ArgumentCaptor<ResearchResultUpdateRequest> captor = ArgumentCaptor.forClass(ResearchResultUpdateRequest.class);
        verify(mockResearchService).updateResearchResults(eq(1L), captor.capture());

        ResearchResultUpdateRequest savedUpdate = captor.getValue();
        assertEquals(ResearchStatus.COMPLETED, savedUpdate.status());
        assertEquals("UNKNOWN", savedUpdate.riskLevel());
        assertNotNull(savedUpdate.riskScore());
        assertFalse(savedUpdate.findings().isBlank());
        assertFalse(savedUpdate.recommendation().isBlank());
    }

    @Test
    void testConductResearch_failureWhenAllSearchesFail() throws Exception {
        OctenSearchClient mockSearchClient = Mockito.mock(OctenSearchClient.class);
        ResearchService mockResearchService = Mockito.mock(ResearchService.class);
        CompanyQueryGenerator companyQueryGen = new CompanyQueryGenerator();
        InternshipQueryGenerator internshipQueryGen = new InternshipQueryGenerator();
        SearchResultDeduplicator deduplicator = new SearchResultDeduplicator();

        ResearchResponse initialReport = new ResearchResponse(
                2L,
                "Fail Corp",
                null,
                ResearchStatus.PENDING,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(mockResearchService.getResearchReportById(2L)).thenReturn(initialReport);
        when(mockSearchClient.searchAndExtract(anyString())).thenThrow(new RuntimeException("Network down"));

        ResearchOrchestrator orchestrator = new ResearchOrchestrator(
                mockSearchClient,
                companyQueryGen,
                internshipQueryGen,
                deduplicator,
                mockResearchService
        );

        List<SearchResultItem> results = orchestrator.conductResearch(2L);

        assertEquals(0, results.size());
        verify(mockResearchService).updateResearchStatus(2L, ResearchStatus.IN_PROGRESS);
        verify(mockResearchService).updateResearchStatus(2L, ResearchStatus.FAILED);
    }
}
