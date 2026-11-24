package com.internshield.research;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternshipQueryGeneratorTest {

    private final InternshipQueryGenerator generator = new InternshipQueryGenerator();

    @Test
    void testGenerateQueries_companyAndUrl() {
        List<String> queries = generator.generateQueries("Acme Corp", "https://jobboard.example.com/careers/123");
        assertTrue(queries.size() >= 3);
        assertTrue(queries.get(0).contains("Acme Corp"));
        assertTrue(queries.get(0).contains("offer letter"));
        assertTrue(queries.stream().anyMatch(q -> q.contains("jobboard.example.com")));
    }

    @Test
    void testGenerateQueries_companyOnly() {
        List<String> queries = generator.generateQueries("Acme Corp", null);
        assertEquals(2, queries.size());
        assertTrue(queries.get(0).contains("Acme Corp"));
    }

    @Test
    void testGenerateQueries_urlOnly() {
        List<String> queries = generator.generateQueries(null, "https://example.com/job");
        assertEquals(1, queries.size());
        assertTrue(queries.get(0).contains("https://example.com/job"));
    }

    @Test
    void testGenerateQueries_bothNull() {
        List<String> queries = generator.generateQueries(null, null);
        assertTrue(queries.isEmpty());
    }
}
