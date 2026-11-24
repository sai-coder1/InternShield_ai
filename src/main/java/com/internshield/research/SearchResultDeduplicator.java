package com.internshield.research;

import com.internshield.search.dto.SearchResultItem;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SearchResultDeduplicator {

    public List<SearchResultItem> deduplicate(List<SearchResultItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<SearchResultItem> deduplicated = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (SearchResultItem item : items) {
            if (item == null || item.url() == null || item.url().isBlank()) {
                continue;
            }

            String normalized = normalizeUrl(item.url());
            if (seenUrls.add(normalized)) {
                deduplicated.add(item);
            }
        }

        return deduplicated;
    }

    public String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String cleaned = url.trim();
        try {
            URI uri = URI.create(cleaned);
            // Normalize HTTP/HTTPS schemes uniformly
            String scheme = "https";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            String path = uri.getPath() != null ? uri.getPath() : "";
            if (path.equals("/")) {
                path = "";
            } else if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
            return scheme + "://" + host + path + query;
        } catch (Exception e) {
            if (cleaned.endsWith("/")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            return cleaned.toLowerCase();
        }
    }
}
