package com.internshield.research;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates targeted, high-precision search queries for internship verification.
 * Target areas:
 * 1. Internship registration & training fees
 * 2. Internship scam complaints & fraud allegations
 * 3. Role-specific internship experiences & employee reviews
 * 4. Official company website & legitimate contact information
 */
@Component
public class SearchQueryGenerator {

    /**
     * Generates 3-4 targeted research queries for a company and an optional internship URL or role.
     *
     * @param companyName the name of the company under investigation
     * @param internshipUrl optional URL of the internship listing
     * @return an unmodifiable list of 3-4 focused search queries
     */
    public List<String> generateTargetedQueries(String companyName, String internshipUrl) {
        if (companyName == null || companyName.isBlank()) {
            return Collections.emptyList();
        }

        String cleanedCompany = companyName.trim();
        List<String> queries = new ArrayList<>(4);

        // 1. Registration & training fee demands (high risk indicator)
        queries.add(cleanedCompany + " internship registration fee training fee payment demanded");

        // 2. Scam complaints, fake offers, and fraud allegations
        queries.add(cleanedCompany + " internship scam fake offer complaints review glassdoor reddit");

        // 3. Official company identity & legitimate careers page
        String officialDomain = extractDomainFromUrl(internshipUrl);
        if (officialDomain != null && !officialDomain.isBlank()) {
            queries.add(cleanedCompany + " official careers contact domain " + officialDomain);
        } else {
            queries.add(cleanedCompany + " official website careers contact email hr");
        }

        // 4. Role-specific experiences or offer letter verification
        queries.add(cleanedCompany + " internship student experiences stipend offer letter real or fake");

        return Collections.unmodifiableList(queries);
    }

    private String extractDomainFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host != null && !host.isBlank()) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
