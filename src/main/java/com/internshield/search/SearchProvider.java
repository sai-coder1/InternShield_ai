package com.internshield.search;

import com.internshield.search.dto.SearchResultItem;
import java.util.List;

public interface SearchProvider {
    List<SearchResultItem> searchAndExtract(String query) throws Exception;
    List<SearchResultItem> searchAndExtract(String query, int count) throws Exception;
}
