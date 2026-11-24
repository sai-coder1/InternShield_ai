
package com.internshield;

import com.internshield.search.OctenSearchClient;
import com.internshield.research.Evidence;
import com.internshield.research.EvidenceProcessor;
import com.internshield.research.EvidenceJsonExporter;
import com.internshield.research.SearchQueryGenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.internshield.ai.GeminiClient;

import java.util.ArrayList;
import java.util.List;

public class App {

    public static void main(String[] args) {

        try {

            // 1. Create objects
            SearchQueryGenerator generator = new SearchQueryGenerator();

            OctenSearchClient client = new OctenSearchClient();

            EvidenceProcessor processor = new EvidenceProcessor();

            EvidenceJsonExporter exporter = new EvidenceJsonExporter();
            GeminiClient geminiClient = new GeminiClient();

            // 2. Define company details
            String companyName = "ABC Technologies";
            String internshipRole = "Java Developer";

            // 3. Generate search queries
            List<String> queries = generator.generateQueries(
                    companyName,
                    internshipRole);

            // 4. Collect evidence from all searches
            List<Evidence> allEvidence = new ArrayList<>();

            for (int i = 0; i < queries.size(); i++) {

                String query = queries.get(i);

                System.out.println("\n================================");
                System.out.println("SEARCH " + (i + 1));
                System.out.println("Query: " + query);
                System.out.println("================================\n");

                JsonNode results = client.search(query);

                List<Evidence> evidenceList = processor.extractEvidence(results, query);

                allEvidence.addAll(evidenceList);
            }

            // 5. Display total evidence
            System.out.println("\n========== EVIDENCE SUMMARY ==========");

            System.out.println("Total results collected: "
                    + allEvidence.size());

            // 6. Remove duplicate URLs
            List<Evidence> uniqueEvidence = processor.removeDuplicates(allEvidence);

            System.out.println("Unique results after removing duplicates: "
                    + uniqueEvidence.size());

            // 7. Filter relevant evidence
            List<Evidence> relevantEvidence = processor.filterRelevantEvidence(
                    uniqueEvidence,
                    companyName);

            System.out.println("Relevant results after filtering: "
                    + relevantEvidence.size());

            // 8. Display relevant evidence
            for (Evidence evidence : relevantEvidence) {

                System.out.println("\nTitle: " + evidence.title());
                System.out.println("URL: " + evidence.url());
                System.out.println("Highlight: " + evidence.highlight());
            }

            // 9. Export evidence as structured JSON
            String jsonOutput = exporter.export(
                    companyName,
                    relevantEvidence);

            System.out.println(
                    "\n========== STRUCTURED EVIDENCE JSON ==========");

            System.out.println(jsonOutput);

            // 10. Send evidence JSON to Gemini
            String aiAnalysis = geminiClient.analyze(jsonOutput);

            // 11. Display Gemini's analysis
            System.out.println(
                    "\n========== GEMINI AI ANALYSIS ==========");

            System.out.println(aiAnalysis);

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
