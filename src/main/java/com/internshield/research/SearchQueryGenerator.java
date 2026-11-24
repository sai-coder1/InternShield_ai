
package com.internshield.research;

import java.util.List;

public class SearchQueryGenerator {

    public List<String> generateQueries(
            String companyName,
            String internshipRole) {

        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException(
                    "Company name cannot be empty.");
        }

        if (internshipRole == null || internshipRole.isBlank()) {
            throw new IllegalArgumentException(
                    "Internship role cannot be empty.");
        }

        return List.of(

                // 1. Check for payment demands
                "\"" + companyName + "\" internship fees OR registration fees OR training fees",

                // 2. Find complaints and negative reviews
                "\"" + companyName + "\" internship complaints OR scam OR fraud",

                // 3. Find previous intern experiences
                "\"" + companyName + "\" " + internshipRole
                        + " internship experience OR reviews",

                // 4. Verify official company information
                "\"" + companyName + "\" official website internship careers");
    }
}
