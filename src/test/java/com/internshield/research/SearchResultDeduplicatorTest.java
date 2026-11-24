package com.internshield.research;

import com.internshield.search.dto.SearchResultItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultDeduplicatorTest {

    private final SearchResultDeduplicator deduplicator = new SearchResultDeduplicator();

    @Test
    void testDeduplicate_removesDuplicateUrlsWithTrailingSlashesAndProtocols() {
        List<SearchResultItem> items = List.of(
                new SearchResultItem("Title 1", "https://example.com/path/", "Snippet 1", "example.com"),
                new SearchResultItem("Title 1 duplicate", "https://www.example.com/path", "Snippet 2", "example.com"),
                new SearchResultItem("Title 2", "https://example.com/other", "Snippet 3", "example.com"),
                new SearchResultItem("Title 2 duplicate", "http://example.com/other/", "Snippet 4", "example.com")
        );

        List<SearchResultItem> result = deduplicator.deduplicate(items);
        assertEquals(2, result.size());
        assertEquals("Title 1", result.get(0).title());
        assertEquals("Title 2", result.get(1).title());
    }

    @Test
    void testDeduplicate_emptyAndNullHandling() {
        assertTrue(deduplicator.deduplicate(null).isEmpty());
        assertTrue(deduplicator.deduplicate(List.of()).isEmpty());
    }

    @Test
    void testNormalizeUrl() {
        assertEquals("https://example.com/path", deduplicator.normalizeUrl("https://www.example.com/path/"));
        assertEquals("https://example.com", deduplicator.normalizeUrl("https://example.com/"));
    }
}
