package com.internshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.internshield.research.CompanyQueryGenerator;
import com.internshield.research.InternshipQueryGenerator;
import com.internshield.research.SearchResultDeduplicator;
import com.internshield.search.OctenSearchClient;
import com.internshield.search.dto.SearchResultItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone demonstration and CLI runner for InternShield search and verification research.
 * <p>
 * Note: The primary Spring Boot entry point for the web application is
 * {@link InternShieldApplication}, annotated with {@code @SpringBootApplication}.
 * <p>
 * This class preserves standalone CLI execution capability from {@code main} while
 * combining the multi-query research and URL deduplication capabilities from {@code backend}.
 * Downstream AI risk assessment (PR #3) can hook into this pipeline once merged.
 */
public class App {

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("       InternShield AI - Standalone Research Demo    ");
        System.out.println("====================================================\n");

        String companyName = args.length > 0 ? args[0] : "ABC Technologies";
        String internshipRole = args.length > 1 ? args[1] : "Java Developer";

        runDemo(companyName, internshipRole);
    }

    /**
     * Executes the default demonstration query against Octen search.
     * Preserved for backward compatibility with backend demonstration utilities.
     */
    public static void runDemo() {
        runDemo("ABC Technologies", "Java Developer");
    }

    /**
     * Executes a multi-query research demonstration for a target company and internship role:
     * 1. Generates targeted verification search queries.
     * 2. Searches Octen for evidence.
     * 3. Deduplicates search result URLs.
     * 4. Displays formatted research evidence.
     *
     * @param companyName    Name of the company to investigate
     * @param internshipRole Role or position being evaluated
     */
    public static void runDemo(String companyName, String internshipRole) {
        try {
            System.out.println("Investigating Company: " + companyName);
            System.out.println("Internship Role:      " + internshipRole);

            // 1. Initialize research components
            CompanyQueryGenerator companyQueryGen = new CompanyQueryGenerator();
            InternshipQueryGenerator internshipQueryGen = new InternshipQueryGenerator();
            SearchResultDeduplicator deduplicator = new SearchResultDeduplicator();
            OctenSearchClient client = new OctenSearchClient();

            // 2. Generate multi-angle search queries
            List<String> queries = new ArrayList<>();
            queries.addAll(companyQueryGen.generateQueries(companyName));
            queries.addAll(internshipQueryGen.generateQueries(companyName, ""));

            System.out.println("\nGenerated " + queries.size() + " verification queries.");

            // 3. Collect evidence across queries
            List<SearchResultItem> allEvidence = new ArrayList<>();

            for (int i = 0; i < queries.size(); i++) {
                String query = queries.get(i);
                System.out.println("\n--------------------------------");
                System.out.println("SEARCH " + (i + 1) + ": " + query);
                System.out.println("--------------------------------");

                try {
                    List<SearchResultItem> items = client.searchAndExtract(query);
                    allEvidence.addAll(items);
                    System.out.println("Found " + items.size() + " results.");
                } catch (Exception ex) {
                    System.out.println("Search query notice: " + ex.getMessage());
                }
            }

            // 4. Evidence summary and URL deduplication
            System.out.println("\n========== EVIDENCE SUMMARY ==========");
            System.out.println("Total results collected: " + allEvidence.size());

            List<SearchResultItem> uniqueEvidence = deduplicator.deduplicate(allEvidence);
            System.out.println("Unique results after deduplication: " + uniqueEvidence.size());

            // 5. Display structured evidence
            System.out.println("\n========== RELEVANT EVIDENCE ==========");
            for (SearchResultItem item : uniqueEvidence) {
                System.out.println("\nTitle:   " + item.title());
                System.out.println("URL:     " + item.url());
                System.out.println("Source:  " + item.source());
                System.out.println("Snippet: " + item.snippet());
            }

            // Downstream AI hook note:
            // When PR #3 (ai-backend-integration) is merged, the AI risk assessment engine
            // (e.g., GeminiAnalysisService / RiskAssessmentEngine) can analyze uniqueEvidence here.

        } catch (Exception e) {
            System.out.println("Error running demonstration: " + e.getMessage());
        }
    }
}
