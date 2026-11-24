package com.internshield.research;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompanyQueryGenerator {

    public List<String> generateQueries(String companyName) {
        List<String> queries = new ArrayList<>();
        if (companyName == null || companyName.isBlank()) {
            return queries;
        }

        String cleaned = companyName.trim();
        queries.add(cleaned + " reviews scam legit complaints");
        queries.add(cleaned + " internship registration fee training fee");
        queries.add(cleaned + " official website careers contact");
        return queries;
    }
}
