package com.internshield.research;

import com.internshield.report.ResearchService;
import com.internshield.report.ResearchStatus;
import com.internshield.report.dto.ResearchResponse;
import com.internshield.report.dto.ResearchResultUpdateRequest;
import com.internshield.search.OctenSearchClient;
import com.internshield.search.dto.SearchResultItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResearchOrchestrator {

    private final OctenSearchClient octenSearchClient;
    private final CompanyQueryGenerator companyQueryGenerator;
    private final InternshipQueryGenerator internshipQueryGenerator;
    private final SearchResultDeduplicator deduplicator;
    private final ResearchService researchService;

    public ResearchOrchestrator(
            OctenSearchClient octenSearchClient,
            CompanyQueryGenerator companyQueryGenerator,
            InternshipQueryGenerator internshipQueryGenerator,
            SearchResultDeduplicator deduplicator,
            ResearchService researchService) {
        this.octenSearchClient = octenSearchClient;
        this.companyQueryGenerator = companyQueryGenerator;
        this.internshipQueryGenerator = internshipQueryGenerator;
        this.deduplicator = deduplicator;
        this.researchService = researchService;
    }

    public List<SearchResultItem> conductResearch(Long reportId) {
        ResearchResponse report = researchService.getResearchReportById(reportId);

        // Transition to IN_PROGRESS
        researchService.updateResearchStatus(reportId, ResearchStatus.IN_PROGRESS);

        List<String> queries = new ArrayList<>();
        queries.addAll(companyQueryGenerator.generateQueries(report.companyName()));
        queries.addAll(internshipQueryGenerator.generateQueries(report.companyName(), report.internshipUrl()));

        List<SearchResultItem> collected = new ArrayList<>();
        int successfulQueries = 0;

        for (String query : queries) {
            try {
                List<SearchResultItem> results = octenSearchClient.searchAndExtract(query);
                collected.addAll(results);
                successfulQueries++;
            } catch (Exception e) {
                // Allow partial query success
            }
        }

        List<SearchResultItem> uniqueResults = deduplicator.deduplicate(collected);

        if (successfulQueries == 0 && uniqueResults.isEmpty()) {
            researchService.updateResearchStatus(reportId, ResearchStatus.FAILED);
            return List.of();
        }

        StringBuilder findings = new StringBuilder();
        findings.append("Automated research query execution completed. ")
                .append("Queried ").append(queries.size()).append(" terms, ")
                .append("retrieved ").append(uniqueResults.size()).append(" unique sources.\n\n");

        for (int i = 0; i < Math.min(uniqueResults.size(), 5); i++) {
            SearchResultItem item = uniqueResults.get(i);
            findings.append("- [").append(item.source()).append("] ").append(item.title()).append(": ")
                    .append(item.snippet()).append(" (").append(item.url()).append(")\n");
        }

        ResearchResultUpdateRequest updateRequest = new ResearchResultUpdateRequest(
                0,
                "UNKNOWN",
                findings.toString(),
                "Perform manual review of extracted search sources before committing financial deposits or documents.",
                ResearchStatus.COMPLETED
        );

        researchService.updateResearchResults(reportId, updateRequest);
        return uniqueResults;
    }
}
