package com.internshield.search.dto;

public record SearchResultItem(
        String title,
        String url,
        String snippet,
        String source
) {
}
