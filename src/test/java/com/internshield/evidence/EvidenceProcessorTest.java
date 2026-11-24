package com.internshield.evidence;

import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.report.ResearchReport;
import com.internshield.research.SearchResultDeduplicator;
import com.internshield.search.dto.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvidenceProcessorTest {

    private EvidenceRepository evidenceRepository;
    private EvidenceService evidenceService;
    private SearchResultDeduplicator deduplicator;
    private EvidenceProcessor evidenceProcessor;

    @BeforeEach
    void setUp() {
        evidenceRepository = Mockito.mock(EvidenceRepository.class);
        evidenceService = Mockito.mock(EvidenceService.class);
        deduplicator = new SearchResultDeduplicator();
        evidenceProcessor = new EvidenceProcessor(evidenceRepository, evidenceService, deduplicator);
    }

    @Test
    @DisplayName("Should correctly categorize domains into EvidenceCategory")
    void testCategorize_domains() {
        // Official Registry
        assertEquals(EvidenceCategory.OFFICIAL_REGISTRY,
                evidenceProcessor.categorize("mca.gov.in", "https://mca.gov.in/company/123", "MCA"));
        assertEquals(EvidenceCategory.OFFICIAL_REGISTRY,
                evidenceProcessor.categorize("zaubacorp.com", "https://zaubacorp.com/company", "ZaubaCorp"));

        // Review Platform
        assertEquals(EvidenceCategory.REVIEW_PLATFORM,
                evidenceProcessor.categorize("glassdoor.com", "https://glassdoor.com/Reviews", "Reviews"));
        assertEquals(EvidenceCategory.REVIEW_PLATFORM,
                evidenceProcessor.categorize("ambitionbox.com", "https://ambitionbox.com/overview", "Overview"));

        // Community Report
        assertEquals(EvidenceCategory.COMMUNITY_REPORT,
                evidenceProcessor.categorize("reddit.com", "https://reddit.com/r/scams", "Reddit Post"));
        assertEquals(EvidenceCategory.COMMUNITY_REPORT,
                evidenceProcessor.categorize("consumercomplaints.in", "https://consumercomplaints.in/acme", "Complaints"));

        // Job Board
        assertEquals(EvidenceCategory.JOB_BOARD,
                evidenceProcessor.categorize("internshala.com", "https://internshala.com/internship", "Internship"));
        assertEquals(EvidenceCategory.JOB_BOARD,
                evidenceProcessor.categorize("naukri.com", "https://naukri.com/job", "Job"));

        // News Article
        assertEquals(EvidenceCategory.NEWS_ARTICLE,
                evidenceProcessor.categorize("economictimes.indiatimes.com", "https://economictimes.com/news", "Article"));
        assertEquals(EvidenceCategory.NEWS_ARTICLE,
                evidenceProcessor.categorize("thehindu.com", "https://thehindu.com/business", "Report"));

        // Official Source (careers / about / contact)
        assertEquals(EvidenceCategory.OFFICIAL_SOURCE,
                evidenceProcessor.categorize("techcorp.com", "https://techcorp.com/careers", "Careers"));

        // Other
        assertEquals(EvidenceCategory.OTHER,
                evidenceProcessor.categorize("randomsite.xyz", "https://randomsite.xyz/page", "Random"));
    }

    @Test
    @DisplayName("Should evaluate credibility levels based on category")
    void testEvaluateCredibility() {
        assertEquals("HIGH", evidenceProcessor.evaluateCredibility(EvidenceCategory.OFFICIAL_REGISTRY, "mca.gov.in"));
        assertEquals("HIGH", evidenceProcessor.evaluateCredibility(EvidenceCategory.OFFICIAL_SOURCE, "acme.com"));
        assertEquals("HIGH", evidenceProcessor.evaluateCredibility(EvidenceCategory.NEWS_ARTICLE, "reuters.com"));
        assertEquals("MEDIUM", evidenceProcessor.evaluateCredibility(EvidenceCategory.REVIEW_PLATFORM, "glassdoor.com"));
        assertEquals("MEDIUM", evidenceProcessor.evaluateCredibility(EvidenceCategory.COMMUNITY_REPORT, "reddit.com"));
        assertEquals("LOW", evidenceProcessor.evaluateCredibility(EvidenceCategory.OTHER, "random.com"));
    }

    @Test
    @DisplayName("Should process search results, deduplicate URLs, and persist unique items")
    void testProcessAndPersistEvidence_deduplicatesAndPersists() {
        ResearchReport report = new ResearchReport();
        report.setId(42L);

        SearchResultItem item1 = new SearchResultItem(
                "Acme zaubacorp", "https://zaubacorp.com/acme", "Verified Company", "zaubacorp.com", "Acme registration");
        SearchResultItem item1Duplicate = new SearchResultItem(
                "Acme zaubacorp duplicate", "https://zaubacorp.com/acme/", "Duplicate snippet", "zaubacorp.com", "Acme registration");
        SearchResultItem item2 = new SearchResultItem(
                "Glassdoor reviews", "https://glassdoor.com/acme", "Good culture", "glassdoor.com", "Acme reviews");

        when(evidenceRepository.existsBySourceUrlAndResearchReportId("https://zaubacorp.com/acme", 42L)).thenReturn(false);
        when(evidenceRepository.existsBySourceUrlAndResearchReportId("https://glassdoor.com/acme", 42L)).thenReturn(false);

        Evidence savedEvidence1 = new Evidence();
        savedEvidence1.setId(101L);
        savedEvidence1.setSourceUrl("https://zaubacorp.com/acme");

        Evidence savedEvidence2 = new Evidence();
        savedEvidence2.setId(102L);
        savedEvidence2.setSourceUrl("https://glassdoor.com/acme");

        when(evidenceRepository.save(any(Evidence.class)))
                .thenReturn(savedEvidence1)
                .thenReturn(savedEvidence2);

        List<Evidence> result = evidenceProcessor.processAndPersistEvidence(List.of(item1, item1Duplicate, item2), report);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(evidenceRepository, Mockito.times(2)).save(any(Evidence.class));
    }

    @Test
    @DisplayName("Should skip evidence if already exists for this research report")
    void testProcessAndPersistEvidence_skipsExistingUrl() {
        ResearchReport report = new ResearchReport();
        report.setId(99L);

        SearchResultItem item = new SearchResultItem("Title", "https://existing.com/page", "Snippet", "existing.com", "query");

        when(evidenceRepository.existsBySourceUrlAndResearchReportId("https://existing.com/page", 99L)).thenReturn(true);

        List<Evidence> result = evidenceProcessor.processAndPersistEvidence(List.of(item), report);

        assertTrue(result.isEmpty());
        verify(evidenceService, never()).createEvidence(any(EvidenceRequest.class));
    }
}
