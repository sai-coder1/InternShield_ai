package com.internshield.evidence;

import com.internshield.common.exception.ResourceNotFoundException;
import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.report.ResearchReport;
import com.internshield.report.ResearchReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private ResearchReportRepository researchReportRepository;

    @InjectMocks
    private EvidenceService evidenceService;

    private ResearchReport sampleReport;
    private Evidence sampleEvidence;

    @BeforeEach
    void setUp() {
        sampleReport = new ResearchReport("Acme Corp", "https://acme.com/jobs/1");
        sampleReport.setId(1L);

        sampleEvidence = new Evidence(
                "MCA Registry Check",
                "https://mca.gov.in/company/123",
                "Registered entity in good standing",
                "mca.gov.in",
                EvidenceCategory.OFFICIAL_REGISTRY,
                "HIGH",
                sampleReport
        );
        sampleEvidence.setId(10L);
        sampleEvidence.setCollectedAt(LocalDateTime.now());
    }

    @Test
    void testCreateEvidence_success() {
        EvidenceRequest request = new EvidenceRequest(
                "MCA Registry Check",
                "https://mca.gov.in/company/123",
                "Registered entity in good standing",
                "mca.gov.in",
                EvidenceCategory.OFFICIAL_REGISTRY,
                "HIGH",
                1L
        );

        when(researchReportRepository.findById(1L)).thenReturn(Optional.of(sampleReport));
        when(evidenceRepository.existsBySourceUrlAndResearchReportId("https://mca.gov.in/company/123", 1L)).thenReturn(false);
        when(evidenceRepository.save(any(Evidence.class))).thenReturn(sampleEvidence);

        EvidenceResponse response = evidenceService.createEvidence(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("MCA Registry Check", response.title());
        assertEquals(1L, response.researchReportId());
        verify(evidenceRepository).save(any(Evidence.class));
    }

    @Test
    void testCreateEvidence_researchReportNotFound() {
        EvidenceRequest request = new EvidenceRequest(
                "MCA Registry Check",
                "https://mca.gov.in/company/123",
                "Registered entity in good standing",
                "mca.gov.in",
                EvidenceCategory.OFFICIAL_REGISTRY,
                "HIGH",
                999L
        );

        when(researchReportRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evidenceService.createEvidence(request));
        assertEquals("Research report not found with id: 999", ex.getMessage());
    }

    @Test
    void testCreateEvidence_duplicateUrlRejected() {
        EvidenceRequest request = new EvidenceRequest(
                "MCA Registry Check",
                "https://mca.gov.in/company/123",
                "Registered entity in good standing",
                "mca.gov.in",
                EvidenceCategory.OFFICIAL_REGISTRY,
                "HIGH",
                1L
        );

        when(researchReportRepository.findById(1L)).thenReturn(Optional.of(sampleReport));
        when(evidenceRepository.existsBySourceUrlAndResearchReportId("https://mca.gov.in/company/123", 1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> evidenceService.createEvidence(request));
        assertEquals("Evidence with source URL already exists for research report: https://mca.gov.in/company/123", ex.getMessage());
    }

    @Test
    void testGetEvidenceById_success() {
        when(evidenceRepository.findById(10L)).thenReturn(Optional.of(sampleEvidence));

        EvidenceResponse response = evidenceService.getEvidenceById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("MCA Registry Check", response.title());
    }

    @Test
    void testGetEvidenceById_notFound() {
        when(evidenceRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evidenceService.getEvidenceById(999L));
        assertEquals("Evidence not found with id: 999", ex.getMessage());
    }

    @Test
    void testGetEvidenceByReportId_success() {
        when(researchReportRepository.existsById(1L)).thenReturn(true);
        when(evidenceRepository.findByResearchReportId(1L)).thenReturn(List.of(sampleEvidence));

        List<EvidenceResponse> responses = evidenceService.getEvidenceByReportId(1L);

        assertEquals(1, responses.size());
        assertEquals("MCA Registry Check", responses.get(0).title());
    }

    @Test
    void testGetEvidenceByCategory_success() {
        when(researchReportRepository.existsById(1L)).thenReturn(true);
        when(evidenceRepository.findByCategoryAndResearchReportId(EvidenceCategory.OFFICIAL_REGISTRY, 1L))
                .thenReturn(List.of(sampleEvidence));

        List<EvidenceResponse> responses = evidenceService.getEvidenceByCategory(1L, EvidenceCategory.OFFICIAL_REGISTRY);

        assertEquals(1, responses.size());
        assertEquals(EvidenceCategory.OFFICIAL_REGISTRY, responses.get(0).category());
    }

    @Test
    void testGetEvidenceCount_success() {
        when(researchReportRepository.existsById(1L)).thenReturn(true);
        when(evidenceRepository.countByResearchReportId(1L)).thenReturn(5L);

        long count = evidenceService.getEvidenceCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void testDeleteEvidence_success() {
        when(evidenceRepository.findById(10L)).thenReturn(Optional.of(sampleEvidence));

        evidenceService.deleteEvidence(10L);

        verify(evidenceRepository).delete(sampleEvidence);
    }

    @Test
    void testDeleteEvidence_notFound() {
        when(evidenceRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evidenceService.deleteEvidence(999L));
        assertEquals("Evidence not found with id: 999", ex.getMessage());
    }
}
