package com.internshield.search.dto;

public record SearchResultItem(
        String title,
        String url,
        String snippet,
        String source,
        String query
) {
    public SearchResultItem(String title, String url, String snippet, String source) {
        this(title, url, snippet, source, "");
    }
}
