package com.internshield.evidence;

import com.internshield.report.ResearchReport;
import com.internshield.report.ResearchReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class EvidenceRepositoryTest {

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private ResearchReportRepository researchReportRepository;

    private ResearchReport report1;
    private ResearchReport report2;

    @BeforeEach
    void setUp() {
        report1 = researchReportRepository.save(new ResearchReport("Acme Corp", "https://acme.com/jobs/1"));
        report2 = researchReportRepository.save(new ResearchReport("Beta Corp", "https://beta.com/jobs/2"));
    }

    @Test
    void testSaveEvidenceRecordSuccessfully() {
        Evidence evidence = new Evidence(
                "Glassdoor Review",
                "https://glassdoor.example.com/acme-reviews",
                "Review warning about upfront training fees",
                "glassdoor.example.com",
                EvidenceCategory.REVIEW_PLATFORM,
                "HIGH",
                report1
        );

        Evidence saved = evidenceRepository.save(evidence);
        assertNotNull(saved.getId());
        assertEquals("Glassdoor Review", saved.getTitle());
        assertEquals(report1.getId(), saved.getResearchReport().getId());
        assertNotNull(saved.getCollectedAt());
    }

    @Test
    void testRetrieveEvidenceByResearchReportId() {
        evidenceRepository.save(new Evidence("Source 1", "https://example.com/1", "Desc 1", "example.com", EvidenceCategory.COMMUNITY_REPORT, "MEDIUM", report1));
        evidenceRepository.save(new Evidence("Source 2", "https://example.com/2", "Desc 2", "example.com", EvidenceCategory.NEWS_ARTICLE, "HIGH", report1));

        List<Evidence> results = evidenceRepository.findByResearchReportId(report1.getId());
        assertEquals(2, results.size());
    }

    @Test
    void testRetrieveEvidenceByIdAndResearchReportId() {
        Evidence saved = evidenceRepository.save(new Evidence("Source 1", "https://example.com/1", "Desc 1", "example.com", EvidenceCategory.OFFICIAL_REGISTRY, "HIGH", report1));

        Optional<Evidence> found = evidenceRepository.findByIdAndResearchReportId(saved.getId(), report1.getId());
        assertTrue(found.isPresent());
        assertEquals("Source 1", found.get().getTitle());

        Optional<Evidence> notFoundWrongReport = evidenceRepository.findByIdAndResearchReportId(saved.getId(), report2.getId());
        assertFalse(notFoundWrongReport.isPresent());
    }

    @Test
    void testCheckDuplicateSourceUrlExistence() {
        evidenceRepository.save(new Evidence("Source 1", "https://example.com/check-dup", "Desc", "example.com", EvidenceCategory.COMMUNITY_REPORT, "LOW", report1));

        assertTrue(evidenceRepository.existsBySourceUrlAndResearchReportId("https://example.com/check-dup", report1.getId()));
        assertFalse(evidenceRepository.existsBySourceUrlAndResearchReportId("https://example.com/check-dup", report2.getId()));
        assertFalse(evidenceRepository.existsBySourceUrlAndResearchReportId("https://example.com/non-existent", report1.getId()));
    }

    @Test
    void testCountEvidenceRecordsForReport() {
        evidenceRepository.save(new Evidence("E1", "https://example.com/e1", "Desc", "example.com", EvidenceCategory.JOB_BOARD, "MEDIUM", report1));
        evidenceRepository.save(new Evidence("E2", "https://example.com/e2", "Desc", "example.com", EvidenceCategory.OTHER, "LOW", report1));
        evidenceRepository.save(new Evidence("E3", "https://example.com/e3", "Desc", "example.com", EvidenceCategory.NEWS_ARTICLE, "HIGH", report1));

        assertEquals(3, evidenceRepository.countByResearchReportId(report1.getId()));
        assertEquals(0, evidenceRepository.countByResearchReportId(report2.getId()));
    }

    @Test
    void testFilterEvidenceByCategory() {
        evidenceRepository.save(new Evidence("Registry Check", "https://mca.gov.in/check", "Official company filing", "mca.gov.in", EvidenceCategory.OFFICIAL_REGISTRY, "VERIFIED", report1));
        evidenceRepository.save(new Evidence("Reddit Post", "https://reddit.com/r/scams/acme", "Reddit student complaint", "reddit.com", EvidenceCategory.COMMUNITY_REPORT, "MEDIUM", report1));

        List<Evidence> registryEvidence = evidenceRepository.findByCategoryAndResearchReportId(EvidenceCategory.OFFICIAL_REGISTRY, report1.getId());
        assertEquals(1, registryEvidence.size());
        assertEquals("Registry Check", registryEvidence.get(0).getTitle());

        List<Evidence> communityEvidence = evidenceRepository.findByCategoryAndResearchReportId(EvidenceCategory.COMMUNITY_REPORT, report1.getId());
        assertEquals(1, communityEvidence.size());
        assertEquals("Reddit Post", communityEvidence.get(0).getTitle());
    }

    @Test
    void testVerifyEvidenceBelongingToOneReportIsNotReturnedForAnotherReport() {
        Evidence evReport1 = evidenceRepository.save(new Evidence("Report 1 Source", "https://source1.example.com", "D1", "source1.example.com", EvidenceCategory.COMMUNITY_REPORT, "MED", report1));
        Evidence evReport2 = evidenceRepository.save(new Evidence("Report 2 Source", "https://source2.example.com", "D2", "source2.example.com", EvidenceCategory.COMMUNITY_REPORT, "MED", report2));

        List<Evidence> report1List = evidenceRepository.findByResearchReportId(report1.getId());
        List<Evidence> report2List = evidenceRepository.findByResearchReportId(report2.getId());

        assertEquals(1, report1List.size());
        assertEquals(evReport1.getId(), report1List.get(0).getId());

        assertEquals(1, report2List.size());
        assertEquals(evReport2.getId(), report2List.get(0).getId());
    }
}
