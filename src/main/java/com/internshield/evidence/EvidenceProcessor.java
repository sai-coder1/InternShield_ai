package com.internshield.evidence;

import com.internshield.evidence.dto.EvidenceRequest;
import com.internshield.evidence.dto.EvidenceResponse;
import com.internshield.report.ResearchReport;
import com.internshield.research.SearchResultDeduplicator;
import com.internshield.search.dto.SearchResultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class EvidenceProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvidenceProcessor.class);

    private final EvidenceRepository evidenceRepository;
    private final EvidenceService evidenceService;
    private final SearchResultDeduplicator deduplicator;

    public EvidenceProcessor(
            EvidenceRepository evidenceRepository,
            EvidenceService evidenceService,
            SearchResultDeduplicator deduplicator) {
        this.evidenceRepository = evidenceRepository;
        this.evidenceService = evidenceService;
        this.deduplicator = deduplicator;
    }

    /**
     * Categorizes a search result item into the proper EvidenceCategory and evaluates source credibility.
     */
    public EvidenceCategory categorize(String domain, String url, String title) {
        String lowerDomain = (domain != null) ? domain.toLowerCase(Locale.ROOT) : "";
        String lowerUrl = (url != null) ? url.toLowerCase(Locale.ROOT) : "";

        if (lowerDomain.endsWith(".gov") || lowerDomain.endsWith(".gov.in")
                || lowerDomain.contains("mca.gov.in") || lowerDomain.contains("zaubacorp.com")
                || lowerDomain.contains("tofler.in") || lowerDomain.contains("sec.gov")) {
            return EvidenceCategory.OFFICIAL_REGISTRY;
        }

        if (lowerDomain.contains("glassdoor") || lowerDomain.contains("ambitionbox")
                || lowerDomain.contains("indeed") || lowerDomain.contains("trustpilot")
                || lowerDomain.contains("mouthshut")) {
            return EvidenceCategory.REVIEW_PLATFORM;
        }

        if (lowerDomain.contains("reddit.com") || lowerDomain.contains("quora.com")
                || lowerDomain.contains("consumercomplaints") || lowerDomain.contains("complaint")
                || lowerDomain.contains("scamadviser") || lowerDomain.contains("scampulse")
                || lowerDomain.contains("voxya")) {
            return EvidenceCategory.COMMUNITY_REPORT;
        }

        if (lowerDomain.contains("internshala") || lowerDomain.contains("naukri")
                || lowerDomain.contains("unstop") || lowerDomain.contains("wellfound")
                || lowerDomain.contains("monster") || lowerDomain.contains("foundit")
                || (lowerDomain.contains("linkedin.com") && lowerUrl.contains("/jobs/"))) {
            return EvidenceCategory.JOB_BOARD;
        }

        if (lowerDomain.contains("techcrunch") || lowerDomain.contains("economictimes")
                || lowerDomain.contains("livemint") || lowerDomain.contains("ndtv")
                || lowerDomain.contains("hindustantimes") || lowerDomain.contains("forbes")
                || lowerDomain.contains("reuters") || lowerDomain.contains("bloomberg")
                || lowerDomain.contains("timesofindia") || lowerDomain.contains("thehindu")) {
            return EvidenceCategory.NEWS_ARTICLE;
        }

        if (lowerUrl.contains("/about") || lowerUrl.contains("/career")
                || lowerUrl.contains("/contact") || lowerUrl.contains("/terms")
                || lowerUrl.contains("/privacy")) {
            return EvidenceCategory.OFFICIAL_SOURCE;
        }

        return EvidenceCategory.OTHER;
    }

    /**
     * Determines credibility level (HIGH, MEDIUM, LOW) based on category and domain authority.
     */
    public String evaluateCredibility(EvidenceCategory category, String domain) {
        if (category == EvidenceCategory.OFFICIAL_REGISTRY || category == EvidenceCategory.NEWS_ARTICLE) {
            return "HIGH";
        }
        if (category == EvidenceCategory.OFFICIAL_SOURCE) {
            return "HIGH";
        }
        if (category == EvidenceCategory.REVIEW_PLATFORM || category == EvidenceCategory.JOB_BOARD) {
            return "MEDIUM";
        }
        if (category == EvidenceCategory.COMMUNITY_REPORT) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * Deduplicates, filters, and persists search results into database Evidence records associated with a ResearchReport.
     * Prevents duplicate URLs within the same research report.
     *
     * @param items list of search items
     * @param report the parent research report
     * @return list of newly created or retrieved Evidence entities
     */
    public List<Evidence> processAndPersistEvidence(List<SearchResultItem> items, ResearchReport report) {
        if (items == null || items.isEmpty() || report == null) {
            return List.of();
        }

        List<SearchResultItem> uniqueItems = deduplicator.deduplicate(items);
        List<Evidence> persistedList = new ArrayList<>();

        for (SearchResultItem item : uniqueItems) {
            if (item == null || item.url() == null || item.url().isBlank()) {
                continue;
            }

            String url = item.url().trim();
            // Protect against malformed or non-http protocols
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                continue;
            }

            // Check if already persisted for this report
            if (evidenceRepository.existsBySourceUrlAndResearchReportId(url, report.getId())) {
                log.debug("Evidence with URL {} already exists for report id {}", url, report.getId());
                continue;
            }

            String domain = item.source();
            if (domain == null || domain.isBlank() || "unknown".equalsIgnoreCase(domain) || "web".equalsIgnoreCase(domain)) {
                domain = extractDomain(url);
            }

            String title = (item.title() != null && !item.title().isBlank())
                    ? item.title().trim()
                    : "Evidence from " + domain;

            // Retain snippet and query origin in description
            StringBuilder descriptionBuilder = new StringBuilder();
            if (item.snippet() != null && !item.snippet().isBlank()) {
                descriptionBuilder.append(item.snippet().trim());
            }
            if (item.query() != null && !item.query().isBlank()) {
                if (descriptionBuilder.length() > 0) {
                    descriptionBuilder.append(" ");
                }
                descriptionBuilder.append("[Query: ").append(item.query().trim()).append("]");
            }

            EvidenceCategory category = categorize(domain, url, title);
            String credibility = evaluateCredibility(category, domain);

            Evidence evidence = new Evidence(
                    title,
                    url,
                    descriptionBuilder.toString(),
                    domain,
                    category,
                    credibility,
                    report
            );

            try {
                Evidence saved = evidenceRepository.save(evidence);
                persistedList.add(saved);
            } catch (Exception e) {
                log.warn("Failed to persist evidence for URL {}: {}", url, e.getMessage());
            }
        }

        return persistedList;
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {
        }
        return "web";
    }
}
