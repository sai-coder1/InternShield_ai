package com.internshield.research;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyQueryGeneratorTest {

    private final CompanyQueryGenerator generator = new CompanyQueryGenerator();

    @Test
    void testGenerateQueries_validCompany() {
        List<String> queries = generator.generateQueries("Acme Corp");
        assertEquals(3, queries.size());
        assertTrue(queries.get(0).contains("Acme Corp"));
        assertTrue(queries.get(0).contains("scam"));
        assertTrue(queries.get(1).contains("registration fee"));
        assertTrue(queries.get(2).contains("official website"));
    }

    @Test
    void testGenerateQueries_emptyOrNull() {
        assertTrue(generator.generateQueries("").isEmpty());
        assertTrue(generator.generateQueries("   ").isEmpty());
        assertTrue(generator.generateQueries(null).isEmpty());
    }
}
