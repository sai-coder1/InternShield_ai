package com.internshield.research;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchQueryGeneratorTest {

    private SearchQueryGenerator queryGenerator;

    @BeforeEach
    void setUp() {
        queryGenerator = new SearchQueryGenerator();
    }

    @Test
    @DisplayName("Should generate 4 targeted queries covering fees, scam allegations, official site, and student reviews")
    void testGenerateTargetedQueries_normalCase() {
        String company = "Acme Technologies";
        String url = "https://www.acme-tech.com/careers/internships";

        List<String> queries = queryGenerator.generateTargetedQueries(company, url);

        assertNotNull(queries);
        assertEquals(4, queries.size());

        // Query 1: Registration / training fees
        assertTrue(queries.get(0).contains("Acme Technologies"));
        assertTrue(queries.get(0).toLowerCase().contains("fee"));

        // Query 2: Scam allegations / reviews
        assertTrue(queries.get(1).contains("Acme Technologies"));
        assertTrue(queries.get(1).toLowerCase().contains("scam"));

        // Query 3: Official careers and domain
        assertTrue(queries.get(2).contains("acme-tech.com"));

        // Query 4: Student experiences / stipend / offer letter
        assertTrue(queries.get(3).contains("Acme Technologies"));
        assertTrue(queries.get(3).toLowerCase().contains("stipend") || queries.get(3).toLowerCase().contains("offer letter"));
    }

    @Test
    @DisplayName("Should generate fallback official query when URL is null or empty")
    void testGenerateTargetedQueries_withoutUrl() {
        String company = "Globex Corp";

        List<String> queriesNullUrl = queryGenerator.generateTargetedQueries(company, null);
        assertNotNull(queriesNullUrl);
        assertEquals(4, queriesNullUrl.size());
        assertTrue(queriesNullUrl.get(2).contains("Globex Corp official website careers"));

        List<String> queriesBlankUrl = queryGenerator.generateTargetedQueries(company, "   ");
        assertEquals(4, queriesBlankUrl.size());
        assertTrue(queriesBlankUrl.get(2).contains("Globex Corp official website careers"));
    }

    @Test
    @DisplayName("Should return empty list for null, empty, or blank company name")
    void testGenerateTargetedQueries_invalidCompany() {
        assertTrue(queryGenerator.generateTargetedQueries(null, "https://example.com").isEmpty());
        assertTrue(queryGenerator.generateTargetedQueries("", "https://example.com").isEmpty());
        assertTrue(queryGenerator.generateTargetedQueries("   ", "https://example.com").isEmpty());
    }

    @Test
    @DisplayName("Should correctly strip www. prefix from domain in search query")
    void testGenerateTargetedQueries_domainExtraction() {
        List<String> queries = queryGenerator.generateTargetedQueries("Initech", "https://www.initech.io/jobs/123");
        assertEquals(4, queries.size());
        assertTrue(queries.get(2).contains("domain initech.io"));
        assertFalse(queries.get(2).contains("www.initech.io"));
    }
}
