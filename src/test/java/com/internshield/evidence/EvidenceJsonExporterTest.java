package com.internshield.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceJsonExporterTest {

    private EvidenceJsonExporter exporter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        exporter = new EvidenceJsonExporter();
    }

    @Test
    @DisplayName("Should export list of Evidence items into structured JSON string")
    void testExportToJson_validEvidenceList() throws Exception {
        Evidence ev1 = new Evidence();
        ev1.setId(1L);
        ev1.setTitle("MCA Corporate Records");
        ev1.setSourceUrl("https://mca.gov.in/company/acme");
        ev1.setSourceDomain("mca.gov.in");
        ev1.setCategory(EvidenceCategory.OFFICIAL_REGISTRY);
        ev1.setSourceCredibility("HIGH");
        ev1.setDescription("Active company registered in 2018");

        Evidence ev2 = new Evidence();
        ev2.setId(2L);
        ev2.setTitle("Student Reddit Warning");
        ev2.setSourceUrl("https://reddit.com/r/scams/acme");
        ev2.setSourceDomain("reddit.com");
        ev2.setCategory(EvidenceCategory.COMMUNITY_REPORT);
        ev2.setSourceCredibility("MEDIUM");
        ev2.setDescription("Registration fee requested on Telegram");

        String json = exporter.exportToJson("Acme Corp", 100L, List.of(ev1, ev2));

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);

        assertEquals("Acme Corp", root.path("company_name").asText());
        assertEquals(100L, root.path("report_id").asLong());
        assertEquals(2, root.path("evidence_count").asInt());

        JsonNode items = root.path("evidence_items");
        assertTrue(items.isArray());
        assertEquals(2, items.size());

        assertEquals("MCA Corporate Records", items.get(0).path("title").asText());
        assertEquals("https://mca.gov.in/company/acme", items.get(0).path("url").asText());
        assertEquals("OFFICIAL_REGISTRY", items.get(0).path("category").asText());
        assertEquals("HIGH", items.get(0).path("credibility").asText());

        assertEquals("Student Reddit Warning", items.get(1).path("title").asText());
        assertEquals("COMMUNITY_REPORT", items.get(1).path("category").asText());
    }

    @Test
    @DisplayName("Should handle empty or null evidence list gracefully")
    void testExportToJson_emptyEvidence() throws Exception {
        String json = exporter.exportToJson("Ghost Corp", null, List.of());
        JsonNode root = objectMapper.readTree(json);

        assertEquals("Ghost Corp", root.path("company_name").asText());
        assertEquals(0, root.path("evidence_count").asInt());
        assertEquals(0, root.path("evidence_items").size());
    }
}
