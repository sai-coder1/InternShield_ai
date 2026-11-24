package com.internshield.research;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class InternshipQueryGenerator {

    public List<String> generateQueries(String companyName, String internshipUrl) {
        List<String> queries = new ArrayList<>();
        String company = (companyName != null) ? companyName.trim() : "";

        if (!company.isBlank()) {
            queries.add(company + " internship offer letter stipend real or fake");
            queries.add(company + " internship student experiences glassdoor reddit");
        }

        if (internshipUrl != null && !internshipUrl.isBlank()) {
            String trimmedUrl = internshipUrl.trim();
            queries.add(trimmedUrl + " scam check review");

            try {
                URI uri = URI.create(trimmedUrl);
                String host = uri.getHost();
                if (host != null && !host.isBlank()) {
                    String domain = host.startsWith("www.") ? host.substring(4) : host;
                    if (!company.isBlank() && !domain.toLowerCase().contains(company.toLowerCase())) {
                        queries.add(domain + " " + company + " legitimacy domain check");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return queries;
    }
}
