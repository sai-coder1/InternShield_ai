
package com.internshield.research;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class EvidenceProcessor {

    public List<Evidence> extractEvidence(
            JsonNode results,
            String searchQuery) {

        List<Evidence> evidenceList = new ArrayList<>();

        for (JsonNode result : results) {

            String title = result.path("title").asText("");
            String url = result.path("url").asText("");
            String highlight = result.path("highlight").asText("");

            Evidence evidence = new Evidence(
                    title,
                    url,
                    highlight,
                    searchQuery);

            evidenceList.add(evidence);
        }

        return evidenceList;
    }

    public List<Evidence> removeDuplicates(
            List<Evidence> evidenceList) {

        List<Evidence> uniqueEvidence = new ArrayList<>();

        java.util.Set<String> seenUrls = new java.util.HashSet<>(); // HashSet stores URLs we've already encountered.

        for (Evidence evidence : evidenceList) {

            String url = evidence.url().trim();

            if (!url.isEmpty() && seenUrls.add(url)) { // add(url) returns true if the URL is new and false if already
                                                       // exists.
                uniqueEvidence.add(evidence);
            }
        }

        return uniqueEvidence;
    }

    public List<Evidence> filterRelevantEvidence(
            List<Evidence> evidenceList,
            String companyName) {

        List<Evidence> relevantEvidence = new ArrayList<>();

        String company = companyName.toLowerCase().trim();

        for (Evidence evidence : evidenceList) {

            String title = evidence.title().toLowerCase();
            String highlight = evidence.highlight().toLowerCase();

            if (title.contains(company) || highlight.contains(company)) {
                relevantEvidence.add(evidence);
            }
        }

        return relevantEvidence;
    }

}
