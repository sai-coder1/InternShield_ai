package com.internshield.internship.dto;

import java.util.List;

/**
 * Structured details extracted from an internship URL listing or pasted description text.
 */
public record ExtractedInternshipDetails(
        String title,
        String companyName,
        String rawContent,
        String role,
        String stipend,
        List<String> responsibilities,
        String recruiterEmail,
        String recruiterPhone,
        String communicationChannel,
        List<String> paymentKeywordsFound,
        boolean feeDemanded,
        boolean unreachable,
        String extractionError
) {
    public static ExtractedInternshipDetails empty() {
        return new ExtractedInternshipDetails(
                "", "", "", "", "", List.of(), "", "", "UNKNOWN", List.of(), false, false, null
        );
    }

    public static ExtractedInternshipDetails unreachable(String error) {
        return new ExtractedInternshipDetails(
                "", "", "", "", "", List.of(), "", "", "UNKNOWN", List.of(), false, true, error
        );
    }
}
